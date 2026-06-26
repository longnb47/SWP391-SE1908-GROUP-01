package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.FriendRequestResponse;
import com.se1908.group01.dto.FriendResponse;
import com.se1908.group01.dto.SendFriendRequestRequest;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final CurrentUserService currentUserService;

    @PostMapping("/request")
    public ApiResponse<FriendRequestResponse> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.sendFriendRequest(currentUserId, request.getEmail());
        return ApiResponse.success("Send friend request successfully", response);
    }

    @GetMapping("/requests/incoming")
    public ApiResponse<List<FriendRequestResponse>> getIncomingRequests() {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.getIncomingRequests(currentUserId);
        return ApiResponse.success("Get incoming friend requests successfully", response);
    }

    @GetMapping("/requests/outgoing")
    public ApiResponse<List<FriendRequestResponse>> getOutgoingRequests() {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.getOutgoingRequests(currentUserId);
        return ApiResponse.success("Get outgoing friend requests successfully", response);
    }

    @PostMapping("/requests/{requestId}/accept")
    public ApiResponse<FriendRequestResponse> acceptFriendRequest(
            @PathVariable Long requestId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.acceptFriendRequest(requestId, currentUserId);
        return ApiResponse.success("Accept friend request successfully", response);
    }

    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<FriendRequestResponse> rejectFriendRequest(
            @PathVariable Long requestId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.rejectFriendRequest(requestId, currentUserId);
        return ApiResponse.success("Reject friend request successfully", response);
    }

    @DeleteMapping("/requests/{requestId}/cancel")
    public ApiResponse<FriendRequestResponse> cancelFriendRequest(
            @PathVariable Long requestId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.cancelFriendRequest(requestId, currentUserId);
        return ApiResponse.success("Cancel friend request successfully", response);
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> unfriend(
            @PathVariable Long friendId
    ) {
        Long currentUserId = currentUserService.getCurrentUserId();

        friendService.unfriend(currentUserId, friendId);

        return ApiResponse.success("Unfriend successfully", null);
    }

    @GetMapping
    public ApiResponse<List<FriendResponse>> getFriends() {
        Long currentUserId = currentUserService.getCurrentUserId();

        var response = friendService.getFriends(currentUserId);
        return ApiResponse.success("Get friends successfully", response);
    }
}
