package com.se1908.group01.service.impl;

import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentAccessService;
import java.util.HashSet;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class DocumentAccessServiceImpl implements DocumentAccessService {

	private final DocumentRepository documentRepository;
	private final DocumentFolderRepository documentFolderRepository;

	public DocumentAccessServiceImpl(
			DocumentRepository documentRepository,
			DocumentFolderRepository documentFolderRepository
	) {
		this.documentRepository = documentRepository;
		this.documentFolderRepository = documentFolderRepository;
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
		var distinctDocumentIds = documentIds.stream().distinct().toList();
		var requestedDocumentIds = new HashSet<>(distinctDocumentIds);
		var documents = documentRepository.findAccessibleDocumentsByIdsAndStatus(
				distinctDocumentIds,
				userId,
				DocumentStatus.READY
		);
		var resolvedDocumentIds = documents.stream()
				.map(Document::getDocumentId)
				.collect(java.util.stream.Collectors.toSet());
		if (!resolvedDocumentIds.equals(requestedDocumentIds)) {
			throw new IllegalArgumentException(
					"One or more selected documents are unavailable, not ready, or not accessible");
		}
		return documents;
	}

	@Override
	public List<Document> getAllReadyDocumentsForUser(
			Long userId,
			@Nullable Long folderId,
			boolean includePublicDocuments
	) {
		if (folderId != null) {
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Document folder not found"));
			if (includePublicDocuments) {
				return documentRepository.findOwnedFolderAndPublicDocumentsByStatus(
						userId,
						folderId,
						DocumentStatus.READY
				);
			}
			return documentRepository.findOwnedDocumentsByFolderAndStatus(
					userId,
					folderId,
					DocumentStatus.READY
			);
		}
		if (includePublicDocuments) {
			return documentRepository.findAllAccessibleDocumentsByStatus(userId, DocumentStatus.READY);
		}
		return documentRepository.findOwnedDocumentsByStatus(userId, DocumentStatus.READY);
	}
}
