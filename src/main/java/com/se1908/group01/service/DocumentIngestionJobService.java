package com.se1908.group01.service;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIngestionJobService {

	Path copyToTempFile(MultipartFile file) throws IOException;

	void ingestAsync(Long documentId, Path filePath, String originalFilename, String contentType);
}
