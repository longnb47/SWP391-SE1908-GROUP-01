package com.se1908.group01.service;

import com.se1908.group01.dto.DocumentFolderRequest;
import com.se1908.group01.dto.DocumentFolderResponse;
import com.se1908.group01.dto.DocumentUploadResponse;
import java.util.List;

public interface DocumentFolderService {

	DocumentFolderResponse createFolder(DocumentFolderRequest request);

	List<DocumentFolderResponse> getMyFolders();

	DocumentFolderResponse updateFolder(Long folderId, DocumentFolderRequest request);

	void deleteFolder(Long folderId);

	List<DocumentUploadResponse> getFolderDocuments(Long folderId);
}
