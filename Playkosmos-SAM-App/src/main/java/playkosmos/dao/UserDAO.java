package playkosmos.dao;

import java.sql.*;
import java.time.LocalDateTime;

import playkosmos.dbutil.DatabaseConnectionManager;
import lombok.RequiredArgsConstructor;
import playkosmos.entity.User;

@RequiredArgsConstructor
public class UserDAO {

    private final DatabaseConnectionManager dbConnectionManager;

    public void saveUserToDatabase(User user) throws SQLException {

        ensureUserTableExists();

        String query =
                "INSERT INTO user_table (username, email, phoneNumber, countryCode, dateOfBirth, password) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getEmail());
            preparedStatement.setString(3, user.getPhoneNumber());
            preparedStatement.setString(4, user.getCountryCode());
            preparedStatement.setDate(5, Date.valueOf(user.getDateOfBirth()));
            preparedStatement.setString(6, user.getPassword());
            preparedStatement.executeUpdate();
        }

    }

    private void ensureUserTableExists() throws SQLException{

        String createTableQuery = "CREATE TABLE IF NOT EXISTS user_table (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(255) NOT NULL, " +
                "email VARCHAR(255), " +
                "phoneNumber VARCHAR(255), " +
                "countryCode VARCHAR(10), " +
                "dateOfBirth DATE NOT NULL, " +
                "password VARCHAR(255) NOT NULL" +
                ")";

        try(Connection connection = dbConnectionManager.getConnection();
            Statement statement = connection.createStatement()){
            statement.execute(createTableQuery);
        }
    }

    public User findUserByEmail(String email) throws SQLException {

        String query = "SELECT id, username, email, phoneNumber, countryCode, dateOfBirth, password FROM user_table WHERE email = ?";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, email);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User(
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            resultSet.getString("phoneNumber"),
                            resultSet.getString("countryCode"),
                            resultSet.getDate("dateOfBirth").toLocalDate(),
                            resultSet.getString("password")
                    );
                    user.setId(resultSet.getLong("id"));
                    return user;
                } else {
                    return null;
                }
            }
        }
    }


    public User findUserByPhoneNumber(String phoneNumber) throws SQLException {

//        if (phoneNumber.startsWith("0")) {
//            phoneNumber = "+234" + phoneNumber.substring(1);
//        }

        String query = "SELECT id, username, email, phoneNumber, countryCode, dateOfBirth, password FROM user_table WHERE phoneNumber = ?";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, phoneNumber);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    User user = new User(
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            resultSet.getString("phoneNumber"),
                            resultSet.getString("countryCode"),
                            resultSet.getDate("dateOfBirth").toLocalDate(),
                            resultSet.getString("password")
                    );
                    user.setId(resultSet.getLong("id"));
                    return user;
                } else {
                    return null;
                }
            }
        }
    }

    public void saveOtpToDatabase(User user, String otp) throws SQLException {
        ensureOtpTableExists();

        String query = "INSERT INTO otp_table (user_id, otp, expiry_time) VALUES (?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, user.getId());
            preparedStatement.setString(2, otp);
            preparedStatement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusMinutes(10))); // OTP expires in 10 minutes

            preparedStatement.executeUpdate();
        }
    }

    private void ensureOtpTableExists() throws SQLException {
        String createOtpTableQuery = "CREATE TABLE IF NOT EXISTS otp_table (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "otp VARCHAR(6) NOT NULL, " +
                "expiry_time TIMESTAMP NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES user_table(id)" +
                ")";

        try (Connection connection = dbConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createOtpTableQuery);
        }
    }

    public boolean isOtpValid(User user, String otp) throws SQLException {

        String query = "SELECT otp, expiry_time FROM otp_table WHERE user_id = ? ORDER BY expiry_time DESC LIMIT 1";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setLong(1, user.getId());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String storedOtp = resultSet.getString("otp");
                    LocalDateTime expiryTime = resultSet.getTimestamp("expiry_time").toLocalDateTime();

                    if (storedOtp.equals(otp) && expiryTime.isAfter(LocalDateTime.now())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public User findUserByUsername(String username) throws SQLException {
        String query = "SELECT * FROM user_table WHERE username = ?";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getString("username"),
                            resultSet.getString("email"),
                            resultSet.getString("phoneNumber"),
                            resultSet.getString("countryCode"),
                            resultSet.getDate("dateOfBirth").toLocalDate(),
                            resultSet.getString("password")
                    );
                } else {
                    return null;
                }
            }
        }
    }

    public void updateUserPassword(String username, String newPassword) throws SQLException {
        String query = "UPDATE user_table SET password = ? WHERE username = ?";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, newPassword);
            preparedStatement.setString(2, username);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Updating password failed, no rows affected.");
            }
        }
    }

}
