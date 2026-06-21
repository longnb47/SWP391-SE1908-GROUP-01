package com.se1908.group01.controller;

import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(
            Authentication authentication,
            @RequestBody PurchaseRequest request) {

        String email = authentication.getName();

        String paymentUrl =
                paymentService.purchase(
                        email,
                        request);

        return ResponseEntity.ok(paymentUrl);
    }
}