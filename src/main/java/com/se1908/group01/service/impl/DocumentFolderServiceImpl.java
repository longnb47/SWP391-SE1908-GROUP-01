package com.se1908.group01.service.impl;

import com.se1908.group01.dto.DocumentFolderRequest;
import com.se1908.group01.dto.DocumentFolderResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentFolder;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentFolderService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DocumentFolderServiceImpl implements DocumentFolderService {

	private final CurrentUserService currentUserService;
	private final DocumentFolderRepository documentFolderRepository;
	private final DocumentRepository documentRepository;

	public DocumentFolderServiceImpl(
			CurrentUserService currentUserService,
			DocumentFolderRepository documentFolderRepository,
			DocumentRepository documentRepository
	) {
		this.currentUserService = currentUserService;
		this.documentFolderRepository = documentFolderRepository;
		this.documentRepository = documentRepository;
	}

	@Transactional
	@Override
	public DocumentFolderResponse createFolder(DocumentFolderRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var name = normalizeName(request.getName());
		if (documentFolderRepository.existsByUserIdAndNameIgnoreCase(userId, name)) {
			throw new IllegalArgumentException("Folder name already exists");
		}

		var folder = new DocumentFolder();
		folder.setUserId(userId);
		folder.setName(name);
		return toResponse(documentFolderRepository.save(folder));
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentFolderResponse> getMyFolders() {
		var userId = currentUserService.getCurrentUserId();
		return documentFolderRepository.findByUserIdOrderByNameAsc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	@Override
	public DocumentFolderResponse updateFolder(Long folderId, DocumentFolderRequest request) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedFolder(userId, folderId);
		var name = normalizeName(request.getName());
		if (documentFolderRepository.existsByUserIdAndNameIgnoreCaseAndFolderIdNot(userId, name, folderId)) {
			throw new IllegalArgumentException("Folder name already exists");
		}

		folder.setName(name);
		return toResponse(documentFolderRepository.save(folder));
	}

	@Transactional
	@Override
	public void deleteFolder(Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedFolder(userId, folderId);
		documentRepository.clearFolderForUser(userId, folder.getFolderId());
		documentFolderRepository.delete(folder);
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getFolderDocuments(Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var folder = findOwnedFolder(userId, folderId);
		return documentRepository.findByUserIdAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc(
						userId,
						folder.getFolderId()
				)
				.stream()
				.map(this::toDocumentResponse)
				.toList();
	}

	private DocumentFolder findOwnedFolder(Long userId, Long folderId) {
		if (folderId == null) {
			throw new IllegalArgumentException("folderId is required");
		}
		return documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
	}

	private String normalizeName(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Folder name is required");
		}
		return name.trim();
	}

	private DocumentFolderResponse toResponse(DocumentFolder folder) {
		var response = new DocumentFolderResponse();
		response.setFolderId(folder.getFolderId());
		response.setUserId(folder.getUserId());
		response.setName(folder.getName());
		response.setCreatedAt(folder.getCreatedAt());
		response.setUpdatedAt(folder.getUpdatedAt());
		return response;
	}

	private DocumentUploadResponse toDocumentResponse(Document doc) {
		var res = new DocumentUploadResponse();
		res.setDocumentId(doc.getDocumentId());
		res.setUserId(doc.getUserId());
		res.setFolderId(doc.getFolderId());
		res.setOriginalFileName(doc.getOriginalFileName());
		res.setS3Key(doc.getS3Key());
		res.setContentType(doc.getContentType());
		res.setFileSize(doc.getFileSize());
		res.setIsPublic(doc.getIsPublic());
		res.setIsDeleted(doc.getIsDeleted());
		res.setStatus(doc.getStatus());
		res.setUploadedAt(doc.getUploadedAt());
		res.setDeletedAt(doc.getDeletedAt());
		return res;
	}
}
