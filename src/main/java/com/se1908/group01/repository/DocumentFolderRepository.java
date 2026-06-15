package com.se1908.group01.repository;

import com.se1908.group01.entity.DocumentFolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

	List<DocumentFolder> findByUserIdOrderByNameAsc(Long userId);

	Optional<DocumentFolder> findByFolderIdAndUserId(Long folderId, Long userId);

	boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

	boolean existsByUserIdAndNameIgnoreCaseAndFolderIdNot(Long userId, String name, Long folderId);
}
