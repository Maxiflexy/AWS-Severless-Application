package playkosmos.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Comment {

    private Long commentId;
    private Long postId;
    private Long userId;
    private String CommentText;
    private LocalDateTime createdAt;

    public Comment(Long postId, Long userId, String CommentText) {
        this.postId = postId;
        this.userId = userId;
        this.CommentText = CommentText;
        this.createdAt = LocalDateTime.now();
    }
}

