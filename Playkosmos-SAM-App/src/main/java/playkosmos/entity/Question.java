package playkosmos.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Question {
    private Long questionId;
    private Long postId;
    private Long userId;
    private String questionText;
    private LocalDateTime createdAt;
    private List<Answer> answers;

    public Question(Long postId, Long userId, String questionText) {
        this.postId = postId;
        this.userId = userId;
        this.questionText = questionText;
        this.createdAt = LocalDateTime.now();
    }
}

/*
Given below is the Question and Answer entity also is the PostDAO
 */
