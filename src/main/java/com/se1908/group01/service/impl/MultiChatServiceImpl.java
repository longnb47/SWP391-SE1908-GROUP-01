package com.se1908.group01.service.impl;

import com.se1908.group01.dto.MultiChatAskRequest;
import com.se1908.group01.dto.MultiChatAskResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentAccessService;
import com.se1908.group01.service.MultiChatService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MultiChatServiceImpl implements MultiChatService {

	private final DocumentAccessService documentAccessService;
	private final CurrentUserService currentUserService;

	public MultiChatServiceImpl(DocumentAccessService documentAccessService,
			CurrentUserService currentUserService) {
		this.documentAccessService = documentAccessService;
		this.currentUserService = currentUserService;
	}

	@Override
	public MultiChatAskResponse askMulti(MultiChatAskRequest request) {
		validateRequest(request);

		Long userId = currentUserService.getCurrentUserId();

		List<Document> documents;
		if ("SelectedDocuments".equals(request.getMode())) {
			documents = documentAccessService.getReadyDocumentsForChat(userId, request.getSelectedDocumentIds());
		} else {
			documents = documentAccessService.getAllReadyDocumentsForUser(userId, request.getFolderId());
		}

		return new MultiChatAskResponse(
				"Found " + documents.size() + " ready documents for this request. Multi-doc chat is not implemented yet.");
	}

	private void validateRequest(MultiChatAskRequest request) {
		if ("SelectedDocuments".equals(request.getMode())
				&& (request.getSelectedDocumentIds() == null || request.getSelectedDocumentIds().isEmpty())) {
			throw new IllegalArgumentException("selectedDocumentIds must be provided for SelectedDocuments mode");
		}
	}
}
