package com.se1908.group01.service.impl;

import com.se1908.group01.config.S3Properties;
import com.se1908.group01.dto.UpdateUserProfileRequest;
import com.se1908.group01.dto.UserProfileResponse;
import com.se1908.group01.entity.User;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.FileValidationService;
import com.se1908.group01.service.S3StorageService;
import com.se1908.group01.service.UserProfileService;
import com.se1908.group01.util.FilenameSanitizer;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

	private final UserRepository userRepository;
	private final CurrentUserService currentUserService;
	private final FileValidationService fileValidationService;
	private final S3StorageService s3StorageService;
	private final S3Properties s3Properties;

	@Override
	@Transactional(readOnly = true)
	public UserProfileResponse getMyProfile() {
		return toResponse(findCurrentUser());
	}

	@Override
	@Transactional
	public UserProfileResponse updateMyProfile(UpdateUserProfileRequest request) {
		var user = findCurrentUser();

		if (StringUtils.hasText(request.getFullName())) {
			user.setFullName(request.getFullName());
		}
		if (request.getBio() != null) {
			user.setBio(request.getBio());
		}

		return toResponse(userRepository.save(user));
	}

	@Override
	@Transactional
	public UserProfileResponse updateAvatar(MultipartFile file) throws IOException {
		fileValidationService.validateForAvatarUpload(file);

		var user = findCurrentUser();
		var oldKey = user.getAvatarS3Key();

		var sanitizedName = FilenameSanitizer.sanitize(file.getOriginalFilename());
		var newKey = buildAvatarObjectKey(user.getUserId(), sanitizedName);

		s3StorageService.uploadPrivate(file, newKey);

		try {
			user.setAvatarS3Key(newKey);
			user = userRepository.save(user);
		} catch (RuntimeException ex) {
			try {
				s3StorageService.delete(newKey);
			} catch (RuntimeException ignored) {
			}
			throw ex;
		}

		if (StringUtils.hasText(oldKey) && !oldKey.equals(newKey)) {
			try {
				s3StorageService.delete(oldKey);
			} catch (RuntimeException ignored) {
			}
		}

		return toResponse(user);
	}

	private String buildAvatarObjectKey(Long userId, String sanitizedFilename) {
		var prefix = s3Properties.getKeyPrefix();
		if (prefix == null) {
			prefix = "";
		}
		prefix = prefix.trim();
		if (!prefix.isEmpty() && !prefix.endsWith("/")) {
			prefix = prefix + "/";
		}

		var uuid = UUID.randomUUID();
		return prefix + "avatars/" + userId + "/" + uuid + "-" + sanitizedFilename;
	}

	private User findCurrentUser() {
		var userId = currentUserService.getCurrentUserId();
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private UserProfileResponse toResponse(User user) {
		return UserProfileResponse.builder()
				.userId(user.getUserId())
				.fullName(user.getFullName())
				.email(user.getEmail())
				.bio(user.getBio())
				.avatarUrl(resolveAvatarUrl(user))
				.role(user.getRole())
				.status(user.getStatus())
				.createdAt(user.getCreateAt())
				.updatedAt(user.getUpdatedAt())
				.build();
	}

	private String resolveAvatarUrl(User user) {
		if (!StringUtils.hasText(user.getAvatarS3Key())) {
			return null;
		}
		return s3StorageService.createPresignedGetUrl(user.getAvatarS3Key(), null, null, false);
	}
}
