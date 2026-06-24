package com.se1908.group01.service.impl;

import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.dto.RetrievedChunk;
import com.se1908.group01.entity.Document;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.DocumentEmbeddingService;
import com.se1908.group01.service.MultiChatService;
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

	public MultiChatServiceImpl(
			DocumentAccessService documentAccessService,
			CurrentUserService currentUserService,
			DocumentEmbeddingService documentEmbeddingService,
			VectorSearchService vectorSearchService
	) {
		this.documentAccessService = documentAccessService;
		this.currentUserService = currentUserService;
		this.documentEmbeddingService = documentEmbeddingService;
		this.vectorSearchService = vectorSearchService;
	}

	@Override
	public MultiChatAskResponse askMulti(MultiChatAskRequest request) {
		validateRequest(request);

		var userId = currentUserService.getCurrentUserId();

		List<Document> documents;
		if ("SelectedDocuments".equals(request.getMode())) {
			documents = documentAccessService.getReadyDocumentsForChat(userId, request.getSelectedDocumentIds());
		} else {
			documents = documentAccessService.getAllReadyDocumentsForUser(userId, request.getFolderId());
		}

		var resolvedDocumentIds = documents.stream()
				.map(Document::getDocumentId)
				.toList();

		var questionEmbeddingVector = documentEmbeddingService.embedQuestion(request.getQuestion());

		var chunks = vectorSearchService.search(questionEmbeddingVector, resolvedDocumentIds, null, null, TOP_K);

		var distinctDocumentCount = chunks.stream()
				.map(c -> c.getChunk().getDocument().getDocumentId())
				.distinct()
				.count();

		var context = buildContext(chunks);

		return new MultiChatAskResponse(
				"Retrieved " + chunks.size() + " chunks from " + distinctDocumentCount + " documents. LLM not called yet.");
	}

	private String buildContext(List<RetrievedChunk> chunks) {
		if (chunks.isEmpty()) {
			return "";
		}
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
