package com.se1908.group01.service;

import com.se1908.group01.entity.Document;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentIngestionService {

	int ingest(Document document, MultipartFile file) throws IOException;
}
