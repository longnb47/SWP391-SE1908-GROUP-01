package com.annct.swp.aistudyhubsystem.controller;

import com.annct.swp.aistudyhubsystem.dto.LoginDto;
import com.annct.swp.aistudyhubsystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginDto.Response> login(
            @Valid @RequestBody LoginDto.Request request) {

        return ResponseEntity.ok(authService.login(request));
    }
}
