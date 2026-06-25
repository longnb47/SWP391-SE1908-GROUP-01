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

        return ResponseEntity.ok(
                paymentService.purchase(
                        authentication.getName(),
                        request
                )
        );
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnPayReturn(

            @RequestParam("vnp_TxnRef")
            String transactionNo,

            @RequestParam("vnp_ResponseCode")
            String responseCode) {

        paymentService.handleVNPayCallback(
                transactionNo,
                responseCode);

        return ResponseEntity.ok(
                "VNPay callback processed successfully");
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getMyPaymentHistory(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/revenue")
    public ResponseEntity<?> revenue() {

        return ResponseEntity.ok(
                paymentService.getRevenue()
        );
    }

    @GetMapping("/my-subscription")
    public ResponseEntity<?> mySubscription(
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.getMySubscription(
                        authentication.getName()
                )
        );
    }
}