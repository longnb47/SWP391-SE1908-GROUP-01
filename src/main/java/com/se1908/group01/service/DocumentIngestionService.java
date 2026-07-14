package com.se1908.group01.service;

import com.se1908.group01.entity.Document;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for converting uploaded files into embedded document chunks.
 */
public interface DocumentIngestionService {

	/** Build and persist the searchable chunks for one document. */
	int ingest(Document document, MultipartFile file) throws IOException;
}
