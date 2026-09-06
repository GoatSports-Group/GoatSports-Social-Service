package iuh.fit.socialservice.application.service.impl;

import iuh.fit.socialservice.application.dto.request.RespondFriendRequestCommand;
import iuh.fit.socialservice.application.dto.request.SendFriendRequestCommand;
import iuh.fit.socialservice.application.dto.response.FriendshipResponseCommand;
import iuh.fit.socialservice.application.exception.ServiceException;
import iuh.fit.socialservice.application.port.persistence.FriendshipRepository;
import iuh.fit.socialservice.application.service.SocialBlockService;
import iuh.fit.socialservice.application.service.SocialFriendService;
import iuh.fit.socialservice.domain.entity.Friendship;
import iuh.fit.socialservice.domain.enums.FriendshipStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialFriendServiceImpl implements SocialFriendService {
    private final FriendshipRepository friendshipRepository;
    private final SocialBlockService socialBlockService;

    @Override
    @Transactional
    public FriendshipResponseCommand sendFriendRequest(SendFriendRequestCommand command) throws ServiceException {
        if (command.getRequesterId().equals(command.getAddresseeId())) {
            throw new ServiceException("Không thể gửi lời mời kết bạn cho chính mình.");
        }

        if (socialBlockService.isBlocked(command.getRequesterId(), command.getAddresseeId())) {
            throw new ServiceException("Không thể gửi kết bạn do tài khoản đang bị chặn.");
        }

        Optional<Friendship> existing = friendshipRepository.findBetweenUsers(command.getRequesterId(), command.getAddresseeId());
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new ServiceException("Hai người đã là bạn bè.");
            } else if (f.getStatus() == FriendshipStatus.PENDING) {
                throw new ServiceException("Lời mời kết bạn đang chờ phản hồi.");
            } else {
                f.setStatus(FriendshipStatus.PENDING);
                f.setRequesterId(command.getRequesterId());
                f.setAddresseeId(command.getAddresseeId());
                f.setRequestedAt(LocalDateTime.now());
                f.setRespondedAt(null);
                return mapToResponse(friendshipRepository.save(f));
            }
        }

        Friendship newFriendship = Friendship.builder()
                .requesterId(command.getRequesterId())
                .addresseeId(command.getAddresseeId())
                .status(FriendshipStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        return mapToResponse(friendshipRepository.save(newFriendship));
    }

    @Override
    @Transactional
    public FriendshipResponseCommand respondFriendRequest(RespondFriendRequestCommand command) throws ServiceException {
        Friendship friendship = friendshipRepository.findById(command.getFriendshipId())
                .orElseThrow(() -> new ServiceException("Không tìm thấy lời mời kết bạn."));

        if (!friendship.getAddresseeId().equals(command.getActorUserId())) {
            throw new ServiceException("Chỉ người nhận mới có quyền chấp nhận hoặc từ chối.");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new ServiceException("Lời mời kết bạn này đã được phản hồi.");
        }

        friendship.setStatus(command.isAccepted() ? FriendshipStatus.ACCEPTED : FriendshipStatus.REJECTED);
        friendship.setRespondedAt(LocalDateTime.now());

        return mapToResponse(friendshipRepository.save(friendship));
    }

    @Override
    @Transactional
    public void unfriend(UUID friendshipId, UUID actorUserId) throws ServiceException {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new ServiceException("Không tìm thấy thông tin bạn bè."));

        if (!friendship.getRequesterId().equals(actorUserId) && !friendship.getAddresseeId().equals(actorUserId)) {
            throw new ServiceException("Bạn không có quyền thực hiện thao tác này.");
        }

        friendshipRepository.deleteById(friendshipId);
    }

    @Override
    public List<FriendshipResponseCommand> getFriendsList(UUID userId) {
        return friendshipRepository.findUserFriends(userId, FriendshipStatus.ACCEPTED)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<FriendshipResponseCommand> getPendingReceivedRequests(UUID userId) {
        return friendshipRepository.findByAddresseeIdAndStatus(userId, FriendshipStatus.PENDING)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<FriendshipResponseCommand> getPendingSentRequests(UUID userId) {
        return friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.PENDING)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private FriendshipResponseCommand mapToResponse(Friendship f) {
        return FriendshipResponseCommand.builder()
                .friendshipId(f.getFriendshipId())
                .requesterId(f.getRequesterId())
                .addresseeId(f.getAddresseeId())
                .status(f.getStatus())
                .requestedAt(f.getRequestedAt())
                .respondedAt(f.getRespondedAt())
                .build();
    }
}
