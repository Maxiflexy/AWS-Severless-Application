package playkosmos.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Review {
    private Long id;
    private Long postId;
    private Long userId;
    private String reviewText;
    private Integer rating; // Rating between 1 and 5
    private LocalDateTime createdAt;
}
