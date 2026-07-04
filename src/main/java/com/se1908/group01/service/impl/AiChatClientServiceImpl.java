package com.se1908.group01.service.impl;

import com.se1908.group01.dto.AiGenerationOptions;
import com.se1908.group01.service.AiChatClientService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiChatClientServiceImpl implements AiChatClientService {

	private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

	public AiChatClientServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
		this.chatClientBuilderProvider = chatClientBuilderProvider;
	}

	@Override
	public String ask(String prompt, AiGenerationOptions options) {
		var builder = chatClientBuilderProvider.getIfAvailable();
		if (builder == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Spring AI chat model is not configured. Set SPRING_AI_MODEL_CHAT=google-genai and GEMINI_CHAT_API_KEY.");
		}

		try {
			var chatOptions = GoogleGenAiChatOptions.builder()
					.model(options.model().getProviderModel())
					.temperature(options.temperature());
			return builder.build()
					.prompt()
					.options(chatOptions)
					.user(prompt)
					.call()
					.content();
		} catch (RuntimeException ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable", ex);
		}
	}
}
