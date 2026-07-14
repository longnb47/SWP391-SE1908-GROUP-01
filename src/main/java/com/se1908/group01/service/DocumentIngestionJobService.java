package com.se1908.group01.service;

import java.io.IOException;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for preserving upload bytes and dispatching asynchronous ingestion.
 */
public interface DocumentIngestionJobService {

	/** Copy request-scoped multipart bytes to a file that survives until async ingestion runs. */
	Path copyToTempFile(MultipartFile file) throws IOException;

	/** Parse and index the committed document in a worker thread. */
	void ingestAsync(Long documentId, Path filePath, String originalFilename, String contentType);
}
