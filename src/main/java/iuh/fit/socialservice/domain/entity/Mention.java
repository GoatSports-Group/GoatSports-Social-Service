package iuh.fit.socialservice.domain.entity;

import java.util.UUID;

/**
 * Mot nguoi duoc nhac ten trong bai / binh luan. {@code name} la dung chuoi da chen vao noi dung
 * ("@name"), luu lai de lien ket van dung khi nguoi do doi ten sau nay.
 */
public record Mention(UUID userId, String name) {
}
