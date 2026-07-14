package com.se1908.group01.service;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction cho private object storage dùng để lưu file gốc đã upload.
 */
public interface S3StorageService {

	/** Upload bytes gốc mà không public object trong bucket. */
	void uploadPrivate(MultipartFile file, String objectKey) throws IOException;

	String createPresignedGetUrl(String objectKey, String fileName, String contentType, boolean download);

	void delete(String objectKey);
}
