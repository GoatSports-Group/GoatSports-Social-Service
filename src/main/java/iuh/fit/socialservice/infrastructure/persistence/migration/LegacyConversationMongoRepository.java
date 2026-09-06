package iuh.fit.socialservice.infrastructure.persistence.migration;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LegacyConversationMongoRepository extends MongoRepository<LegacyConversationDocument, String> {
}
