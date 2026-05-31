package com.se1908.group01.service;

import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

	private static final long MAX_BYTES = 20L * 1024L * 1024L;

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
			"pdf", "doc", "docx", "pptx", "xls", "xlsx", "png"
	);

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
		if (!StringUtils.hasText(ext) || !ALLOWED_EXTENSIONS.contains(ext)) {
			throw new IllegalArgumentException("Unsupported file extension: " + ext);
		}
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

