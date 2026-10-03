package iuh.fit.socialservice.infrastructure.persistence.migration;

import iuh.fit.socialservice.domain.service.PostTags;
import iuh.fit.socialservice.infrastructure.persistence.entity.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bai cu chi co `tags` theo cach cu (co the dang dau to hop NFD, chua gop "#bóngđá" voi "#bongda") va chua co
 * `tagKeys`. Rut lai tag tu noi dung theo quy tac moi va ghi `tagKeys`. Chi duyet bai chua co `tagKeys`, nen chay
 * lai bao nhieu lan cung khong doi ket qua.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class PostTagKeyMigration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int updated = backfill();
        if (updated > 0) log.info("Re-extracted hashtags and tag keys for {} posts", updated);
    }

    public int backfill() {
        int updated = 0;
        for (PostEntity post : mongoTemplate.find(new Query(Criteria.where("tagKeys").exists(false)), PostEntity.class)) {
            List<String> tags = PostTags.extract(post.getContent());
            mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(post.getPostId())),
                    new Update().set("tags", tags).set("tagKeys", tags.stream().map(PostTags::key).toList()),
                    PostEntity.class);
            updated++;
        }
        return updated;
    }
}
