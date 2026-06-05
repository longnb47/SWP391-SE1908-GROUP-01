package com.se1908.group01.repository;

import com.se1908.group01.entity.Document;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	Optional<Document> findByDocumentIdAndUserId(Long documentId, Long userId);

	List<Document> findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(Long userId);
}
