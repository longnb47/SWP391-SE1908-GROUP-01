package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.UpdateUserProfileRequest;
import com.se1908.group01.dto.UserProfileResponse;
import com.se1908.group01.service.UserProfileService;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

	private final UserProfileService userProfileService;

	@GetMapping
	public ApiResponse<UserProfileResponse> getMyProfile() {
		var response = userProfileService.getMyProfile();
		return ApiResponse.success("Get profile successfully", response);
	}

	@PatchMapping
	public ApiResponse<UserProfileResponse> updateMyProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
		var response = userProfileService.updateMyProfile(request);
		return ApiResponse.success("Update profile successfully", response);
	}

	@PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<UserProfileResponse> updateAvatar(@RequestParam("file") MultipartFile file) {
		try {
			var response = userProfileService.updateAvatar(file);
			return ApiResponse.success("Update avatar successfully", response);
		} catch (S3Exception ex) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "S3 upload failed", ex);
		} catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "File read failed", ex);
		}
	}
}
