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
            String email,
            PurchaseRequest request) {

        // Tìm user từ email trong JWT
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Tìm gói đăng ký
        SubscriptionPlan plan = planRepository
                .findById(request.getPlanId())
                .orElseThrow(() ->
                        new RuntimeException("Subscription plan not found"));

        // Kiểm tra gói còn hoạt động
        if (!plan.isActive()) {
            throw new RuntimeException(
                    "Subscription plan is no longer available.");
        }

        // Chuyển String -> Enum
        PaymentMethod paymentMethod;

        try {
            paymentMethod = PaymentMethod.valueOf(
                    request.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid payment method.");
        }

        // Tạo payment
        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        payment = paymentRepository.save(payment);

        // Chuyển sang cổng thanh toán tương ứng
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

    private String createVNPayUrl(
            Payment payment) {

        // Sau này thay bằng URL VNPay thật
        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?paymentId="
                + payment.getId();
    }

    private String createMomoUrl(
            Payment payment) {

        // Sau này thay bằng URL MoMo thật
        return "https://test-payment.momo.vn/?paymentId="
                + payment.getId();
    }
}