package com.se1908.group01.service.impl;

import com.se1908.group01.dto.request.VerifyOtpRequest;
import com.se1908.group01.dto.response.RegisterResponse;
import com.se1908.group01.dto.request.RegisterRequest;
import com.se1908.group01.dto.response.VerifyOtpResponse;
import com.se1908.group01.entity.OtpVerification;
import com.se1908.group01.entity.User;
import com.se1908.group01.repository.OtpVerificationRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.AuthService;
import com.se1908.group01.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .role("USER")
                .status("PENDING")
                .verifiedStatus(false)
                .build();

        User savedUser = userRepository.save(user);

        String otpCode = genarateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .userId(savedUser.getUserId())
                .otpCode(otpCode)
                .verificationType("REGISTER")
                .attempts(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpVerificationRepository.save(otpVerification);

        emailService.sendOtpEmail(savedUser.getEmail(), otpCode);

        return new RegisterResponse(
                "Register successfully. OTP has been sent to your email.",
                savedUser.getEmail()
        );

    }


    private String genarateOtp() {
        int otp = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }


    @Override
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        OtpVerification otpVerification = otpVerificationRepository
                .findTopByUserIdAndVerificationTypeOrderByCreatedAtDesc(
                        user.getUserId(),
                        "REGISTER"
                )
                .orElseThrow(() -> new RuntimeException("OTP not found"));

        if (otpVerification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expried");
        }

        if (!otpVerification.getOtpCode().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP!");
        }

        user.setStatus("ACTIVE");
        user.setVerifiedStatus(true);

        userRepository.save(user);

        return new VerifyOtpResponse("OTP verified successfully. Account actived.");
    }
}
