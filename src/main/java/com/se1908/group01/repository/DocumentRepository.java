package com.se1908.group01.repository;

import com.se1908.group01.entity.Document;
import com.se1908.group01.entity.DocumentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	Optional<Document> findByDocumentIdAndUserId(Long documentId, Long userId);

	Optional<Document> findByDocumentIdAndUserIdAndIsDeletedFalse(Long documentId, Long userId);

	Optional<Document> findByDocumentIdAndIsPublicTrueAndIsDeletedFalse(Long documentId);

	List<Document> findByUserIdAndIsDeletedFalseOrderByUploadedAtDesc(Long userId);

	List<Document> findByUserIdAndIsStarredTrueAndIsDeletedFalseOrderByUploadedAtDesc(Long userId);

	List<Document> findByUserIdAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc(Long userId, Long folderId);

	List<Document> findByIsPublicTrueAndIsDeletedFalseOrderByUploadedAtDesc();

	List<Document> findByUserIdAndIsDeletedTrueOrderByDeletedAtDesc(Long userId);

	@Modifying
	@Query("update Document d set d.folderId = null where d.userId = :userId and d.folderId = :folderId")
	void clearFolderForUser(@Param("userId") Long userId, @Param("folderId") Long folderId);

	@Query("SELECT d FROM Document d WHERE d.documentId IN :documentIds AND d.isDeleted = false AND d.status = :status AND (d.userId = :userId OR d.isPublic = true)")
	List<Document> findAccessibleDocumentsByIdsAndStatus(
			@Param("documentIds") List<Long> documentIds,
			@Param("userId") Long userId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND (d.userId = :userId OR d.isPublic = true)")
	List<Document> findAllAccessibleDocumentsByStatus(
			@Param("userId") Long userId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND d.userId = :userId")
	List<Document> findOwnedDocumentsByStatus(
			@Param("userId") Long userId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND d.userId = :userId AND d.folderId = :folderId")
	List<Document> findOwnedDocumentsByFolderAndStatus(
			@Param("userId") Long userId,
			@Param("folderId") Long folderId,
			@Param("status") DocumentStatus status);

	@Query("SELECT d FROM Document d WHERE d.isDeleted = false AND d.status = :status AND ((d.userId = :userId AND d.folderId = :folderId) OR d.isPublic = true)")
	List<Document> findOwnedFolderAndPublicDocumentsByStatus(
			@Param("userId") Long userId,
			@Param("folderId") Long folderId,
			@Param("status") DocumentStatus status);
}
