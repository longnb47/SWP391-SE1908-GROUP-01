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
/**
 * Adapter nối service nghiệp vụ với Spring AI Google GenAI ChatClient.
 * Lớp này nhận prompt đã bị giới hạn theo document context và trả nội dung answer từ model.
 */
public class AiChatClientServiceImpl implements AiChatClientService {

	// ObjectProvider cho phép application vẫn khởi động khi chưa cấu hình ChatClient/provider.
	private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

	public AiChatClientServiceImpl(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
		// Spring inject provider; ChatClient thật chỉ được lấy khi có request AI.
		this.chatClientBuilderProvider = chatClientBuilderProvider;
	}

	@Override
	public String ask(String prompt, AiGenerationOptions options) {
		// Lấy ChatClient do Spring cấu hình; thiếu provider thì trả SERVICE_UNAVAILABLE thay vì giả lập answer.
		var builder = chatClientBuilderProvider.getIfAvailable();
		if (builder == null) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Spring AI chat model is not configured. Set SPRING_AI_MODEL_CHAT=google-genai and GEMINI_CHAT_API_KEY.");
		}

		try {
			// Truyền model/temperature đã được resolve và prompt RAG vào Google GenAI.
			var chatOptions = GoogleGenAiChatOptions.builder()
					// Chuyển model nghiệp vụ sang tên model mà Google GenAI hiểu.
					.model(options.model().getProviderModel())
					// Temperature thấp cho câu trả lời bám sát tài liệu và ít ngẫu nhiên hơn.
					.temperature(options.temperature());
			return builder.build()
					// Bắt đầu tạo một prompt request mới, độc lập với request trước đó.
					.prompt()
					// Gắn model và temperature đã resolve cho riêng lần gọi này.
					.options(chatOptions)
					// Toàn bộ RAG prompt được truyền như một user message tới model.
					.user(prompt)
					// Thực hiện synchronous network call tới AI provider.
					.call()
					// Chỉ lấy phần text answer, bỏ metadata provider khỏi response nghiệp vụ.
					.content();
		} catch (RuntimeException ex) {
			// Không lộ chi tiết lỗi/key/provider cho client; giữ exception gốc làm cause để log/debug.
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is unavailable", ex);
		}
	}
}
