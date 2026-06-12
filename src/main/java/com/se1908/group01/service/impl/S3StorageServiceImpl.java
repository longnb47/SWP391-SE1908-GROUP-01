package com.se1908.group01.service.impl;

import com.se1908.group01.config.S3Properties;
import com.se1908.group01.service.S3StorageService;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.lang.Nullable;

@Service
public class S3StorageServiceImpl implements S3StorageService {

	private final S3Client s3Client;
	private final S3Properties s3Properties;

	public S3StorageServiceImpl(@Nullable S3Client s3Client, S3Properties s3Properties) {
		this.s3Client = s3Client;
		this.s3Properties = s3Properties;
	}

	@Override
	public void uploadPrivate(MultipartFile file, String objectKey) throws IOException {
		if (s3Client == null) {
			throw new IllegalStateException("S3 is not configured (missing aws.region/AWS credentials)");
		}
		if (!StringUtils.hasText(s3Properties.getBucketName())) {
			throw new IllegalStateException("Missing required config: aws.s3.bucket-name (env AWS_S3_BUCKET_NAME)");
		}
		var request = PutObjectRequest.builder()
				.bucket(s3Properties.getBucketName())
				.key(objectKey)
				.acl(ObjectCannedACL.PRIVATE)
				.contentType(file.getContentType())
				.contentLength(file.getSize())
				.build();

		try (var in = file.getInputStream()) {
			s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
		}
	}

	@Override
	public void delete(String objectKey) {
		if (s3Client == null) {
			return;
		}
		if (!StringUtils.hasText(s3Properties.getBucketName())) {
			throw new IllegalStateException("Missing required config: aws.s3.bucket-name (env AWS_S3_BUCKET_NAME)");
		}
		var request = DeleteObjectRequest.builder()
				.bucket(s3Properties.getBucketName())
				.key(objectKey)
				.build();
		s3Client.deleteObject(request);
	}
}
