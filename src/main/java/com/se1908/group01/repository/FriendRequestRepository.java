package com.se1908.group01.repository;

import com.se1908.group01.entity.FriendRequest;
import com.se1908.group01.enums.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    boolean existsBySender_UserIdAndReceiver_UserIdAndStatus(
            Long senderId,
            Long receiverId,
            FriendRequestStatus status
    );

    Optional<FriendRequest> findBySender_UserIdAndReceiver_UserIdAndStatus(
            Long senderId,
            Long receiverId,
            FriendRequestStatus status
    );
}
