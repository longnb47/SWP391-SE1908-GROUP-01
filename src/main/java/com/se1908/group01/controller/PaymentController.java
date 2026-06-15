package com.se1908.group01.controller;

import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @PostMapping("/purchase")
    public ResponseEntity<?> purchase(
            @RequestParam Long userId,
            @RequestBody PurchaseRequest request) {

        String paymentUrl =
                service.purchase(userId, request);

        return ResponseEntity.ok(paymentUrl);
    }
}