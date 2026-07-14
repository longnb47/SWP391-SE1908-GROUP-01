package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentFolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Tìm folder theo owner để thao tác sau upload không thể trỏ tới folder của user khác.
 */
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

	List<DocumentFolder> findByUserIdOrderByNameAsc(Long userId);

	List<DocumentFolder> findByUserIdAndIsStarredTrueOrderByNameAsc(Long userId);

	Optional<DocumentFolder> findByFolderIdAndUserId(Long folderId, Long userId);

	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndFolderIdNot(Long userId, String name, Long folderId);
}
