package com.se1908.group01.service;

import com.se1908.group01.config.S3Properties;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.entity.Document;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.util.FilenameSanitizer;
import java.io.IOException;
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
	private final DocumentIngestionService documentIngestionService;

	public DocumentService(
			FileValidationService fileValidationService,
			S3StorageService s3StorageService,
			S3Properties s3Properties,
			DocumentRepository documentRepository,
			DocumentIngestionService documentIngestionService
	) {
		this.fileValidationService = fileValidationService;
		this.s3StorageService = s3StorageService;
		this.s3Properties = s3Properties;
		this.documentRepository = documentRepository;
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

			doc = documentRepository.save(doc);
			documentIngestionService.ingest(doc, file);
		} catch (RuntimeException ex) {
			try {
				s3StorageService.delete(key);
			} catch (RuntimeException ignored) {
				// best effort cleanup
			}
			throw ex;
		} catch (IOException ex) {
			try {
				s3StorageService.delete(key);
			} catch (RuntimeException ignored) {
				// best effort cleanup
			}
			throw ex;
		}

		var res = new DocumentUploadResponse();
		res.setDocumentId(doc.getDocumentId());
		res.setUserId(doc.getUserId());
		res.setOriginalFileName(doc.getOriginalFileName());
		res.setS3Key(doc.getS3Key());
		res.setContentType(doc.getContentType());
		res.setFileSize(doc.getFileSize());
		res.setIsPublic(doc.getIsPublic());
		res.setUploadedAt(doc.getUploadedAt());
		return res;
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
}
