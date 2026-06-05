package com.se1908.group01.service;

import com.se1908.group01.config.S3Properties;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.DocumentChunkRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.util.FilenameSanitizer;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

	private final FileValidationService fileValidationService;
	private final S3StorageService s3StorageService;
	private final S3Properties s3Properties;
	private final DocumentRepository documentRepository;
	private final DocumentChunkRepository documentChunkRepository;
	private final DocumentIngestionService documentIngestionService;

	public DocumentService(
			FileValidationService fileValidationService,
			S3StorageService s3StorageService,
			S3Properties s3Properties,
			DocumentRepository documentRepository,
			DocumentChunkRepository documentChunkRepository,
			DocumentIngestionService documentIngestionService
	) {
		this.fileValidationService = fileValidationService;
		this.s3StorageService = s3StorageService;
		this.s3Properties = s3Properties;
		this.documentRepository = documentRepository;
		this.documentChunkRepository = documentChunkRepository;
		this.documentIngestionService = documentIngestionService;
	}

	@Transactional(rollbackFor = Exception.class)
	public DocumentUploadResponse upload(Long userId, MultipartFile file, Boolean isPublic) throws IOException {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}

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
	public DocumentUploadResponse moveToTrash(Long userId, Long documentId) {
		var doc = findOwnedDocument(userId, documentId);
		if (!Boolean.TRUE.equals(doc.getIsDeleted())) {
			doc.setIsDeleted(Boolean.TRUE);
			doc.setDeletedAt(Instant.now());
			doc = documentRepository.save(doc);
		}
		return toResponse(doc);
	}

	@Transactional(readOnly = true)
	public List<DocumentUploadResponse> getTrash(Long userId) {
		validateUserId(userId);
		return documentRepository.findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(userId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional
	public DocumentUploadResponse restoreFromTrash(Long userId, Long documentId) {
		var doc = findOwnedDocument(userId, documentId);
		if (Boolean.TRUE.equals(doc.getIsDeleted())) {
			doc.setIsDeleted(Boolean.FALSE);
			doc.setDeletedAt(null);
			doc = documentRepository.save(doc);
		}
		return toResponse(doc);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deletePermanently(Long userId, Long documentId) {
		var doc = findOwnedDocument(userId, documentId);
		if (!Boolean.TRUE.equals(doc.getIsDeleted())) {
			throw new IllegalArgumentException("Document must be in trash before permanent delete");
		}

		if (doc.getS3Key() != null && !doc.getS3Key().isBlank()) {
			s3StorageService.delete(doc.getS3Key());
		}
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

	private void validateUserId(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
	}

	private DocumentUploadResponse toResponse(Document doc) {
		var res = new DocumentUploadResponse();
		res.setDocumentId(doc.getDocumentId());
		res.setUserId(doc.getUserId());
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
