package com.se1908.group01.service;

import com.se1908.group01.entity.Document;
import java.util.List;
import org.jspecify.annotations.Nullable;

public interface DocumentAccessService {

	Document getReadyDocumentForChat(Long userId, Long documentId);

	List<Document> getReadyDocumentsForChat(Long userId, List<Long> documentIds);

	List<Document> getAllReadyDocumentsForUser(
			Long userId,
			@Nullable Long folderId,
			boolean includePublicDocuments
	);
}
