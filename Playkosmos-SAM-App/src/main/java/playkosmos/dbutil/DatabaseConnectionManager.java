package playkosmos.dbutil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

public class DatabaseConnectionManager {

    private static DatabaseConnectionManager instance;
    private final String dbUrl;
    private final String username;
    private final String password;

    public DatabaseConnectionManager(Map<String, Object> secretMap) {
        this.username = (String) secretMap.get("username");
        this.password = (String) secretMap.get("password");
        String host = (String) secretMap.get("host");

        int portInteger = ((Double) secretMap.get("port")).intValue();
        String port = Integer.toString(portInteger);
        String dbname = (String) secretMap.get("dbname");

        if (username == null || password == null || host == null || port.isEmpty() || dbname == null) {
            throw new RuntimeException("Database credentials or connection details are missing in the secret");
        }

        this.dbUrl = String.format("jdbc:mysql://%s:%s/%s", host, port, dbname);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, username, password);
    }

    public static synchronized DatabaseConnectionManager getInstance(Map<String, Object> secretMap) {
        if (instance == null) {
            instance = new DatabaseConnectionManager(secretMap);
        }
        return instance;
    }
}