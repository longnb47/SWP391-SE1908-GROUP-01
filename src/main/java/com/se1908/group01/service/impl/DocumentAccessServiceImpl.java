package com.se1908.group01.service.impl;

import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentAccessService;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class DocumentAccessServiceImpl implements DocumentAccessService {

	private final DocumentRepository documentRepository;

	public DocumentAccessServiceImpl(DocumentRepository documentRepository) {
		this.documentRepository = documentRepository;
	}

	@Override
	public Document getReadyDocumentForChat(Long userId, Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("Document ID is required");
		}

		var document = documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(documentId, userId)
				.or(() -> documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId))
				.orElseThrow(() -> new ResourceNotFoundException("Document not found or not accessible"));

		if (document.getStatus() != DocumentStatus.READY) {
			throw new IllegalArgumentException("Document is not ready for chat");
		}

		return document;
	}

	@Override
	public List<Document> getReadyDocumentsForChat(Long userId, List<Long> documentIds) {
		if (documentIds == null || documentIds.isEmpty()) {
			throw new IllegalArgumentException("Document IDs are required for SelectedDocuments mode");
		}
		return documentRepository.findAccessibleDocumentsByIdsAndStatus(documentIds, userId, DocumentStatus.READY);
	}

	@Override
	public List<Document> getAllReadyDocumentsForUser(Long userId, @Nullable Long folderId) {
		if (folderId != null) {
			return documentRepository.findAccessibleDocumentsByFolderAndStatus(userId, folderId, DocumentStatus.READY);
		}
		return documentRepository.findAllAccessibleDocumentsByStatus(userId, DocumentStatus.READY);
	}
}
