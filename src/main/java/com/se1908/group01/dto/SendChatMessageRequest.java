package com.se1908.group01.dto;

import jakarta.validation.constraints.NotBlank;

public record SendChatMessageRequest(
		@NotBlank(message = "Question is required")
		String question
) {
}
