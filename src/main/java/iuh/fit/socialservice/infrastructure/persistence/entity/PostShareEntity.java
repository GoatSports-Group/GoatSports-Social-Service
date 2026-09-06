package iuh.fit.socialservice.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "post_shares")
@CompoundIndexes({
        @CompoundIndex(name = "share_post_time_idx", def = "{'postId': 1, 'sharedAt': -1}"),
        @CompoundIndex(name = "share_user_time_idx", def = "{'userId': 1, 'sharedAt': -1}")
})
public class PostShareEntity {
    @Id
    private String shareId;
    private String postId;
    private String userId;
    private String caption;
    private LocalDateTime sharedAt;
}
