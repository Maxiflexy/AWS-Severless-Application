package playkosmos.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Post {

    private Long postId;
    private Long userId;
    private String caption;
    private List<String> mediaUrls;
    private List<String> mediaTypes;
    private List<Long> taggedUserIds;
    private List<Long> attendingUserIds;
    private List<Comment> comments;
    private List<Question> questions;
    private List<Answer> answers;
    private List<Like> likes;
    private List<Review> reviews;
    private List<Long> participantIds;
    private int shares;
}
