package com.se1908.group01.service;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

	private static final long MAX_DOC_BYTES = 20L * 1024L * 1024L;

	private static final Set<String> ALLOWED_DOC_EXTENSIONS = Set.of(
			"pdf", "doc", "docx", "pptx", "xls", "xlsx", "png"
	);

	private static final Set<String> VIDEO_EXTENSIONS = Set.of(
			"mp4", "mov", "avi", "webm"
	);

	@Value("${app.upload.max-video-file-size:52428800}")
	private long maxVideoFileSize;

	public void validateForUpload(MultipartFile file) {
		if (file == null) {
			throw new IllegalArgumentException("File is required");
		}
		if (file.isEmpty() || file.getSize() <= 0) {
			throw new IllegalArgumentException("File is empty");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new IllegalArgumentException("File exceeds 20MB limit");
		}

		var originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			throw new IllegalArgumentException("Original filename is required");
		}

		var ext = getExtensionLower(originalFilename);
		var contentType = file.getContentType();
		var isImage = StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/");
		var isVideo = isVideoFile(ext, contentType);

		if (!StringUtils.hasText(ext) || (!ALLOWED_DOC_EXTENSIONS.contains(ext) && !isImage && !isVideo)) {
			throw new IllegalArgumentException("Unsupported file extension: " + ext);
		}

		if (isVideo) {
			if (file.getSize() > maxVideoFileSize) {
				throw new IllegalArgumentException("Video file exceeds " + (maxVideoFileSize / 1024 / 1024) + "MB limit");
			}
		} else {
			if (file.getSize() > MAX_DOC_BYTES) {
				throw new IllegalArgumentException("File exceeds 20MB limit");
			}
		}
	}

	private static boolean isVideoFile(String ext, String contentType) {
		if (VIDEO_EXTENSIONS.contains(ext)) {
			return true;
		}
		return StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("video/");
	}

	private static String getExtensionLower(String filename) {
		var clean = filename.replace("\\", "/");
		var lastSlash = clean.lastIndexOf('/');
		var base = lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
		var lastDot = base.lastIndexOf('.');
		if (lastDot < 0 || lastDot == base.length() - 1) {
			return "";
		}
		return base.substring(lastDot + 1).toLowerCase();
	}
}
