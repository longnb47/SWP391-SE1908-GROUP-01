package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.ChatAskRequest;
import com.se1908.group01.dto.ChatAskResponse;
import com.se1908.group01.dto.ChatMessageListResponse;
import com.se1908.group01.dto.ChatMessageResponse;
import com.se1908.group01.dto.ChatSessionResponse;
import com.se1908.group01.dto.CreateChatSessionRequest;
import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.dto.SendChatMessageRequest;
import com.se1908.group01.dto.UpdateChatSessionRequest;
import com.se1908.group01.service.ChatSessionService;
import com.se1908.group01.service.ChatService;
import com.se1908.group01.service.MultiChatService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;
	private final MultiChatService multiChatService;
	private final ChatSessionService chatSessionService;

	public ChatController(
			ChatService chatService,
			MultiChatService multiChatService,
			ChatSessionService chatSessionService
	) {
		this.chatService = chatService;
		this.multiChatService = multiChatService;
		this.chatSessionService = chatSessionService;
	}

	@PostMapping("/ask")
	public ApiResponse<ChatAskResponse> ask(@Valid @RequestBody ChatAskRequest request) {
		var response = chatService.ask(request);
		return ApiResponse.success("Ask document successfully", response);
	}

	@PostMapping("/ask-multi")
	public ApiResponse<MultiChatAskResponse> askMulti(@Valid @RequestBody MultiChatAskRequest request) {
		var response = multiChatService.askMulti(request);
		return ApiResponse.success("Multi-document chat processed", response);
	}

	@PostMapping("/sessions")
	public ApiResponse<ChatSessionResponse> createSession(
			@Valid @RequestBody CreateChatSessionRequest request
	) {
		return ApiResponse.success(
				"Create chat session successfully",
				chatSessionService.createSession(request)
		);
	}

	@GetMapping("/sessions")
	public ApiResponse<List<ChatSessionResponse>> getMySessions() {
		return ApiResponse.success(
				"Get chat sessions successfully",
				chatSessionService.getMySessions()
		);
	}

	@PatchMapping("/sessions/{sessionId}")
	public ApiResponse<ChatSessionResponse> updateSession(
			@PathVariable Long sessionId,
			@Valid @RequestBody UpdateChatSessionRequest request
	) {
		return ApiResponse.success(
				"Update chat session successfully",
				chatSessionService.updateSession(sessionId, request)
		);
	}

	@DeleteMapping("/sessions/{sessionId}")
	public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
		chatSessionService.deleteSession(sessionId);
		return ApiResponse.success("Delete chat session successfully", null);
	}

	@GetMapping("/sessions/{sessionId}/messages")
	public ApiResponse<ChatMessageListResponse> getMessages(
			@PathVariable Long sessionId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size
	) {
		return ApiResponse.success(
				"Get chat messages successfully",
				chatSessionService.getMessages(sessionId, page, size)
		);
	}

	@PostMapping("/sessions/{sessionId}/messages")
	public ApiResponse<ChatMessageResponse> sendMessage(
			@PathVariable Long sessionId,
			@Valid @RequestBody SendChatMessageRequest request
	) {
		return ApiResponse.success(
				"Send chat message successfully",
				chatSessionService.sendMessage(sessionId, request)
		);
	}
}
