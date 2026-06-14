package com.se1908.group01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

	/**
	 * S3 bucket name. Usually provided via env var AWS_S3_BUCKET_NAME.
	 */
	private String bucketName;

	/**
	 * Optional object key prefix, e.g. "documents/".
	 */
	private String keyPrefix = "";

	/**
	 * Optional endpoint override (LocalStack/MinIO), e.g. http://localhost:4566
	 */
	private String endpoint;

	/**
	 * Pre-signed URL lifetime in minutes.
	 */
	private long presignedUrlExpirationMinutes = 10;

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix == null ? "" : keyPrefix.trim();
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public long getPresignedUrlExpirationMinutes() {
		return presignedUrlExpirationMinutes;
	}

	public void setPresignedUrlExpirationMinutes(long presignedUrlExpirationMinutes) {
		this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
	}
}
