package com.se1908.group01.service;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface S3StorageService {

	void uploadPrivate(MultipartFile file, String objectKey) throws IOException;

	String createPresignedGetUrl(String objectKey, String fileName, String contentType, boolean download);

	void delete(String objectKey);
}
