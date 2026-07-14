package com.se1908.group01.service;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
/**
 * Performs server-side validation for uploaded document and video files.
 * This is the authoritative guard even when the frontend already rejected an invalid file.
 */
public class FileValidationService {

	private static final int DEFAULT_MAX_DOC_MB = 20;

	private static final Set<String> ALLOWED_DOC_EXTENSIONS = Set.of(
			"pdf", "doc", "docx", "pptx", "xls", "xlsx", "png"
	);

	private static final Set<String> VIDEO_EXTENSIONS = Set.of(
			"mp4", "mov", "avi", "webm"
	);

	private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of(
			"png", "jpg", "jpeg", "webp"
	);

	@Value("${app.upload.max-video-file-size:52428800}")
	private long maxVideoFileSize;

	@Value("${app.upload.max-avatar-file-size:5242880}")
	private long maxAvatarFileSize;

	public void validateForUpload(MultipartFile file) {
		validateForUpload(file, DEFAULT_MAX_DOC_MB);
	}

	public void validateForUpload(MultipartFile file, Integer maxNonVideoUploadSizeMb) {
		// Reject missing or empty input before reading filename, type or size metadata.
		if (file == null) {
			throw new IllegalArgumentException("File is required");
		}
		if (file.isEmpty() || file.getSize() <= 0) {
			throw new IllegalArgumentException("File is empty");
		}

		var originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			throw new IllegalArgumentException("Original filename is required");
		}

		var ext = getExtensionLower(originalFilename);
		var contentType = file.getContentType();
		var isImage = StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/");
		var isVideo = isVideoFile(ext, contentType);

		// The extension/content-type combination must belong to a supported document, image or video category.
		if (!StringUtils.hasText(ext) || (!ALLOWED_DOC_EXTENSIONS.contains(ext) && !isImage && !isVideo)) {
			throw new IllegalArgumentException("Unsupported file extension: " + ext);
		}

		if (isVideo) {
			// Videos use the application-wide byte limit; plan video permission is checked separately.
			if (file.getSize() > maxVideoFileSize) {
				throw new IllegalArgumentException("Video file exceeds " + (maxVideoFileSize / 1024 / 1024) + "MB limit");
			}
		} else {
			// Non-video files use maxUploadSizeMb from the user's active subscription plan.
			var maxNonVideoBytes = toBytes(maxNonVideoUploadSizeMb);
			if (file.getSize() > maxNonVideoBytes) {
				throw new IllegalArgumentException("File exceeds " + maxNonVideoUploadSizeMb + "MB limit");
			}
		}
	}

	public void validateForAvatarUpload(MultipartFile file) {
		if (file == null) {
			throw new IllegalArgumentException("File is required");
		}
		if (file.isEmpty() || file.getSize() <= 0) {
			throw new IllegalArgumentException("File is empty");
		}

		var originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			throw new IllegalArgumentException("Original filename is required");
		}

		var ext = getExtensionLower(originalFilename);
		var contentType = file.getContentType();
		var isImage = StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("image/");

		if (!ALLOWED_AVATAR_EXTENSIONS.contains(ext) || !isImage) {
			throw new IllegalArgumentException("Unsupported avatar file type: " + ext);
		}

		if (file.getSize() > maxAvatarFileSize) {
			throw new IllegalArgumentException("Avatar file exceeds " + (maxAvatarFileSize / 1024 / 1024) + "MB limit");
		}
	}

	public boolean isVideo(MultipartFile file) {
		if (file == null) {
			return false;
		}
		var originalFilename = file.getOriginalFilename();
		var ext = StringUtils.hasText(originalFilename) ? getExtensionLower(originalFilename) : "";
		return isVideoFile(ext, file.getContentType());
	}

	private static boolean isVideoFile(String ext, String contentType) {
		if (VIDEO_EXTENSIONS.contains(ext)) {
			return true;
		}
		return StringUtils.hasText(contentType) && contentType.toLowerCase().startsWith("video/");
	}

	private static long toBytes(Integer sizeMb) {
		if (sizeMb == null || sizeMb <= 0) {
			throw new IllegalStateException("Active subscription plan upload limit is not configured");
		}
		return sizeMb * 1024L * 1024L;
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
