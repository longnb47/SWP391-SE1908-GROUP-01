package com.se1908.group01.service;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over private object storage used for original uploaded files.
 */
public interface S3StorageService {

	/** Upload the original bytes without exposing the bucket object publicly. */
	void uploadPrivate(MultipartFile file, String objectKey) throws IOException;

	String createPresignedGetUrl(String objectKey, String fileName, String contentType, boolean download);

	void delete(String objectKey);
}
