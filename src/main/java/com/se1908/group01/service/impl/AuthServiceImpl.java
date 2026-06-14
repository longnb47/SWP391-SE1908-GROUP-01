package com.se1908.group01.service.impl;

import com.se1908.group01.dto.*;
import com.se1908.group01.entity.OtpVerification;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.AuthProvider;
import com.se1908.group01.enums.Role;
import com.se1908.group01.enums.VerificationType;
import com.se1908.group01.repository.OtpVerificationRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.security.JwtUtil;
import com.se1908.group01.service.AuthService;
import com.se1908.group01.service.EmailService;
import com.se1908.group01.service.OtpService;
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
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .status(AccountStatus.PENDING)
                .verifiedStatus(false)
                .build();

        User savedUser = userRepository.save(user);

        String otpCode = otpService.generateOtp();

        OtpVerification otpVerification = OtpVerification.builder()
                .userId(savedUser.getUserId())
                .otpCode(otpCode)
                .verificationType(VerificationType.REGISTER)
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

    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        return otpService.verifyOtp(request);
    }

    @Override
    public GoogleLoginResponse loginWithGoogle(String email, String fullName) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user != null) {

            if (!AuthProvider.GOOGLE.equals(user.getProvider())) {
                throw new IllegalArgumentException(
                        "Email already registered with another provider"
                );
            }

        } else {

            user = User.builder()
                    .fullName(fullName)
                    .email(email)
                    .passwordHash(null)
                    .provider(AuthProvider.GOOGLE)
                    .role(Role.USER)
                    .status(AccountStatus.ACTIVE)
                    .verifiedStatus(true)
                    .build();

            user = userRepository.save(user);
        }

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
        return new GoogleLoginResponse(
                "Google login successfully",
                token,
                user.getUserId(),
                user.getEmail(),
                user.getRole(),
                user.getFullName()
        );
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!AccountStatus.ACTIVE.equals(user.getStatus())) {
            throw new IllegalArgumentException("Account is not verified. Please complete OTP verification.");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name());

        return new LoginResponse(token, user.getUserId(), user.getEmail(), user.getRole().name());
    }
}
