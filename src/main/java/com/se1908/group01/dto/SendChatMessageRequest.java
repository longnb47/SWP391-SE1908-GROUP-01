package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;

/** Request chứa câu hỏi mới gửi vào một persistent chat session. */
public record SendChatMessageRequest(
		@NotBlank(message = "Question is required")
		String question
) {
}
