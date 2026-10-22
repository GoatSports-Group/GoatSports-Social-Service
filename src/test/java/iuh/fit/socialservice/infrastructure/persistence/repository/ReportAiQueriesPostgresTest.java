package iuh.fit.socialservice.infrastructure.persistence.repository;

import iuh.fit.socialservice.domain.enums.ReportStatus;
import iuh.fit.socialservice.infrastructure.persistence.entity.ContentReportEntity;
import iuh.fit.socialservice.infrastructure.persistence.entity.ModerationActionEntity;
import iuh.fit.socialservice.infrastructure.persistence.repository.jpa.JpaContentReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.io.FileInputStream;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Chạy thật các truy vấn gợi ý AI của báo cáo trên Postgres dev, trong một transaction luôn rollback (DDL của
 * Postgres cũng rollback được, nên cột mới chỉ tồn tại trong test nếu DB chưa chạy migration). Mock không bắt được
 * sai SQL native hay "nulls last". Tự bỏ qua khi không có DB.
 */
class ReportAiQueriesPostgresTest {
    private static EntityManagerFactory factory;
    private static EntityManager entityManager;

    @BeforeAll
    static void connect() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try (FileInputStream input = new FileInputStream("src/main/resources/application-dev.properties")) {
            Properties dev = new Properties();
            dev.load(input);
            String url = dev.getProperty("spring.datasource.url");
            DriverManager.getConnection(url).close();
            factory = new Configuration()
                    .addAnnotatedClass(ContentReportEntity.class)
                    .addAnnotatedClass(ModerationActionEntity.class)
                    .setProperty("hibernate.connection.url", url)
                    .setProperty("hibernate.hbm2ddl.auto", "none")
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
    void severeReportsComeFirstAndTheJobOnlyPicksUnreadOnes() {
        JpaContentReportRepository reports = new JpaRepositoryFactory(entityManager)
                .getRepository(JpaContentReportRepository.class);
        entityManager.getTransaction().begin();
        try {
            entityManager.createNativeQuery("""
                    alter table content_reports
                      add column if not exists ai_label varchar(20),
                      add column if not exists ai_severity int,
                      add column if not exists ai_reason varchar(300),
                      add column if not exists ai_checked_at timestamp,
                      add column if not exists ai_attempts int not null default 0""").executeUpdate();
            // Chỉ nhìn ba báo cáo của test: các báo cáo thật đang mở bị đẩy sang trạng thái khác trong transaction.
            entityManager.createNativeQuery("update content_reports set status = 'RESOLVED'").executeUpdate();
            UUID old = insert(LocalDateTime.now().minusHours(3));
            UUID severe = insert(LocalDateTime.now().minusHours(2));
            UUID mild = insert(LocalDateTime.now().minusHours(1));

            assertThat(reports.findAwaitingAiVerdict(3, 10)).extracting(ContentReportEntity::getReportId)
                    .containsExactly(old, severe, mild);

            reports.saveAiVerdict(severe, "SCAM", 3, "Xin chuyển khoản.", LocalDateTime.now());
            reports.saveAiVerdict(mild, "SPAM", 1, "Quảng cáo.", LocalDateTime.now());
            reports.addAiAttempts(old, 3);
            entityManager.clear();

            assertThat(reports.findAwaitingAiVerdict(3, 10)).isEmpty();
            List<ContentReportEntity> queue = reports.findQueue(
                    List.of(ReportStatus.PENDING, ReportStatus.REVIEWING), PageRequest.of(0, 10)).getContent();
            assertThat(queue).extracting(ContentReportEntity::getReportId).containsExactly(severe, mild, old);
            assertThat(queue.get(0).getAiLabel()).isEqualTo("SCAM");
            assertThat(queue.get(0).getAiReason()).isEqualTo("Xin chuyển khoản.");
        } finally {
            entityManager.getTransaction().rollback();
        }
    }

    private UUID insert(LocalDateTime createdAt) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery("""
                        insert into content_reports (report_id, reporter_id, target_type, target_id, reason, status,
                          created_at, updated_at)
                        values (?1, ?2, 'POST', ?3, 'test', 'PENDING', ?4, ?4)""")
                .setParameter(1, id).setParameter(2, UUID.randomUUID()).setParameter(3, UUID.randomUUID())
                .setParameter(4, createdAt).executeUpdate();
        return id;
    }
}
