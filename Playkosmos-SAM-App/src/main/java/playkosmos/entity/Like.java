package playkosmos.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Like {

    private Long id;
    private Long postId;
    private Long userId;
    private String likeType; // A for thumbs up, B for clap, C for love
    private LocalDateTime likedAt;

}
