package com.se1908.group01.service.impl;

import com.se1908.group01.entity.DocumentStatus;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.service.DocumentIngestionJobService;
import com.se1908.group01.service.DocumentIngestionService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentIngestionJobServiceImpl implements DocumentIngestionJobService {

	private static final Logger log = LoggerFactory.getLogger(DocumentIngestionJobServiceImpl.class);
	private final DocumentRepository documentRepository;
	private final DocumentIngestionService documentIngestionService;

	public DocumentIngestionJobServiceImpl(
			DocumentRepository documentRepository,
			DocumentIngestionService documentIngestionService
	) {
		this.documentRepository = documentRepository;
		this.documentIngestionService = documentIngestionService;
	}

	@Override
	public Path copyToTempFile(MultipartFile file) throws IOException {
		var tempFile = Files.createTempFile("document-ingestion-", ".tmp");
		try (var inputStream = file.getInputStream()) {
			Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
		}
		return tempFile;
	}

	@Async
	@Override
	public void ingestAsync(Long documentId, Path filePath, String originalFilename, String contentType) {
		try {
			var document = documentRepository.findById(documentId)
					.orElseThrow(() -> new IllegalArgumentException("Document not found"));
			var multipartFile = new PathMultipartFile(filePath, originalFilename, contentType);
			documentIngestionService.ingest(document, multipartFile);
			log.info("Document ingestion completed for documentId={}", documentId);
		} catch (Exception ex) {
			log.error("Document ingestion failed for documentId={}", documentId, ex);
			documentRepository.findById(documentId).ifPresent(document -> {
				document.setStatus(DocumentStatus.FAILED);
				documentRepository.save(document);
			});
		} finally {
			deleteTempFile(filePath);
		}
	}

	private void deleteTempFile(Path filePath) {
		if (filePath == null) {
			return;
		}
		try {
			Files.deleteIfExists(filePath);
		} catch (IOException ex) {
			log.warn("Failed to delete ingestion temp file: {}", filePath, ex);
		}
	}

	private static class PathMultipartFile implements MultipartFile {

		private final Path filePath;
		private final String originalFilename;
		private final String contentType;

		private PathMultipartFile(Path filePath, String originalFilename, String contentType) {
			this.filePath = filePath;
			this.originalFilename = originalFilename;
			this.contentType = contentType;
		}

		@Override
		public String getName() {
			return "file";
		}

		@Override
		public String getOriginalFilename() {
			return originalFilename;
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public boolean isEmpty() {
			return getSize() <= 0;
		}

		@Override
		public long getSize() {
			try {
				return Files.size(filePath);
			} catch (IOException ex) {
				return 0;
			}
		}

		@Override
		public byte[] getBytes() throws IOException {
			return Files.readAllBytes(filePath);
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return Files.newInputStream(filePath);
		}

		@Override
		public void transferTo(File dest) throws IOException, IllegalStateException {
			Files.copy(filePath, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
