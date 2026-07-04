package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentChunk;
import com.se1908.group01.entity.DocumentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	void deleteByDocumentDocumentId(Long documentId);

	List<DocumentChunk> findByDocumentDocumentIdOrderByChunkIndexAsc(Long documentId);

	@Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.document WHERE dc.document.documentId IN :documentIds ORDER BY dc.document.documentId ASC, dc.chunkIndex ASC")
	List<DocumentChunk> findByDocumentIds(@Param("documentIds") List<Long> documentIds);

	@Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.document WHERE dc.document.isDeleted = false AND dc.document.status = :status AND (dc.document.userId = :userId OR dc.document.isPublic = true) ORDER BY dc.document.documentId ASC, dc.chunkIndex ASC")
	List<DocumentChunk> findChunksByUserAccessible(@Param("userId") Long userId, @Param("status") DocumentStatus status);

	@Query("SELECT dc FROM DocumentChunk dc JOIN FETCH dc.document WHERE dc.document.folderId = :folderId AND dc.document.isDeleted = false AND dc.document.status = :status AND (dc.document.userId = :userId OR dc.document.isPublic = true) ORDER BY dc.document.documentId ASC, dc.chunkIndex ASC")
	List<DocumentChunk> findChunksByUserAndFolderAccessible(@Param("userId") Long userId, @Param("folderId") Long folderId, @Param("status") DocumentStatus status);
}
