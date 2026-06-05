package com.annct.swp.aistudyhubsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class LoginDto {

    @Data
    public static class Request {
        @NotBlank(message = "Email í required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password í required")
        private String password;
    }

    @Getter
    @Builder
    public static class Response {
        private String accessToken;
        private String tokenType = "Bearer";
        private String email;
        private String fullName;
        private String role;
        private String subscriptionTier;
    }
}
