package com.se1908.group01.controller;

import com.se1908.group01.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private final PaymentService paymentService;

    @GetMapping("/me")
    public ResponseEntity<?> mySubscription(
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getMySubscription(
                        authentication.getName()
                )
        );
    }
}