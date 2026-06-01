package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	void deleteByDocumentDocumentId(Long documentId);
}

