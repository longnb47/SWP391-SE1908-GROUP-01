package com.se1908.group01.service;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {

	void uploadPrivate(MultipartFile file, String objectKey) throws IOException;

	void delete(String objectKey);
}
