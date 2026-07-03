package com.se1908.group01.controller;

import com.se1908.group01.dto.ApiResponse;
import com.se1908.group01.dto.PaymentCallbackResponse;
import com.se1908.group01.dto.PaymentHistoryResponse;
import com.se1908.group01.dto.PaymentPurchaseResponse;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.dto.RevenueResponse;
import com.se1908.group01.dto.SubscriptionResponse;
import com.se1908.group01.service.PaymentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/purchase")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PaymentPurchaseResponse> purchase(
            Authentication authentication,
            @Valid @RequestBody PurchaseRequest request) {
        return ApiResponse.success(
                "Create payment successfully",
                paymentService.purchase(
                        authentication.getName(),
                        request
                )
        );
    }

    @GetMapping("/vnpay-return")
    public ApiResponse<PaymentCallbackResponse> vnPayReturn(
            @RequestParam Map<String, String> params) {
        return ApiResponse.success(
                "VNPay callback processed successfully",
                paymentService.handleVNPayCallback(params)
        );
    }

    @GetMapping("/history")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<PaymentHistoryResponse>> history(
            Authentication authentication) {
        return ApiResponse.success(
                "Get payment history successfully",
                paymentService.getMyPaymentHistory(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/revenue")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<RevenueResponse> revenue() {
        return ApiResponse.success(
                "Get payment revenue successfully",
                paymentService.getRevenue()
        );
    }

    @GetMapping("/my-subscription")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<SubscriptionResponse> mySubscription(
            Authentication authentication) {
        return ApiResponse.success(
                "Get my subscription successfully",
                paymentService.getMySubscription(
                        authentication.getName()
                )
        );
    }
}
