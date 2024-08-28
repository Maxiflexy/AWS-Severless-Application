package playkosmos.dao;

import lombok.RequiredArgsConstructor;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class PostDAO {

    private final DatabaseConnectionManager dbConnectionManager;

    public void createPostTables() throws SQLException {
        ensurePostTableExists();
        ensurePostMediaTableExists();
        ensurePostTagsTableExists();
        ensurePostLikesTableExists();
        ensureCommentsTableExists();
        ensureQuestionsTableExists();
        ensureAnswersTableExists();
        ensureReviewsTableExists();
        ensureParticipantsTableExists();
    }

    private void executeTableCreation(String createTableQuery) throws SQLException {
        try (Connection connection = dbConnectionManager.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(createTableQuery);
        }
    }

    public void savePost(Post post) throws SQLException {
        createPostTables();

        // Insert into posts table without the shares column
        String query = "INSERT INTO posts (user_id, caption) VALUES (?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setLong(1, post.getUserId());
            preparedStatement.setString(2, post.getCaption());

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long postId = generatedKeys.getLong(1);
                    saveAssociatedData(postId, post);
                }
            }
        }
    }

    private void saveAssociatedData(long postId, Post post) throws SQLException {
        CompletableFuture<Void> mediaFuture = CompletableFuture.runAsync(() -> {
            try {
                saveMedia(postId, post.getMediaUrls(), post.getMediaTypes());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> tagsFuture = CompletableFuture.runAsync(() -> {
            try {
                saveTags(postId, post.getTaggedUserIds());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> attendanceFuture = CompletableFuture.runAsync(() -> {
            try {
                saveAttendance(postId, post.getAttendingUserIds());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> commentsFuture = CompletableFuture.runAsync(() -> {
            try {
                saveComments(postId, post.getComments());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> questionsFuture = CompletableFuture.runAsync(() -> {
            try {
                saveQuestions(postId, post.getQuestions());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> answersFuture = CompletableFuture.runAsync(() -> {
            try {
                saveAnswers(postId, post.getAnswers());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> likesFuture = CompletableFuture.runAsync(() -> {
            try {
                saveLikes(postId, post.getLikes());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> reviewsFuture = CompletableFuture.runAsync(() -> {
            try {
                saveReviews(postId, post.getReviews());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> participantsFuture = CompletableFuture.runAsync(() -> {
            try {
                saveParticipants(postId, post.getParticipantIds());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        // Wait for all tasks to complete
        CompletableFuture.allOf(
                mediaFuture,
                tagsFuture,
                attendanceFuture,
                commentsFuture,
                questionsFuture,
                answersFuture,
                likesFuture,
                reviewsFuture,
                participantsFuture
        ).join();
    }

    private void saveMedia(long postId, List<String> mediaUrls, List<String> mediaTypes) throws SQLException {
        String query = "INSERT INTO post_media (post_id, media_url, media_type) VALUES (?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (int i = 0; i < mediaUrls.size(); i++) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setString(2, mediaUrls.get(i));
                preparedStatement.setString(3, mediaTypes.get(i));
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveTags(long postId, List<Long> taggedUserIds) throws SQLException {
        String query = "INSERT INTO post_tags (post_id, tagged_user_id) VALUES (?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Long userId : taggedUserIds) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, userId);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveAttendance(long postId, List<Long> attendingUserIds) throws SQLException {
        String query = "INSERT INTO post_participants (post_id, user_id) VALUES (?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Long userId : attendingUserIds) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, userId);
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveComments(long postId, List<Comment> comments) throws SQLException {
        String query = "INSERT INTO comments (post_id, user_id, comment_text) VALUES (?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Comment comment : comments) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, comment.getUserId());
                preparedStatement.setString(3, comment.getCommentText());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveQuestions(long postId, List<Question> questions) throws SQLException {
        String query = "INSERT INTO post_questions (post_id, user_id, question) VALUES (?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Question question : questions) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, question.getUserId()); // Track the user who asked the question
                preparedStatement.setString(3, question.getQuestionText());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveAnswers(long postId, List<Answer> answers) throws SQLException {
        String query = "INSERT INTO post_answers (post_id, question_id, user_id, answer) VALUES (?, ?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Answer answer : answers) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, answer.getQuestionId());
                preparedStatement.setLong(3, answer.getUserId()); // Track the user who answered the question
                preparedStatement.setString(4, answer.getAnswerText());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }


    private void saveLikes(long postId, List<Like> likes) throws SQLException {
        String query = "INSERT INTO post_likes (post_id, user_id, like_type) VALUES (?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Like like : likes) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, like.getUserId());
                preparedStatement.setString(3, like.getLikeType());  // 'A', 'B', 'C' for thumbs up, clap, love
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveReviews(long postId, List<Review> reviews) throws SQLException {
        String query = "INSERT INTO post_reviews (post_id, user_id, review_text, rating) VALUES (?, ?, ?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Review review : reviews) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, review.getUserId());
                preparedStatement.setString(3, review.getReviewText());
                preparedStatement.setInt(4, review.getRating());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void saveParticipants(long postId, List<Long> participantIds) throws SQLException {
        String query = "INSERT INTO post_participants (post_id, user_id) VALUES (?, ?)";

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (Long userId : participantIds) {
                preparedStatement.setLong(1, postId);
                preparedStatement.setLong(2, userId); // Using the user ID directly
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        }
    }

    private void ensurePostTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS posts (" +
                "post_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT REFERENCES user_table(id), " +
                "caption TEXT, " +
                "shares INT DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensurePostMediaTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_media (" +
                "media_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "media_url TEXT, " +
                "media_type VARCHAR(10)" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensurePostTagsTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_tags (" +
                "post_tag_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "tagged_user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensurePostLikesTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_likes (" +
                "like_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE, " +
                "like_type CHAR(1), " +  // 'A' for thumbs up, 'B' for clap, 'C' for love
                "liked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensureCommentsTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS comments (" +
                "comment_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE, " +
                "comment_text TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensureQuestionsTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_questions (" +
                "question_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE, " +  // Track the user who asked the question
                "question TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensureAnswersTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_answers (" +
                "answer_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "question_id BIGINT REFERENCES post_questions(question_id) ON DELETE CASCADE, " +
                "user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE, " +  // Track the user who answered the question
                "answer TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensureReviewsTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_reviews (" +
                "review_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE, " +
                "review_text TEXT, " +
                "rating INT CHECK (rating BETWEEN 1 AND 5), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        executeTableCreation(createTableQuery);
    }

    private void ensureParticipantsTableExists() throws SQLException {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS post_participants (" +
                "participant_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "post_id BIGINT REFERENCES posts(post_id) ON DELETE CASCADE, " +
                "user_id BIGINT REFERENCES user_table(id) ON DELETE CASCADE" +
                ")";

        executeTableCreation(createTableQuery);
    }

    public Post getPostById(long postId) throws SQLException {
        String query = "SELECT * FROM posts WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Post post = new Post();
                    post.setPostId(resultSet.getLong("post_id"));
                    post.setUserId(resultSet.getLong("user_id"));
                    post.setCaption(resultSet.getString("caption"));
                    post.setShares(Integer.parseInt(resultSet.getString("shares")));
                    return post;
                } else {
                    throw new SQLException("Post not found");
                }
            }
        }
    }

    public List<String> getMediaUrlsByPostId(long postId) throws SQLException {
        String query = "SELECT media_url FROM post_media WHERE post_id = ?";
        List<String> mediaUrls = new ArrayList<>();

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    mediaUrls.add(resultSet.getString("media_url"));
                }
            }
        }
        return mediaUrls;
    }

    public List<Long> getTaggedUserIdsByPostId(long postId) throws SQLException {
        String query = "SELECT tagged_user_id FROM post_tags WHERE post_id = ?";
        List<Long> taggedUserIds = new ArrayList<>();

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    taggedUserIds.add(resultSet.getLong("tagged_user_id"));
                }
            }
        }
        return taggedUserIds;
    }

    public List<Comment> getCommentsByPostId(long postId) throws SQLException {
        String query = "SELECT * FROM comments WHERE post_id = ?";
        List<Comment> comments = new ArrayList<>();

        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Comment comment = new Comment();
                    comment.setUserId(resultSet.getLong("user_id"));
                    comment.setCommentText(resultSet.getString("comment_text"));
                    comments.add(comment);
                }
            }
        }
        return comments;
    }

    public List<Long> getAttendingUserIdsByPostId(long postId) throws SQLException {
        String query = "SELECT user_id FROM post_participants WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Long> userIds = new ArrayList<>();
                while (resultSet.next()) {
                    userIds.add(resultSet.getLong("user_id"));
                }
                return userIds;
            }
        }
    }

    public List<Question> getQuestionsByPostId(long postId) throws SQLException {
        String query = "SELECT * FROM post_questions WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Question> questions = new ArrayList<>();
                while (resultSet.next()) {
                    Question question = new Question();
                    question.setQuestionId(resultSet.getLong("question_id"));
                    question.setUserId(resultSet.getLong("user_id"));
                    question.setQuestionText(resultSet.getString("question"));
                    questions.add(question);
                }
                return questions;
            }
        }
    }

    public List<Answer> getAnswersByPostId(long postId) throws SQLException {
        String query = "SELECT * FROM post_answers WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Answer> answers = new ArrayList<>();
                while (resultSet.next()) {
                    Answer answer = new Answer();
                    answer.setAnswerId(resultSet.getLong("answer_id"));
                    answer.setQuestionId(resultSet.getLong("question_id"));
                    answer.setUserId(resultSet.getLong("user_id"));
                    answer.setAnswerText(resultSet.getString("answer"));
                    answers.add(answer);
                }
                return answers;
            }
        }
    }

    public List<Like> getLikesByPostId(long postId) throws SQLException {
        String query = "SELECT * FROM post_likes WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Like> likes = new ArrayList<>();
                while (resultSet.next()) {
                    Like like = new Like();
                    like.setUserId(resultSet.getLong("user_id"));
                    like.setLikeType(resultSet.getString("like_type"));
                    likes.add(like);
                }
                return likes;
            }
        }
    }

    public List<Review> getReviewsByPostId(long postId) throws SQLException {
        String query = "SELECT * FROM post_reviews WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Review> reviews = new ArrayList<>();
                while (resultSet.next()) {
                    Review review = new Review();
                    review.setUserId(resultSet.getLong("user_id"));
                    review.setReviewText(resultSet.getString("review_text"));
                    review.setRating(resultSet.getInt("rating"));
                    reviews.add(review);
                }
                return reviews;
            }
        }
    }

    public List<Long> getParticipantIdsByPostId(long postId) throws SQLException {
        String query = "SELECT user_id FROM post_participants WHERE post_id = ?";
        try (Connection connection = dbConnectionManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setLong(1, postId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                List<Long> participantIds = new ArrayList<>();
                while (resultSet.next()) {
                    participantIds.add(resultSet.getLong("user_id"));
                }
                return participantIds;
            }
        }
    }

//    public Integer getSharesByPostId(long postId) throws SQLException {
//        String query = "SELECT shares FROM posts WHERE post_id = ?";
//        try (Connection connection = dbConnectionManager.getConnection();
//             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
//            preparedStatement.setLong(1, postId);
//            try (ResultSet resultSet = preparedStatement.executeQuery()) {
//                if (resultSet.next()) {
//                    return resultSet.getInt("shares");
//                }
//                return 0;
//            }
//        }
//    }
}