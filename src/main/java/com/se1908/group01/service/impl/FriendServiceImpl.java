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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;


    /**
     * Sends a new friend request from the current user to another user by email.
     * <p>
     * Validation checks:
     * <ul>
     *   <li>Ensures the receiver exists in the database.</li>
     *   <li>Prevents users from sending friend requests to themselves.</li>
     *   <li>Ensures the two users are not already friends.</li>
     *   <li>Ensures there is no active pending request between the two users in either direction.</li>
     * </ul>
     *
     * @param senderId the ID of the user sending the request
     * @param email    the email of the user receiving the request
     * @return a {@link FriendRequestResponse} containing details of the created request
     * @throws ResourceNotFoundException if the sender or receiver cannot be found
     * @throws IllegalArgumentException  if any validation rule is violated
     */
    @Override
    public FriendRequestResponse sendFriendRequest(Long senderId, String email) {
        User receiver = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        Long receiverId = receiver.getUserId();

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("You cannot send friend request to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        if (friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
            throw new IllegalArgumentException("Users are already friends");
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

    /**
     * Accepts a pending friend request.
     * <p>
     * This method validates the request ownership and status, creates a new {@link Friendship}
     * between the two users, updates the friend request status to {@link FriendRequestStatus#ACCEPTED},
     * and records the response timestamp.
     *
     * @param requestId the ID of the friend request to accept
     * @param userId    the ID of the current user accepting the request (must be the receiver)
     * @return a {@link FriendRequestResponse} with the updated ACCEPTED status
     * @throws ResourceNotFoundException if the request or users cannot be found
     * @throws IllegalArgumentException  if the user is not authorized, the request is not pending,
     *                                   or they are already friends
     */
    @Override
    public FriendRequestResponse acceptFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = getPendingRequestForReceiver(requestId, userId);

        Long senderId = friendRequest.getSender().getUserId();
        Long receiverId = friendRequest.getReceiver().getUserId();
        Long user1Id = Math.min(senderId, receiverId);
        Long user2Id = Math.max(senderId, receiverId);

        if (friendshipRepository.existsByUser_UserIdAndFriend_UserId(user1Id, user2Id)) {
            throw new IllegalArgumentException("Users are already friends");
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

        return mapToFriendRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Override
    public FriendRequestResponse rejectFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = getPendingRequestForReceiver(requestId, userId);

        friendRequest.setStatus(FriendRequestStatus.REJECTED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Override
    public FriendRequestResponse cancelFriendRequest(Long requestId, Long userId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendRequest.getSender().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to cancel this request");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        friendRequest.setStatus(FriendRequestStatus.CANCELLED);
        friendRequest.setRespondedAt(LocalDateTime.now());

        return mapToFriendRequestResponse(friendRequestRepository.save(friendRequest));
    }

    @Override
    public List<FriendRequestResponse> getIncomingRequests(Long userId) {
        return friendRequestRepository
                .findByReceiver_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(this::mapToFriendRequestResponse)
                .toList();
    }

    @Override
    public List<FriendRequestResponse> getOutgoingRequests(Long userId) {
        return friendRequestRepository
                .findBySender_UserIdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING)
                .stream()
                .map(this::mapToFriendRequestResponse)
                .toList();
    }

    @Override
    public void unfriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("You cannot unfriend yourself");
        }

        Long user1Id = Math.min(userId, friendId);
        Long user2Id = Math.max(userId, friendId);

        Friendship friendship = friendshipRepository
                .findByUser_UserIdAndFriend_UserId(user1Id, user2Id)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found"));

        friendshipRepository.delete(friendship);
    }

    /**
     * Retrieves the list of all friends for a given user.
     * <p>
     * This query fetches all relationships from the {@code friendships} table where the user
     * is either the initiator or the receiver, and maps the corresponding friend's information
     * to a {@link FriendResponse} DTO.
     *
     * @param userId the ID of the user whose friends are being retrieved
     * @return a {@link List} of {@link FriendResponse} DTOs representing the user's friends
     */
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

    private FriendRequest getPendingRequestForReceiver(Long requestId, Long userId) {
        FriendRequest friendRequest = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Friend request not found"));

        if (!friendRequest.getReceiver().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You are not allowed to respond to this request");
        }

        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }

        return friendRequest;
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
