package com.se1908.group01.service.impl;

import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentAccessService;
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
}
