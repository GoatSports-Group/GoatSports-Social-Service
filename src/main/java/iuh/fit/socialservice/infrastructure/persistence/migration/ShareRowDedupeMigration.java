package iuh.fit.socialservice.infrastructure.persistence.migration;

import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostShareEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

/**
 * Mot ban trung gian tung ghi CA bai chia se (posts.sharedPostId) LAN dong post_shares cho cung mot lan
 * chia se, nen luot chia se bi dem hai lan. Xoa dong post_shares nao da co bai chia se tuong ung
 * (cung nguoi, cung bai goc). post_shares khong con duoc ghi them, nen tap duyet moi lan khoi dong
 * chi la cac dong kieu cu va chay lai bao nhieu lan cung khong doi ket qua.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class ShareRowDedupeMigration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int removed = removeRowsCoveredBySharePosts();
        if (removed > 0) log.info("Removed {} post_shares rows already represented by share posts", removed);
    }

    public int removeRowsCoveredBySharePosts() {
        int removed = 0;
        for (PostShareEntity row : mongoTemplate.findAll(PostShareEntity.class)) {
            if (row.getPostId() == null || row.getUserId() == null) continue;
            boolean covered = mongoTemplate.exists(new Query(Criteria.where("sharedPostId").is(row.getPostId())
                    .and("authorId").is(row.getUserId())), PostEntity.class);
            if (covered) {
                mongoTemplate.remove(row);
                removed++;
            }
        }
        return removed;
    }
}
