package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.ChatAskRequest;
import com.se1908.group01.dto.ChatAskResponse;
import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.service.ChatService;
import com.se1908.group01.service.MultiChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final ChatService chatService;
	private final MultiChatService multiChatService;

	public ChatController(ChatService chatService, MultiChatService multiChatService) {
		this.chatService = chatService;
		this.multiChatService = multiChatService;
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
}
