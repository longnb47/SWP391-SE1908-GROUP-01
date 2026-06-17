package com.se1908.group01.controller;

import com.se1908.group01.dto.FriendRequestResponse;
import com.se1908.group01.dto.FriendResponse;
import com.se1908.group01.dto.SendFriendRequestRequest;
import com.se1908.group01.entity.User;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    public ResponseEntity<FriendRequestResponse> sendFriendRequest(
            Authentication authentication,
            @Valid @RequestBody SendFriendRequestRequest request
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.sendFriendRequest(
                        currentUserId,
                        request.getEmail()
                )
        );
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendRequestResponse>> getIncomingRequests(
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.getIncomingRequests(currentUserId)
        );
    }

    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendRequestResponse>> getOutgoingRequests(
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.getOutgoingRequests(currentUserId)
        );
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> acceptFriendRequest(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.acceptFriendRequest(
                        requestId,
                        currentUserId
                )
        );
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<FriendRequestResponse> rejectFriendRequest(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.rejectFriendRequest(
                        requestId,
                        currentUserId
                )
        );
    }

    @DeleteMapping("/requests/{requestId}/cancel")
    public ResponseEntity<FriendRequestResponse> cancelFriendRequest(
            Authentication authentication,
            @PathVariable Long requestId
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.cancelFriendRequest(
                        requestId,
                        currentUserId
                )
        );
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> unfriend(
            Authentication authentication,
            @PathVariable Long friendId
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        friendService.unfriend(currentUserId, friendId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends(
            Authentication authentication
    ) {
        Long currentUserId = getCurrentUserId(authentication);

        return ResponseEntity.ok(
                friendService.getFriends(currentUserId)
        );
    }

    private Long getCurrentUserId(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return user.getUserId();
    }
}
