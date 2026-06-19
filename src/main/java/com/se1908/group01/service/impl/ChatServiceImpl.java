package com.se1908.group01.service.impl;

import com.se1908.group01.dto.ChatAskRequest;
import com.se1908.group01.dto.ChatAskResponse;
import com.se1908.group01.dto.ChatSourceResponse;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.service.AiChatClientService;
import com.se1908.group01.service.ChatService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ChatServiceImpl implements ChatService {

	private static final int TOP_K = 5;

	private final CurrentUserService currentUserService;
	private final DocumentAccessService documentAccessService;
	private final DocumentEmbeddingService documentEmbeddingService;
	private final VectorSearchService vectorSearchService;
	private final PromptBuilderService promptBuilderService;
	private final AiChatClientService aiChatClientService;

	public ChatServiceImpl(
			CurrentUserService currentUserService,
			DocumentAccessService documentAccessService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService,
			PromptBuilderService promptBuilderService,
			AiChatClientService aiChatClientService
	) {
		this.currentUserService = currentUserService;
		this.documentAccessService = documentAccessService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
		this.promptBuilderService = promptBuilderService;
		this.aiChatClientService = aiChatClientService;
	}

	@Override
	public ChatAskResponse ask(ChatAskRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Chat request is required");
		}
		if (!StringUtils.hasText(request.getQuestion())) {
			throw new IllegalArgumentException("Question is required");
		}

		var userId = currentUserService.getCurrentUserId();
		var document = documentAccessService.getReadyDocumentForChat(userId, request.getDocumentId());
		var queryVector = documentEmbeddingService.embedVectors(List.of(request.getQuestion())).stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Failed to generate question embedding"));
		var chunks = vectorSearchService.search(document.getDocumentId(), queryVector, TOP_K);
		if (chunks.isEmpty()) {
			throw new IllegalArgumentException("Document has no indexed content for chat");
		}

		var prompt = promptBuilderService.buildDocumentQuestionPrompt(request.getQuestion(), chunks);
		var answer = aiChatClientService.ask(prompt);
		return new ChatAskResponse(document.getDocumentId(), answer, toSources(chunks));
	}

	private List<ChatSourceResponse> toSources(List<RetrievedChunk> chunks) {
		return chunks.stream()
				.map(retrieved -> {
					var chunk = retrieved.getChunk();
					return new ChatSourceResponse(
							chunk.getChunkId(),
							chunk.getChunkIndex(),
							chunk.getPageNumber(),
							roundScore(retrieved.getScore())
					);
				})
				.toList();
	}

	private double roundScore(double score) {
		return Math.round(score * 10000.0) / 10000.0;
	}
}
