package com.se1908.group01.service;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.repository.PaymentRepository;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository planRepository;

    public String purchase(
            Long userId,
            PurchaseRequest request) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow();

        SubscriptionPlan plan =
                planRepository.findById(
                                request.getPlanId())
                        .orElseThrow();

        Payment payment =
                Payment.builder()
                        .user(user)
                        .plan(plan)
                        .amount(plan.getPrice())
                        .status(
                                PaymentStatus.PENDING)
                        .paymentMethod(
                                PaymentMethod.valueOf(
                                        request.getPaymentMethod()))
                        .build();

        paymentRepository.save(payment);

        if (request.getPaymentMethod()
                .equalsIgnoreCase("VNPAY")) {

            return createVNPayUrl(payment);
        }

        return createMomoUrl(payment);
    }

    private String createVNPayUrl(
            Payment payment) {

        return "https://sandbox.vnpayment.vn/payment?...";
    }

    private String createMomoUrl(
            Payment payment) {

        return "https://test-payment.momo.vn/...";
    }
}
