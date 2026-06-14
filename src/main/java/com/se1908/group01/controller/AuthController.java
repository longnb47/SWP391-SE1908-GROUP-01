package com.se1908.group01.controller;

import com.se1908.group01.dto.*;
import com.se1908.group01.service.AuthService;
import com.se1908.group01.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
          @Valid @RequestBody RegisterRequest request) {

        RegisterResponse response = authService.register(request);

        return ResponseEntity.ok(
                ApiResponse.success("Register successfully", response)
        );
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
          @Valid  @RequestBody VerifyOtpRequest request) {

        VerifyOtpResponse response = authService.verifyOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("OTP verified successfully", response)
        );
    }

    @GetMapping("/google/success")
    public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleSuccess(OAuth2AuthenticationToken authentication) {

        String email = authentication.getPrincipal().getAttribute("email");
        String fullName = authentication.getPrincipal().getAttribute("name");

        GoogleLoginResponse response = authService.loginWithGoogle(email,fullName);
        return ResponseEntity.ok(
                ApiResponse.success("Google login successfully", response)
        );

    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successfully", response)
        );
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<ResendOtpResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        ResendOtpResponse response = otpService.resendOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("OTP resent successfully", response)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        RefreshResponse response = authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully", null)
        );
    }

}
