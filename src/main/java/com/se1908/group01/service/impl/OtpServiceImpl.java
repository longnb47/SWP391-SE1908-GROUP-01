package com.se1908.group01.service.impl;

import com.se1908.group01.dto.VerifyOtpRequest;
import com.se1908.group01.dto.VerifyOtpResponse;
import com.se1908.group01.entity.OtpVerification;
import com.se1908.group01.entity.User;
import com.se1908.group01.repository.OtpVerificationRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;

    public String generateOtp() {
        int otp = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }



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
