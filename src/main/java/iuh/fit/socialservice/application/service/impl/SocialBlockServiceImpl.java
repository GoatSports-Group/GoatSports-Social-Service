package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.BlockUserCommand;
import iuh.fit.socialservice.application.dto.response.UserBlockResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.UserBlockRepository;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.domain.entity.UserBlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialBlockServiceImpl implements SocialBlockService {
    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;

    @Override
    @Transactional
    public UserBlockResponseCommand blockUser(BlockUserCommand command) throws ServiceException {
        if (command.getBlockerId().equals(command.getBlockedUserId())) {
            throw new ServiceException("Không thể tự chặn chính mình.");
        }

        if (userBlockRepository.existsByBlockerIdAndBlockedUserId(command.getBlockerId(), command.getBlockedUserId())) {
            throw new ServiceException("Người dùng này đã nằm trong danh sách chặn.");
        }

        UserBlock block = UserBlock.builder()
                .blockerId(command.getBlockerId())
                .blockedUserId(command.getBlockedUserId())
                .reason(command.getReason())
                .blockedAt(LocalDateTime.now())
                .build();
        UserBlock savedBlock = userBlockRepository.save(block);
        friendshipRepository.findBetweenUsers(command.getBlockerId(), command.getBlockedUserId())
                .ifPresent(friendship -> friendshipRepository.deleteById(friendship.getFriendshipId()));
        return mapToResponse(savedBlock);
    }

    @Override
    @Transactional
    public void unblockUser(UUID blockId, UUID actorUserId) throws ServiceException {
        UserBlock block = userBlockRepository.findById(blockId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy thông tin chặn người dùng."));

        if (!block.getBlockerId().equals(actorUserId)) {
            throw new ServiceException("Bạn không có quyền bỏ chặn bản ghi này.");
        }

        userBlockRepository.deleteById(blockId);
    }

    @Override
    public List<UserBlockResponseCommand> getBlockedUsers(UUID userId) {
        return userBlockRepository.findByBlockerId(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public boolean isBlocked(UUID u1, UUID u2) {
        return userBlockRepository.existsByBlockerIdAndBlockedUserId(u1, u2) ||
               userBlockRepository.existsByBlockerIdAndBlockedUserId(u2, u1);
    }

    private UserBlockResponseCommand mapToResponse(UserBlock b) {
        return UserBlockResponseCommand.builder()
                .blockId(b.getBlockId())
                .blockerId(b.getBlockerId())
                .blockedUserId(b.getBlockedUserId())
                .reason(b.getReason())
                .blockedAt(b.getBlockedAt())
                .build();
    }
}
