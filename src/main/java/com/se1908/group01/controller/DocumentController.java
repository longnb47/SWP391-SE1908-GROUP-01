package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.dto.FileAccessUrlResponse;
import com.se1908.group01.service.DocumentService;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
	public ApiResponse<DocumentUploadResponse> upload(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "isPublic", required = false) Boolean isPublic
	) {
		try {
			var response = documentService.upload(file, isPublic);
			return ApiResponse.success("Upload document successfully", response);
		} catch (S3Exception ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 upload failed", ex);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File read failed", ex);
		}
	}

	@GetMapping("/my")
	public ApiResponse<List<DocumentUploadResponse>> getMyDocuments() {
		var response = documentService.getMyDocuments();
		return ApiResponse.success("Get my documents successfully", response);
	}

	@GetMapping("/{documentId}")
	public ApiResponse<DocumentUploadResponse> getDocumentDetail(@PathVariable Long documentId) {
		var response = documentService.getDocumentDetail(documentId);
		return ApiResponse.success("Get document detail successfully", response);
	}

	@GetMapping("/{documentId}/preview-url")
	public ApiResponse<FileAccessUrlResponse> getPreviewUrl(@PathVariable Long documentId) {
		var response = documentService.getPreviewUrl(documentId);
		return ApiResponse.success("Get document preview URL successfully", response);
	}

	@GetMapping("/{documentId}/download-url")
	public ApiResponse<FileAccessUrlResponse> getDownloadUrl(@PathVariable Long documentId) {
		var response = documentService.getDownloadUrl(documentId);
		return ApiResponse.success("Get document download URL successfully", response);
	}

	@GetMapping("/public")
	public ApiResponse<List<DocumentUploadResponse>> getPublicDocuments() {
		var response = documentService.getPublicDocuments();
		return ApiResponse.success("Get public documents successfully", response);
	}

	@GetMapping("/public/{documentId}")
	public ApiResponse<DocumentUploadResponse> getPublicDocumentDetail(@PathVariable Long documentId) {
		var response = documentService.getPublicDocumentDetail(documentId);
		return ApiResponse.success("Get public document detail successfully", response);
	}

	@GetMapping("/public/{documentId}/preview-url")
	public ApiResponse<FileAccessUrlResponse> getPublicPreviewUrl(@PathVariable Long documentId) {
		var response = documentService.getPublicPreviewUrl(documentId);
		return ApiResponse.success("Get public document preview URL successfully", response);
	}

	@GetMapping("/public/{documentId}/download-url")
	public ApiResponse<FileAccessUrlResponse> getPublicDownloadUrl(@PathVariable Long documentId) {
		var response = documentService.getPublicDownloadUrl(documentId);
		return ApiResponse.success("Get public document download URL successfully", response);
	}

	@PatchMapping("/{documentId}/visibility")
	public ApiResponse<DocumentUploadResponse> updateVisibility(
			@PathVariable Long documentId,
			@RequestParam("isPublic") Boolean isPublic
	) {
		var response = documentService.updateVisibility(documentId, isPublic);
		return ApiResponse.success("Update document visibility successfully", response);
	}

	@DeleteMapping("/{documentId}")
	public ApiResponse<DocumentUploadResponse> moveToTrash(@PathVariable Long documentId) {
		var response = documentService.moveToTrash(documentId);
		return ApiResponse.success("Move document to trash successfully", response);
	}

	@GetMapping("/trash")
	public ApiResponse<List<DocumentUploadResponse>> getTrash() {
		var response = documentService.getTrash();
		return ApiResponse.success("Get trash documents successfully", response);
	}

	@PostMapping("/{documentId}/restore")
	public ApiResponse<DocumentUploadResponse> restoreFromTrash(@PathVariable Long documentId) {
		var response = documentService.restoreFromTrash(documentId);
		return ApiResponse.success("Restore document successfully", response);
	}

	@DeleteMapping("/{documentId}/permanent")
	public ApiResponse<Void> deletePermanently(@PathVariable Long documentId) {
		try {
			documentService.deletePermanently(documentId);
			return ApiResponse.success("Delete document permanently successfully", null);
		} catch (S3Exception ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 delete failed", ex);
		}
	}
}
