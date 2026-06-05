package com.annct.swp.aistudyhubsystem.service.impl;

import com.annct.swp.aistudyhubsystem.dto.LoginDto;
import com.annct.swp.aistudyhubsystem.entity.User;
import com.annct.swp.aistudyhubsystem.enums.AccountStatus;
import com.annct.swp.aistudyhubsystem.repository.UserRepository;
import com.annct.swp.aistudyhubsystem.security.JwtUtil;
import com.annct.swp.aistudyhubsystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginDto.Response login(LoginDto.Request request) {
        // Tìm user theo email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email not found"));

        //Kiểm tra email đã verify chưa
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Email not verified");
        }

        // Kiểm tra account status
        if(user.getStatus() == AccountStatus.LOCKED) {
            throw new RuntimeException("Account is locked");
        }

        //Verify pass
        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        //Generate JWT
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        //Return respone
        return LoginDto.Response.builder()
                .accessToken(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .subscriptionTier(user.getSubscriptionTier().name())
                .build();
    }
}
