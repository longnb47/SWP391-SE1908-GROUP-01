package com.se1908.group01.service.impl;

import com.se1908.group01.dto.FriendRequestResponse;
import com.se1908.group01.dto.FriendResponse;
import com.se1908.group01.entity.FriendRequest;
import com.se1908.group01.entity.Friendship;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.FriendRequestStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.FriendRequestRepository;
import com.se1908.group01.repository.FriendshipRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;


    @Override
    public FriendRequestResponse sendFriendRequest(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("You can not sent friend request to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Reciever not found"));

        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        if (friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
            throw new IllegalArgumentException("User are already friends");
        }

        boolean pendingRequestExists = friendRequestRepository
                .existsBySender_UserIdAndReceiver_UserIdAndStatus(
                        senderId,
                        receiverId,
                        FriendRequestStatus.PENDING
                );

        if (pendingRequestExists) {
            throw new IllegalArgumentException("Friend request already exists");
        }

        boolean reversePendingRequestExists = friendRequestRepository
                .existsBySender_UserIdAndReceiver_UserIdAndStatus(
                        receiverId,
                        senderId,
                        FriendRequestStatus.PENDING
                );

        if (reversePendingRequestExists) {
            throw new IllegalArgumentException("This user has already sent you a friend request");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();

        return mapToFriendRequestResponse(
                friendRequestRepository.save(friendRequest)
        );
    }

    @Override
    public FriendRequestResponse acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendRequest.getReceiver().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to accept this request");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        Long senderId = friendRequest.getSender().getUserId();
        Long receiverId = friendRequest.getReceiver().getUserId();

        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        if (friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
            throw new IllegalArgumentException("Users are already firends");
        }

        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Friendship friendship = Friendship.builder()
                .user(user1)
                .friend(user2)
                .build();

        friendshipRepository.save(friendship);

        friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(
                friendRequestRepository.save(friendRequest)
        );
    }

    @Override
    public FriendRequestResponse rejectFriendRequest(Long requestId, Long userId) {

        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendRequest.getReceiver().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to reject this request");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(
                friendRequestRepository.save(friendRequest)
        );
    }

    @Override
    public void unfriend(Long userId, Long friendId) {
        Long user1Id = Math.min(userId, friendId);
        Long user2Id = Math.max(userId, friendId);

        Friendship friendship = friendshipRepository
                .findByUser_UserIdAndFriend_UserId(user1Id, user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));

        friendshipRepository.delete(friendship);
    }

    @Override
    public List<FriendResponse> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository
                .findByUser_UserIdOrFriend_UserId(userId, userId);

        return friendships.stream()
                .map(friendship -> {
                    User friend = friendship.getUser().getUserId().equals(userId)
                            ? friendship.getFriend()
                            : friendship.getUser();

                    return FriendResponse.builder()
                            .friendshipId(friendship.getFriendshipId())
                            .userId(friend.getUserId())
                            .fullName(friend.getFullName())
                            .email(friend.getEmail())
                            .createdAt(friendship.getCreatedAt())
                            .build();
                })
                .toList();
    }


    private FriendRequestResponse mapToFriendRequestResponse(FriendRequest friendRequest) {
        return FriendRequestResponse.builder()
                .requestId(friendRequest.getRequestId())
                .senderId(friendRequest.getSender().getUserId())
                .senderName(friendRequest.getSender().getFullName())
                .senderEmail(friendRequest.getSender().getEmail())
                .receiverId(friendRequest.getReceiver().getUserId())
                .receiverName(friendRequest.getReceiver().getFullName())
                .receiverEmail(friendRequest.getReceiver().getEmail())
                .status(friendRequest.getStatus().name())
                .createdAt(friendRequest.getCreatedAt())
                .respondedAt(friendRequest.getRespondedAt())
                .build();
    }
}
