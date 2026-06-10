package com.se1908.group01.controller;

import com.se1908.group01.dto.RegisterRequest;
import com.se1908.group01.dto.VerifyOtpRequest;
import com.se1908.group01.dto.RegisterResponse;
import com.se1908.group01.dto.VerifyOtpResponse;
import com.se1908.group01.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping ("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping ("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity.ok(response);
    }
//@GetMapping ("/test-email")
//    private String testEmail() {
//        emailService.sendOtpEmail("lequocthien3264@gmail.com", "123456");
//        return "Email send";
//    }

    @PostMapping ("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        VerifyOtpResponse response = authService.verifyOtp(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/google/success")
    public String googleSuccess() {
        return "Google Login Success";
    }




}
