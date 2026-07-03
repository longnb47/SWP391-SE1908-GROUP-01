package com.se1908.group01.service;

import com.se1908.group01.config.VNPayConfig;
import com.se1908.group01.dto.PaymentHistoryResponse;
import com.se1908.group01.dto.PaymentPurchaseResponse;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.dto.RevenueResponse;
import com.se1908.group01.dto.SubscriptionResponse;
import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.enums.SubscriptionStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.PaymentRepository;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.SubscriptionRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VNPayConfig vnPayConfig;

    public PaymentPurchaseResponse purchase(
            String email,
            PurchaseRequest request) {

        validateVNPayConfiguration();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));

        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription plan not found"));

        if (!plan.isActive()) {
            throw new IllegalArgumentException(
                    "Subscription plan is no longer available");
        }

        PaymentMethod paymentMethod = parsePaymentMethod(
                request.getPaymentMethod());

        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .amount(BigDecimal.valueOf(plan.getPrice()))
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        payment = paymentRepository.save(payment);

        return PaymentPurchaseResponse.builder()
                .paymentId(payment.getId())
                .transactionNo(payment.getTransactionNo())
                .paymentUrl(createVNPayUrl(payment))
                .status(payment.getStatus())
                .build();
    }

    public void handleVNPayCallback(
            String transactionNo,
            String responseCode) {

        Payment payment = paymentRepository
                .findByTransactionNo(transactionNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found"));

        payment.setResponseCode(responseCode);

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            paymentRepository.save(payment);
            return;
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            createSubscription(payment);
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }

    public List<PaymentHistoryResponse> getMyPaymentHistory(
            String email) {

        User user = findUser(email);

        return paymentRepository.findByUser(user)
                .stream()
                .map(payment -> PaymentHistoryResponse.builder()
                        .paymentId(payment.getId())
                        .planName(payment.getPlan().getName())
                        .amount(payment.getAmount())
                        .paymentMethod(payment.getPaymentMethod())
                        .status(payment.getStatus())
                        .paidAt(payment.getPaidAt())
                        .build())
                .toList();
    }

    public RevenueResponse getRevenue() {
        return RevenueResponse.builder()
                .totalRevenue(paymentRepository.getTotalRevenue())
                .totalTransactions(
                        paymentRepository.countByStatus(
                                PaymentStatus.SUCCESS))
                .build();
    }

    public SubscriptionResponse getMySubscription(String email) {
        User user = findUser(email);

        Subscription subscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active subscription"));

        SubscriptionPlan plan = subscription.getPlan();

        return SubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .planName(plan.getName())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .storageLimitGb(plan.getStorageLimitGb())
                .allowedFormats(plan.getAllowedFormats())
                .maxUploadSizeMb(plan.getMaxUploadSizeMb())
                .multipleDocuments(plan.getMultipleDocuments())
                .videoUpload(plan.getVideoUpload())
                .monthlyTokenLimit(plan.getMonthlyTokenLimit())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"));
    }

    private PaymentMethod parsePaymentMethod(String paymentMethod) {
        try {
            return PaymentMethod.valueOf(
                    paymentMethod.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid payment method");
        }
    }

    private void createSubscription(Payment payment) {
        Subscription activeSubscription = subscriptionRepository
                .findByUserAndStatus(
                        payment.getUser(),
                        SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (activeSubscription != null) {
            activeSubscription.setStatus(SubscriptionStatus.EXPIRED);
            activeSubscription.setEndDate(LocalDate.now());
            subscriptionRepository.save(activeSubscription);
        }

        Subscription newSubscription = Subscription.builder()
                .user(payment.getUser())
                .plan(payment.getPlan())
                .startDate(LocalDate.now())
                .endDate(
                        LocalDate.now().plusDays(
                                payment.getPlan().getDurationDays()))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        subscriptionRepository.save(newSubscription);
    }

    private String createVNPayUrl(Payment payment) {
        Map<String, String> params = new HashMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        long amount = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", payment.getTransactionNo());
        params.put(
                "vnp_OrderInfo",
                "Purchase " + payment.getPlan().getName());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put(
                "vnp_CreateDate",
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        String query = VNPayUtil.buildQuery(params);
        String secureHash = VNPayUtil.hmacSHA512(
                vnPayConfig.getHashSecret(),
                query);

        return vnPayConfig.getPayUrl()
                + "?"
                + query
                + "&vnp_SecureHash="
                + secureHash;
    }

    private void validateVNPayConfiguration() {
        if (!StringUtils.hasText(vnPayConfig.getTmnCode())
                || !StringUtils.hasText(vnPayConfig.getHashSecret())
                || !StringUtils.hasText(vnPayConfig.getPayUrl())
                || !StringUtils.hasText(vnPayConfig.getReturnUrl())) {
            throw new IllegalStateException(
                    "VNPay configuration is incomplete");
        }
    }
}
