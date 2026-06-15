package com.se1908.group01.service.impl;

import com.se1908.group01.config.S3Properties;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.dto.FileAccessUrlResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentFolderRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.repository.DocumentTagRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.DocumentIngestionService;
import com.se1908.group01.service.DocumentService;
import com.se1908.group01.service.FileValidationService;
import com.se1908.group01.service.S3StorageService;
import com.se1908.group01.util.FilenameSanitizer;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentServiceImpl implements DocumentService {

	private final FileValidationService fileValidationService;
	private final S3StorageService s3StorageService;
	private final S3Properties s3Properties;
	private final DocumentRepository documentRepository;
	private final DocumentFolderRepository documentFolderRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final DocumentTagRepository documentTagRepository;
	private final DocumentIngestionService documentIngestionService;
	private final CurrentUserService currentUserService;

	public DocumentServiceImpl(
			FileValidationService fileValidationService,
			S3StorageService s3StorageService,
			S3Properties s3Properties,
			DocumentRepository documentRepository,
			DocumentFolderRepository documentFolderRepository,
			DocumentChunkRepository documentChunkRepository,
			DocumentTagRepository documentTagRepository,
			DocumentIngestionService documentIngestionService,
			CurrentUserService currentUserService
	) {
		this.fileValidationService = fileValidationService;
		this.s3StorageService = s3StorageService;
		this.s3Properties = s3Properties;
		this.documentRepository = documentRepository;
		this.documentFolderRepository = documentFolderRepository;
		this.documentChunkRepository = documentChunkRepository;
		this.documentTagRepository = documentTagRepository;
		this.documentIngestionService = documentIngestionService;
		this.currentUserService = currentUserService;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public DocumentUploadResponse upload(MultipartFile file, Boolean isPublic) throws IOException {
		var userId = currentUserService.getCurrentUserId();
		fileValidationService.validateForUpload(file);

		var originalName = FilenameSanitizer.sanitize(file.getOriginalFilename());
		var key = buildObjectKey(userId, originalName);

		s3StorageService.uploadPrivate(file, key);

		Document doc;
		try {
			doc = new Document();
			doc.setUserId(userId);
			doc.setOriginalFileName(originalName);
			doc.setS3Key(key);
			doc.setContentType(file.getContentType());
			doc.setFileSize(file.getSize());
			doc.setIsPublic(Boolean.TRUE.equals(isPublic));
			doc.setStatus(DocumentStatus.UPLOADED);

			doc = documentRepository.save(doc);
			try {
				documentIngestionService.ingest(doc, file);
			} catch (RuntimeException | IOException ex) {
				doc.setStatus(DocumentStatus.FAILED);
				doc = documentRepository.save(doc);
			}
		} catch (RuntimeException ex) {
			try {
				s3StorageService.delete(key);
			} catch (RuntimeException ignored) {
				// best effort cleanup
			}
			throw ex;
		}

		return toResponse(doc);
	}

	@Transactional
	@Override
	public DocumentUploadResponse moveToTrash(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedDocument(userId, documentId);
		if (!Boolean.TRUE.equals(doc.getIsDeleted())) {
			doc.setIsDeleted(Boolean.TRUE);
			doc.setDeletedAt(Instant.now());
			doc = documentRepository.save(doc);
		}
		return toResponse(doc);
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getMyDocuments() {
		var userId = currentUserService.getCurrentUserId();
		return documentRepository.findByUserIdAndIsDeletedFalseOrderByUploadedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentUploadResponse getDocumentDetail(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toResponse(findOwnedActiveDocument(userId, documentId));
	}

	@Transactional
	@Override
	public DocumentUploadResponse renameDocument(Long documentId, String originalFileName) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		doc.setOriginalFileName(normalizeOriginalFileName(originalFileName));
		return toResponse(documentRepository.save(doc));
	}

	@Transactional
	@Override
	public DocumentUploadResponse moveDocumentToFolder(Long documentId, Long folderId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		if (folderId != null) {
			documentFolderRepository.findByFolderIdAndUserId(folderId, userId)
					.orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
		}
		doc.setFolderId(folderId);
		return toResponse(documentRepository.save(doc));
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getPreviewUrl(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toFileAccessUrlResponse(findOwnedActiveDocument(userId, documentId), false);
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getDownloadUrl(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		return toFileAccessUrlResponse(findOwnedActiveDocument(userId, documentId), true);
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getPublicDocuments() {
		return documentRepository.findByIsPublicTrueAndIsDeletedFalseOrderByUploadedAtDesc()
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	@Override
	public DocumentUploadResponse getPublicDocumentDetail(Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId)
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Public document not found"));
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getPublicPreviewUrl(Long documentId) {
		return toFileAccessUrlResponse(findPublicActiveDocument(documentId), false);
	}

	@Transactional(readOnly = true)
	@Override
	public FileAccessUrlResponse getPublicDownloadUrl(Long documentId) {
		return toFileAccessUrlResponse(findPublicActiveDocument(documentId), true);
	}

	@Transactional
	@Override
	public DocumentUploadResponse updateVisibility(Long documentId, Boolean isPublic) {
		if (isPublic == null) {
			throw new IllegalArgumentException("isPublic is required");
		}
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedActiveDocument(userId, documentId);
		doc.setIsPublic(isPublic);
		return toResponse(documentRepository.save(doc));
	}

	@Transactional(readOnly = true)
	@Override
	public List<DocumentUploadResponse> getTrash() {
		var userId = currentUserService.getCurrentUserId();
		return documentRepository.findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	@Override
	public DocumentUploadResponse restoreFromTrash(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedDocument(userId, documentId);
		if (Boolean.TRUE.equals(doc.getIsDeleted())) {
			doc.setIsDeleted(Boolean.FALSE);
			doc.setDeletedAt(null);
			doc = documentRepository.save(doc);
		}
		return toResponse(doc);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public void deletePermanently(Long documentId) {
		var userId = currentUserService.getCurrentUserId();
		var doc = findOwnedDocument(userId, documentId);
		if (!Boolean.TRUE.equals(doc.getIsDeleted())) {
			throw new IllegalArgumentException("Document must be in trash before permanent delete");
		}

		if (doc.getS3Key() != null && !doc.getS3Key().isBlank()) {
			s3StorageService.delete(doc.getS3Key());
		}
		documentTagRepository.deleteByDocumentDocumentId(doc.getDocumentId());
		documentChunkRepository.deleteByDocumentDocumentId(doc.getDocumentId());
		documentRepository.delete(doc);
	}

	private String buildObjectKey(Long userId, String sanitizedFilename) {
		var prefix = s3Properties.getKeyPrefix();
		if (prefix == null) {
			prefix = "";
		}
		prefix = prefix.trim();
		if (!prefix.isEmpty() && !prefix.endsWith("/")) {
			prefix = prefix + "/";
		}

		var uuid = UUID.randomUUID();
		return prefix + "documents/" + userId + "/" + uuid + "-" + sanitizedFilename;
	}

	private Document findOwnedDocument(Long userId, Long documentId) {
		validateUserId(userId);
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndUserId(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
	}

	private Document findOwnedActiveDocument(Long userId, Long documentId) {
		validateUserId(userId);
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndUserIdAndIsDeletedFalse(documentId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found"));
	}

	private Document findPublicActiveDocument(Long documentId) {
		if (documentId == null) {
			throw new IllegalArgumentException("documentId is required");
		}
		return documentRepository.findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Public document not found"));
	}

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
	}

	private String normalizeOriginalFileName(String originalFileName) {
		var sanitized = FilenameSanitizer.sanitize(originalFileName);
		if (sanitized.length() > 512) {
			sanitized = sanitized.substring(sanitized.length() - 512);
		}
		return sanitized;
	}

	private FileAccessUrlResponse toFileAccessUrlResponse(Document doc, boolean download) {
		var expiresAt = Instant.now().plus(Duration.ofMinutes(s3Properties.getPresignedUrlExpirationMinutes()));
		var url = s3StorageService.createPresignedGetUrl(
				doc.getS3Key(),
				doc.getOriginalFileName(),
				doc.getContentType(),
				download
		);
		return new FileAccessUrlResponse(url, expiresAt, doc.getOriginalFileName(), doc.getContentType());
	}

	private DocumentUploadResponse toResponse(Document doc) {
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
