package com.se1908.group01.service;

import com.se1908.group01.dto.DocumentUploadResponse;
import com.se1908.group01.dto.DocumentShareLinkResponse;
import com.se1908.group01.dto.DocumentShareResponse;
import com.se1908.group01.dto.FileAccessUrlResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

	DocumentUploadResponse upload(MultipartFile file, Boolean isPublic) throws IOException;

	DocumentUploadResponse moveToTrash(Long documentId);

	List<DocumentUploadResponse> getMyDocuments();

	List<DocumentUploadResponse> getStarredDocuments();

	DocumentUploadResponse getDocumentDetail(Long documentId);

	DocumentUploadResponse renameDocument(Long documentId, String originalFileName);

	DocumentUploadResponse moveDocumentToFolder(Long documentId, Long folderId);

	FileAccessUrlResponse getPreviewUrl(Long documentId);

	FileAccessUrlResponse getDownloadUrl(Long documentId);

	List<DocumentUploadResponse> getPublicDocuments();

	DocumentUploadResponse getPublicDocumentDetail(Long documentId);

	FileAccessUrlResponse getPublicPreviewUrl(Long documentId);

	FileAccessUrlResponse getPublicDownloadUrl(Long documentId);

	DocumentShareLinkResponse createShareLink(Long documentId);

	DocumentShareLinkResponse disableShareLink(Long documentId);

	DocumentUploadResponse getDocumentByShareLink(String token);

	FileAccessUrlResponse getShareLinkPreviewUrl(String token);

	FileAccessUrlResponse getShareLinkDownloadUrl(String token);

	DocumentShareResponse saveShareLinkToSharedWithMe(String token);

	DocumentShareResponse shareDocumentWithUser(Long documentId, String email);

	void removeUserShare(Long documentId, Long userId);

	List<DocumentUploadResponse> getSharedWithMeDocuments();

	DocumentUploadResponse getSharedWithMeDocumentDetail(Long documentId);

	FileAccessUrlResponse getSharedWithMePreviewUrl(Long documentId);

	FileAccessUrlResponse getSharedWithMeDownloadUrl(Long documentId);

	DocumentUploadResponse updateVisibility(Long documentId, Boolean isPublic);

	DocumentUploadResponse updateStarred(Long documentId, Boolean isStarred);

	List<DocumentUploadResponse> getTrash();

	DocumentUploadResponse restoreFromTrash(Long documentId);

	void deletePermanently(Long documentId);
}
