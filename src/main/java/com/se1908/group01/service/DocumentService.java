package com.se1908.group01.service;

import com.se1908.group01.dto.DocumentUploadResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

	DocumentUploadResponse upload(MultipartFile file, Boolean isPublic) throws IOException;

	DocumentUploadResponse moveToTrash(Long documentId);

	List<DocumentUploadResponse> getMyDocuments();

	DocumentUploadResponse getDocumentDetail(Long documentId);

	List<DocumentUploadResponse> getPublicDocuments();

	DocumentUploadResponse getPublicDocumentDetail(Long documentId);

	DocumentUploadResponse updateVisibility(Long documentId, Boolean isPublic);

	List<DocumentUploadResponse> getTrash();

	DocumentUploadResponse restoreFromTrash(Long documentId);

	void deletePermanently(Long documentId);
}
