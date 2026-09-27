package iuh.fit.socialservice.domain.service;

import iuh.fit.socialservice.domain.enums.AttachmentType;

import java.util.List;
import java.util.UUID;

/**
 * Anh gui trong tin nhan. Mot tin nhan mang ca nhom anh (toi da {@link #MAX_IMAGES}) de nguoi nhan thay
 * chung la mot lan gui, khong phai N tin rieng.
 */
public final class ChatAttachmentPolicy {
    public static final int MAX_IMAGES = 10;
    /** Trung voi tran cua thu muc "chat-messages" trong UploadPolicy cua storage-service. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    /** Thu muc R2 cua anh chat. Khong nam trong danh sach public cua storage-service → doc qua URL ky han. */
    public static final String FOLDER = "chat-messages";

    private ChatAttachmentPolicy() {
    }

    public record Item(String storageKey, AttachmentType type, Long fileSize) {
    }

    /** null khi hop le; nguoc lai la thong bao loi hien cho nguoi dung. */
    public static String violation(List<Item> items, UUID senderId) {
        if (items == null || items.isEmpty()) return null;
        if (items.size() > MAX_IMAGES) return "Mỗi tin nhắn gửi tối đa " + MAX_IMAGES + " ảnh.";
        for (Item item : items) {
            if (item == null || item.storageKey() == null || item.storageKey().isBlank()) {
                return "Thông tin ảnh không hợp lệ.";
            }
            if (item.type() != AttachmentType.IMAGE) return "Tin nhắn hiện chỉ hỗ trợ gửi ảnh.";
            if (item.fileSize() != null && (item.fileSize() <= 0 || item.fileSize() > MAX_BYTES)) {
                return "Mỗi ảnh tối đa 10 MB.";
            }
            if (!ownedBy(item.storageKey().trim(), senderId)) return "Ảnh không thuộc về bạn.";
        }
        return null;
    }

    /**
     * Khoa phai la anh do chinh nguoi gui tai len (storage dat userId vao khoa luc cap URL tai len), neu
     * khong thi ai biet khoa anh cua nguoi khac cung dinh kem va "doc" duoc anh do trong hoi thoai cua minh.
     * Chap nhan ca khoa da promote (bo "temp/") de gui lai mot tin loi van dung duoc.
     */
    static boolean ownedBy(String key, UUID senderId) {
        if (senderId == null || key.contains("..")) return false;
        String owned = FOLDER + "/" + senderId + "/";
        return key.startsWith("temp/" + owned) || key.startsWith(owned);
    }

    /** Dong xem truoc trong danh sach hoi thoai cho tin chi co anh. */
    public static String preview(String content, int imageCount) {
        if (content != null && !content.isBlank()) return content;
        if (imageCount <= 0) return content;
        return imageCount == 1 ? "Đã gửi một ảnh" : "Đã gửi " + imageCount + " ảnh";
    }
}
