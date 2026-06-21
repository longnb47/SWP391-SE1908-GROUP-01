package com.se1908.group01.service;

import com.se1908.group01.entity.Document;

public interface DocumentAccessService {

	Document getReadyDocumentForChat(Long userId, Long documentId);
}
