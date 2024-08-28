package playkosmos.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import playkosmos.dao.PostDAO;
import playkosmos.dbutil.DatabaseConnectionManager;
import playkosmos.entity.*;
import playkosmos.utils.LocalDateTimeTypeAdapter;
import playkosmos.utils.LocalDateTypeAdapter;
import playkosmos.utils.SecretsManagerHelper;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PostRetrievalHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SecretsManagerHelper secretsManagerHelper;
    private final Gson gson;

    public PostRetrievalHandler() {
        String region = System.getenv("REGION_NAME");
        String secretName = System.getenv("DB_SECRET");

        this.secretsManagerHelper = SecretsManagerHelper.getInstance(region, secretName);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent requestEvent, Context context) {
        String postIdStr = requestEvent.getPathParameters().get("postId");
        long postId;

        try {
            postId = Long.parseLong(postIdStr);
        } catch (NumberFormatException e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(gson.toJson(Map.of("status", "error", "message", "Invalid post ID")));
        }

        try {
            String secret = secretsManagerHelper.getSecret();
            Map<String, Object> secretMap = gson.fromJson(secret, Map.class);

            DatabaseConnectionManager dcm = DatabaseConnectionManager.getInstance(secretMap);
            PostDAO postDAO = new PostDAO(dcm);

            CompletableFuture<Post> postFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getPostById(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<String>> mediaFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getMediaUrlsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Long>> tagsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getTaggedUserIdsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Comment>> commentsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getCommentsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Long>> attendingUsersFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getAttendingUserIdsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Question>> questionsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getQuestionsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Answer>> answersFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getAnswersByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Like>> likesFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getLikesByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Review>> reviewsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getReviewsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            CompletableFuture<List<Long>> participantsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return postDAO.getParticipantIdsByPostId(postId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Wait for all tasks to complete
            CompletableFuture.allOf(postFuture, mediaFuture, tagsFuture, commentsFuture,
                    attendingUsersFuture, questionsFuture, answersFuture, likesFuture, reviewsFuture,
                    participantsFuture)
                    .join();


            // Collect results
            Post post = postFuture.get();
            post.setMediaUrls(mediaFuture.get());
            post.setTaggedUserIds(tagsFuture.get());
            post.setComments(commentsFuture.get());
            post.setAttendingUserIds(attendingUsersFuture.get());
            post.setQuestions(questionsFuture.get());
            post.setAnswers(answersFuture.get());
            post.setLikes(likesFuture.get());
            post.setReviews(reviewsFuture.get());
            post.setParticipantIds(participantsFuture.get());
            //post.setShares(sharesFuture.get());

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody(gson.toJson(post));

        } catch (Exception e) {
            context.getLogger().log("Error retrieving post: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(gson.toJson(Map.of("status", "error", "message", "Internal server error")));
        }
    }
}

