package com.se1908.group01.controller;

import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.service.DocumentService;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DocumentUploadResponse upload(
			@RequestParam("userId") Long userId,
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "isPublic", required = false) Boolean isPublic
	) {
		try {
			return documentService.upload(userId, file, isPublic);
		} catch (S3Exception ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 upload failed", ex);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File read failed", ex);
		}
	}
}
