package iuh.fit.socialservice.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Dữ liệu mẫu cho môi trường dev. Bật bằng `app.demo-data.enabled=true`.
 *
 * <p>Chat nhóm của CLB. Bình thường club-service gọi
 * {@code PUT /internal/conversations/context/{contextId}} để tạo, nhưng seeder của club-service
 * ghi thẳng vào database nên không đi qua lời gọi đó — nếu không seed ở đây thì CLB mẫu
 * sẽ không có nhóm chat.
 *
 * <p>Id suy ra từ tên CLB theo đúng quy ước của club-service, nên
 * {@code conversations.context_id} khớp {@code clubs.club_id} và
 * {@code conversations.conversation_id} khớp {@code clubs.conversation_id}.
 */
@Slf4j
@Component
@Order(20)
@ConditionalOnProperty(prefix = "app.demo-data", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    private static UUID userId(String email) {
        return UUID.nameUUIDFromBytes(("goatsports:user:" + email).getBytes(StandardCharsets.UTF_8));
    }

    private static UUID id(String kind, String key) {
        return UUID.nameUUIDFromBytes(("goatsports:" + kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private record MemberSeed(String email, String role) {}

    private record ClubChatSeed(String clubName, List<MemberSeed> members) {}

    private static final List<ClubChatSeed> CLUB_CHATS = List.of(
            new ClubChatSeed("CLB Bóng Đá Sài Gòn Phủi", List.of(
                    new MemberSeed("huy.danggia@goatsports.vn", "OWNER"),
                    new MemberSeed("tung.vuthanh@goatsports.vn", "ADMIN"),
                    new MemberSeed("minh.hoangnhat@goatsports.vn", "MEMBER"),
                    new MemberSeed("dang.trinhhai@goatsports.vn", "MEMBER"),
                    new MemberSeed("anh.ngoduc@goatsports.vn", "MEMBER"),
                    new MemberSeed("minh.nguyenvan@goatsports.vn", "MEMBER"),
                    new MemberSeed("phuc.tranhoang@goatsports.vn", "MEMBER"),
                    new MemberSeed("duc.ngominh@goatsports.vn", "MEMBER"))),
            new ClubChatSeed("CLB Cầu Lông Phú Nhuận", List.of(
                    new MemberSeed("linh.buikhanh@goatsports.vn", "OWNER"),
                    new MemberSeed("trang.lythu@goatsports.vn", "MEMBER"),
                    new MemberSeed("ngoc.caobao@goatsports.vn", "MEMBER"),
                    new MemberSeed("an.nguyenvan@goatsports.vn", "MEMBER"),
                    new MemberSeed("nam.lehoang@goatsports.vn", "MEMBER"),
                    new MemberSeed("kiet.dotuan@goatsports.vn", "MEMBER")))
    );

    @Override
    @Transactional
    public void run(String... args) {
        int conversations = 0;
        int members = 0;
        for (ClubChatSeed chat : CLUB_CHATS) {
            conversations += insertConversation(chat);
            for (MemberSeed member : chat.members()) {
                members += insertMember(chat, member);
            }
        }
        log.info("Dữ liệu mẫu: {} nhóm chat CLB và {} thành viên (bỏ qua bản ghi đã có)",
                conversations, members);
    }

    private static UUID conversationId(String clubName) {
        return id("conversation", id("club", clubName).toString());
    }

    private int insertConversation(ClubChatSeed seed) {
        return entityManager.createNativeQuery("""
                        insert into conversations (conversation_id, type, name, context_id,
                                                   created_at, created_by, updated_at, updated_by)
                        select :conversationId, 'CLUB', :name, :contextId,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (
                            select 1 from conversations where conversation_id = :conversationId)
                        """)
                .setParameter("conversationId", conversationId(seed.clubName()))
                .setParameter("name", seed.clubName())
                .setParameter("contextId", id("club", seed.clubName()))
                .executeUpdate();
    }

    private int insertMember(ClubChatSeed chat, MemberSeed member) {
        return entityManager.createNativeQuery("""
                        insert into conversation_members (member_id, conversation_id, user_id, role, joined_at,
                                                          created_at, created_by, updated_at, updated_by)
                        select :memberId, :conversationId, :userId, :role, :joinedAt,
                               now(), 'demo-seed', now(), 'demo-seed'
                        where not exists (
                            select 1 from conversation_members
                            where conversation_id = :conversationId and user_id = :userId)
                        """)
                .setParameter("memberId", id("conversation-member", chat.clubName() + "|" + member.email()))
                .setParameter("conversationId", conversationId(chat.clubName()))
                .setParameter("userId", userId(member.email()))
                .setParameter("role", member.role())
                .setParameter("joinedAt", LocalDateTime.now().minusDays(20))
                .executeUpdate();
    }
}
