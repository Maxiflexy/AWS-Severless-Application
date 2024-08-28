package playkosmos.entity;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Answer {
    private Long answerId;
    private Long questionId;
    private Long userId;
    private String answerText;
    private LocalDateTime createdAt;

    public Answer(Long questionId, Long userId, String answerText) {
        this.questionId = questionId;
        this.userId = userId;
        this.answerText = answerText;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters omitted for brevity
}
