package com.se1908.group01.dto;

import com.se1908.group01.enums.ChatMessageRole;
import com.se1908.group01.enums.ChatMessageStatus;
import java.time.Instant;
import java.util.List;

public record ChatMessageResponse(
		Long messageId,
		ChatMessageRole role,
		String content,
		ChatMessageStatus status,
		Instant createdAt,
		List<ChatMessageSourceResponse> sources
) {
}
