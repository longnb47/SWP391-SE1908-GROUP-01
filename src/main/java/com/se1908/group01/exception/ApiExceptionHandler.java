package com.se1908.group01.exception;

import com.se1908.group01.dto.ApiError;
import com.se1908.group01.dto.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
		return error(HttpStatus.BAD_REQUEST, "Validation failed", fieldError(null, ex.getMessage()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
		return error(HttpStatus.BAD_REQUEST, "Validation failed",
				fieldError(ex.getParameterName(), "Required request parameter is missing"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
		return error(HttpStatus.PAYLOAD_TOO_LARGE, "File upload failed",
				fieldError("file", "File exceeds maximum upload size"));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiResponse<Void>> handleResponseStatus(ResponseStatusException ex) {
		var message = hasText(ex.getReason()) ? ex.getReason() : "Request failed";
		return error(ex.getStatusCode(), message, fieldError(null, message));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
		return error(HttpStatus.NOT_FOUND, "Resource not found", fieldError(null, ex.getMessage()));
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Request failed", fieldError(null, ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error",
				fieldError(null, "Unexpected server error"));
	}

	private static ResponseEntity<ApiResponse<Void>> error(HttpStatusCode status, String message, ApiError error) {
		return ResponseEntity.status(status).body(ApiResponse.error(message, List.of(error)));
	}

	private static ApiError fieldError(String field, String message) {
		return new ApiError(field, hasText(message) ? message : "Request failed");
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
