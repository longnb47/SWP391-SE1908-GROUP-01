package com.se1908.group01.service;

import com.se1908.group01.config.VNPayConfig;
import com.se1908.group01.dto.PaymentHistoryResponse;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.dto.RevenueResponse;
import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.enums.SubscriptionStatus;
import com.se1908.group01.repository.PaymentRepository;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.SubscriptionRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final VNPayConfig vnPayConfig;

    public String purchase(
            String email,
            PurchaseRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        SubscriptionPlan plan = planRepository
                .findById(request.getPlanId())
                .orElseThrow(() ->
                        new RuntimeException("Subscription plan not found"));

        if (!plan.isActive()) {
            throw new RuntimeException(
                    "Subscription plan is no longer available.");
        }

        PaymentMethod paymentMethod;

        try {
            paymentMethod = PaymentMethod.valueOf(
                    request.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid payment method.");
        }

        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .amount(BigDecimal.valueOf(plan.getPrice()))
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        payment = paymentRepository.save(payment);

        return switch (paymentMethod) {

            case VNPAY -> createVNPayUrl(payment);

            case MOMO -> createMomoUrl(payment);

            default -> throw new RuntimeException(
                    "Payment method not supported.");
        };
    }

    public void handleVNPayCallback(
            String transactionNo,
            String responseCode) {

        Payment payment = paymentRepository
                .findByTransactionNo(transactionNo)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if ("00".equals(responseCode)) {

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                return;
            }

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            paymentRepository.save(payment);

            createSubscription(payment);

        } else {

            payment.setStatus(PaymentStatus.FAILED);

            paymentRepository.save(payment);
        }
    }

    public List<PaymentHistoryResponse> getMyPaymentHistory(
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return paymentRepository.findByUser(user)
                .stream()
                .map(payment ->
                        PaymentHistoryResponse.builder()
                                .id(payment.getId())
                                .amount(
                                        BigDecimal.valueOf(payment.getAmount()
                                                .doubleValue())
                                )
                                .paymentMethod(
                                        payment.getPaymentMethod()
                                )
                                .status(
                                        payment.getStatus()
                                )
                                .createdAt(
                                        payment.getCreatedAt()
                                )
                                .paidAt(
                                        payment.getPaidAt()
                                )
                                .build()
                )
                .toList();
    }

    public RevenueResponse getRevenue() {

        return RevenueResponse.builder()
                .totalRevenue(
                        paymentRepository.getTotalRevenue()
                )
                .totalTransactions(
                        paymentRepository.countByStatus(
                                PaymentStatus.SUCCESS
                        )
                )
                .build();
    }

    public Subscription getMySubscription(
            String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return subscriptionRepository
                .findByUserAndStatus(
                        user,
                        SubscriptionStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "No active subscription"
                        ));
    }

    private void createSubscription(
            Payment payment) {

        boolean hasActiveSubscription =
                subscriptionRepository.existsByUserAndStatus(
                        payment.getUser(),
                        SubscriptionStatus.ACTIVE
                );

        if (hasActiveSubscription) {
            return;
        }

        Subscription subscription =
                Subscription.builder()
                        .user(payment.getUser())
                        .plan(payment.getPlan())
                        .startDate(LocalDate.now())
                        .endDate(
                                LocalDate.now()
                                        .plusDays(
                                                payment.getPlan()
                                                        .getDurationDays()
                                        )
                        )
                        .status(SubscriptionStatus.ACTIVE)
                        .build();

        subscriptionRepository.save(subscription);
    }

    private String createVNPayUrl(
            Payment payment) {

        Map<String, String> params =
                new HashMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

        long amount = payment.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        params.put("vnp_Amount",
                String.valueOf(amount));

        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef",
                payment.getTransactionNo());

        params.put("vnp_OrderInfo",
                "Subscription Payment");

        params.put("vnp_OrderType",
                "other");

        params.put("vnp_Locale",
                "vn");

        params.put("vnp_ReturnUrl",
                vnPayConfig.getReturnUrl());

        params.put("vnp_IpAddr",
                "127.0.0.1");

        params.put(
                "vnp_CreateDate",
                LocalDateTime.now()
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMddHHmmss"
                                )
                        )
        );

        String query =
                VNPayUtil.buildQuery(params);

        String secureHash =
                VNPayUtil.hmacSHA512(
                        vnPayConfig.getHashSecret(),
                        query
                );

        return vnPayConfig.getPayUrl()
                + "?"
                + query
                + "&vnp_SecureHash="
                + secureHash;
    }

    private String createMomoUrl(
            Payment payment) {

        return "https://test-payment.momo.vn/?txnRef="
                + payment.getTransactionNo();
    }
}