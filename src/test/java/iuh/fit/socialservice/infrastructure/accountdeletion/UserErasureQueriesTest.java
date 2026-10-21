package iuh.fit.socialservice.infrastructure.accountdeletion;

import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationMemberEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.FollowEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.FriendshipEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.UserBlockEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaConversationRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFriendshipRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaUserBlockRepository;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPQL chỉ được Hibernate kiểm lúc khởi động, mock không bắt được sai tên trường. Test này cho Hibernate phân tích
 * đúng chuỗi @Query của các truy vấn xóa dữ liệu tài khoản mà không cần database (không đọc metadata JDBC).
 */
class UserErasureQueriesTest {

    @Test
    void erasureQueriesParseAgainstTheRealEntityModel() throws Exception {
        List<Method> methods = List.of(
                JpaFriendshipRepository.class.getMethod("deleteAllOf", UUID.class),
                JpaFollowRepository.class.getMethod("deleteAllOf", UUID.class),
                JpaUserBlockRepository.class.getMethod("deleteAllOf", UUID.class),
                JpaConversationRepository.class.getMethod("leaveGroupConversations", UUID.class, LocalDateTime.class));
        try (SessionFactory factory = new Configuration()
                .addAnnotatedClass(FriendshipEntity.class)
                .addAnnotatedClass(FollowEntity.class)
                .addAnnotatedClass(UserBlockEntity.class)
                .addAnnotatedClass(ConversationEntity.class)
                .addAnnotatedClass(ConversationMemberEntity.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .buildSessionFactory();
             var entityManager = factory.createEntityManager()) {
            for (Method method : methods) {
                entityManager.createQuery(method.getAnnotation(Query.class).value());
            }
        }
    }
}
