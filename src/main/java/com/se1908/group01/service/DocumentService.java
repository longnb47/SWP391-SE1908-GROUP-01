package com.se1908.group01.service;

import com.se1908.group01.dto.DocumentUploadResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

	DocumentUploadResponse upload(Long userId, MultipartFile file, Boolean isPublic) throws IOException;

	DocumentUploadResponse moveToTrash(Long userId, Long documentId);

	List<DocumentUploadResponse> getMyDocuments(Long userId);

	DocumentUploadResponse getDocumentDetail(Long userId, Long documentId);

	List<DocumentUploadResponse> getPublicDocuments();

	DocumentUploadResponse getPublicDocumentDetail(Long documentId);

	DocumentUploadResponse updateVisibility(Long userId, Long documentId, Boolean isPublic);

	List<DocumentUploadResponse> getTrash(Long userId);

	DocumentUploadResponse restoreFromTrash(Long userId, Long documentId);

	void deletePermanently(Long userId, Long documentId);
}
