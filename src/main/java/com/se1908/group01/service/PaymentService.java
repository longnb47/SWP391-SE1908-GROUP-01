package com.se1908.group01.service;

import com.se1908.group01.dto.PurchaseRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;

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
                .amount(plan.getPrice())
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        payment = paymentRepository.save(payment);

        switch (paymentMethod) {

            case VNPAY:
                return createVNPayUrl(payment);

            case MOMO:
                return createMomoUrl(payment);

            default:
                throw new RuntimeException(
                        "Payment method not supported.");
        }
    }

    public void fakeSuccess(Long paymentId) {

        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException(
                    "Payment already completed");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());

        paymentRepository.save(payment);

        createSubscription(payment);
    }

    public void handleVNPayCallback(
            String transactionNo,
            String responseCode) {

        Payment payment = paymentRepository
                .findByTransactionNo(transactionNo)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if ("00".equals(responseCode)) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());

            paymentRepository.save(payment);

            createSubscription(payment);

        } else {

            payment.setStatus(PaymentStatus.FAILED);

            paymentRepository.save(payment);
        }
    }

    private void createSubscription(
            Payment payment) {

        Subscription subscription =
                Subscription.builder()
                        .user(payment.getUser())
                        .plan(payment.getPlan())
                        .startDate(LocalDate.now())

                        // TẠM THỜI 30 NGÀY
                        // Sau này thay bằng:
                        // payment.getPlan().getDurationDays()

                        .endDate(
                                LocalDate.now()
                                        .plusDays(payment.getPlan().getDurationDays())
                        )

                        .status(SubscriptionStatus.ACTIVE)
                        .build();

        subscriptionRepository.save(subscription);
    }

    private String createVNPayUrl(
            Payment payment) {

        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?txnRef="
                + payment.getTransactionNo();
    }

    private String createMomoUrl(
            Payment payment) {

        return "https://test-payment.momo.vn/?txnRef="
                + payment.getTransactionNo();
    }
}