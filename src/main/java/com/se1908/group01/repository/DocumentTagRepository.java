package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentTag;
import com.se1908.group01.entity.DocumentTagId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentTagRepository extends JpaRepository<DocumentTag, DocumentTagId> {

	List<DocumentTag> findByDocumentDocumentIdOrderByTagNameAsc(Long documentId);

	boolean existsByDocumentDocumentIdAndTagTagId(Long documentId, Long tagId);

	void deleteByDocumentDocumentIdAndTagTagId(Long documentId, Long tagId);

	void deleteByDocumentDocumentId(Long documentId);

	void deleteByTagTagIdAndTagUserId(Long tagId, Long userId);
}
