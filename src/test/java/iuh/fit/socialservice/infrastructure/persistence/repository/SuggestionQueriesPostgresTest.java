package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.domain.enums.ConversationType;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.ConversationMemberEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.FriendshipEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaConversationRepository;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFriendshipRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.io.FileInputStream;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Chay that hai truy van cua "Goi y theo doi" (nguoi chung nhom chat, ban cua ban be) tren Postgres dev, chi doc.
 * JPQL chi duoc Hibernate kiem khi khoi dong; mock khong bat duoc sai ten truong hay sai cu phap join. Tu bo qua khi
 * khong co DB (doc URL tu application-dev.properties, khong chep mat khau vao test).
 */
class SuggestionQueriesPostgresTest {
    private static EntityManagerFactory factory;
    private static EntityManager entityManager;

    @BeforeAll
    static void connect() {
        // Windows dat JVM la "Asia/Saigon", ten Postgres khong nhan.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try (FileInputStream input = new FileInputStream("src/main/resources/application-dev.properties")) {
            Properties dev = new Properties();
            dev.load(input);
            String url = dev.getProperty("spring.datasource.url");
            DriverManager.getConnection(url).close();
            factory = new Configuration()
                    .addAnnotatedClass(ConversationEntity.class)
                    .addAnnotatedClass(ConversationMemberEntity.class)
                    .addAnnotatedClass(FriendshipEntity.class)
                    .addAnnotatedClass(iuh.fit.socialservice.infrastructure.persistence.entity.FollowEntity.class)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.hbm2ddl.auto", "none")
                    .setProperty("hibernate.connection.readOnly", "true")
                    .buildSessionFactory();
            entityManager = factory.createEntityManager();
        } catch (Exception unavailable) {
            assumeTrue(false, "Postgres dev không sẵn sàng: " + unavailable.getMessage());
        }
    }

    @AfterAll
    static void close() {
        if (entityManager != null) entityManager.close();
        if (factory != null) factory.close();
    }

    @Test
    void coMembersQueryRunsAndNeverReturnsTheViewer() {
        JpaConversationRepository conversations = new JpaRepositoryFactory(entityManager)
                .getRepository(JpaConversationRepository.class);
        UUID anyMember = entityManager.createQuery("SELECT m.userId FROM ConversationMemberEntity m", UUID.class)
                .setMaxResults(1).getResultStream().findFirst().orElse(UUID.randomUUID());

        List<Object[]> rows = conversations.findCoMembers(anyMember, Set.of(ConversationType.values()));

        assertThat(rows).allSatisfy(row -> {
            assertThat(row[0]).isInstanceOf(UUID.class).isNotEqualTo(anyMember);
            assertThat(row[1]).isInstanceOf(ConversationType.class);
        });
    }

    @Test
    void pagedFollowerAndFollowingQueriesRun() {
        var follows = new JpaRepositoryFactory(entityManager)
                .getRepository(iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaFollowRepository.class);
        var page = org.springframework.data.domain.PageRequest.of(0, 5,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        UUID nobody = new UUID(0, 0);

        assertThat(follows.findByTargetTypeAndTargetIdAndFollowerIdNotIn(
                iuh.fit.socialservice.domain.enums.FollowTargetType.USER, UUID.randomUUID(), List.of(nobody), page)).isEmpty();
        assertThat(follows.findByFollowerIdAndTargetTypeAndTargetIdNotIn(
                UUID.randomUUID(), iuh.fit.socialservice.domain.enums.FollowTargetType.USER, List.of(nobody), page)).isEmpty();
    }

    @Test
    void friendsOfManyPeopleQueryRuns() {
        JpaFriendshipRepository friendships = new JpaRepositoryFactory(entityManager)
                .getRepository(JpaFriendshipRepository.class);
        UUID someone = UUID.randomUUID();

        List<FriendshipEntity> rows = friendships.findByStatusAndUserIn(FriendshipStatus.ACCEPTED, Set.of(someone));

        assertThat(rows).isEmpty();
    }
}
