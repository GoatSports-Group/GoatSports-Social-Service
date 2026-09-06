package iuh.fit.socialservice.domain.entity;

import iuh.fit.socialservice.domain.enums.FollowTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Follow extends BaseEntity {
    private UUID followId;
    private UUID followerId;
    private FollowTargetType targetType;
    private UUID targetId;
}
