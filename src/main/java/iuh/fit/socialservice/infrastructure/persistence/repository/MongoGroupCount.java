package iuh.fit.socialservice.infrastructure.persistence.repository;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dem theo lo: mot aggregation {@code $match → $group} thay cho mot truy van dem moi bai. */
final class MongoGroupCount {
    private MongoGroupCount() {
    }

    static Map<UUID, Long> count(MongoTemplate mongoTemplate, Class<?> entity, String field,
                                 Collection<UUID> ids, Criteria extra) {
        if (ids.isEmpty()) return Map.of();
        List<String> keys = ids.stream().map(UUID::toString).distinct().toList();
        Criteria match = extra == null
                ? Criteria.where(field).in(keys)
                : new Criteria().andOperator(Criteria.where(field).in(keys), extra);
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(match),
                Aggregation.group(field).count().as("n"));
        Map<UUID, Long> result = new HashMap<>();
        for (Document row : mongoTemplate.aggregate(aggregation, entity, Document.class).getMappedResults()) {
            result.put(UUID.fromString(row.getString("_id")), ((Number) row.get("n")).longValue());
        }
        return result;
    }
}
