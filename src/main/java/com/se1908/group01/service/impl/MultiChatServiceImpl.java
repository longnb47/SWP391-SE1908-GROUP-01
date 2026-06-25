package com.se1908.group01.service.impl;

import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.entity.Document;
import com.se1908.group01.enums.ChatMode;
import com.se1908.group01.enums.KnowledgePolicy;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.MultiChatService;
import com.se1908.group01.service.PromptBuilderService;
import com.se1908.group01.service.VectorSearchService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MultiChatServiceImpl implements MultiChatService {

	private static final int TOP_K = 10;

	private final DocumentAccessService documentAccessService;
	private final CurrentUserService currentUserService;
	private final DocumentEmbeddingService documentEmbeddingService;
	private final VectorSearchService vectorSearchService;
	private final PromptBuilderService promptBuilderService;

	public MultiChatServiceImpl(
			DocumentAccessService documentAccessService,
			CurrentUserService currentUserService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService,
			PromptBuilderService promptBuilderService
	) {
		this.documentAccessService = documentAccessService;
		this.currentUserService = currentUserService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
		this.promptBuilderService = promptBuilderService;
	}

	@Override
	public MultiChatAskResponse askMulti(MultiChatAskRequest request) {
		validateRequest(request);

		var userId = currentUserService.getCurrentUserId();

		List<Document> documents;
		ChatMode chatMode;
		if ("SelectedDocuments".equals(request.getMode())) {
			chatMode = ChatMode.SELECTED_DOCUMENTS;
			documents = documentAccessService.getReadyDocumentsForChat(userId, request.getSelectedDocumentIds());
		} else {
			chatMode = ChatMode.USER_STORAGE;
			documents = documentAccessService.getAllReadyDocumentsForUser(userId, request.getFolderId());
		}

		if (documents.isEmpty()) {
			return new MultiChatAskResponse(noContextMessage(chatMode));
		}

		var resolvedDocumentIds = documents.stream()
				.map(Document::getDocumentId)
				.toList();

		var questionEmbeddingVector = documentEmbeddingService.embedQuestion(request.getQuestion());

		List<RetrievedChunk> chunks;
		if (chatMode == ChatMode.SELECTED_DOCUMENTS) {
			chunks = vectorSearchService.search(questionEmbeddingVector, resolvedDocumentIds, null, null, TOP_K);
		} else {
			// Pass userId so visibility filtering is applied correctly for UserStorage mode
			chunks = vectorSearchService.search(questionEmbeddingVector, resolvedDocumentIds, userId, request.getFolderId(), TOP_K);
		}

		if (chunks.isEmpty()) {
			return new MultiChatAskResponse(noContextMessage(chatMode));
		}

		var context = buildContext(chunks);

		// Phase 5 (dev): build prompt and return as answer for inspection — LLM not called yet
		var prompt = promptBuilderService.buildMultiDocumentQuestionPrompt(
				chatMode, KnowledgePolicy.DOCUMENTS_ONLY, context, request.getQuestion());

		return new MultiChatAskResponse(prompt);
	}

	private String noContextMessage(ChatMode mode) {
		return switch (mode) {
			case SELECTED_DOCUMENTS ->
					"I cannot find this information in the documents you selected.";
			case USER_STORAGE ->
					"I cannot find sufficient information in your documents (and public documents) to answer this question.";
		};
	}

	private String buildContext(List<RetrievedChunk> chunks) {
		var sb = new StringBuilder();
		for (var retrieved : chunks) {
			var chunk = retrieved.getChunk();
			sb.append("[Document ").append(chunk.getDocument().getDocumentId())
					.append(", chunk ").append(chunk.getChunkIndex()).append("]\n")
					.append(chunk.getContent())
					.append("\n\n");
		}
		return sb.toString().trim();
	}

	private void validateRequest(MultiChatAskRequest request) {
		if ("SelectedDocuments".equals(request.getMode())
				&& (request.getSelectedDocumentIds() == null || request.getSelectedDocumentIds().isEmpty())) {
			throw new IllegalArgumentException("selectedDocumentIds must be provided for SelectedDocuments mode");
		}
	}
}
