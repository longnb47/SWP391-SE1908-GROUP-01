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
import com.se1908.group01.dto.SubscriptionResponse;
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

    // Repository thao tác với User
    private final UserRepository userRepository;

    // Repository thao tác với Payment
    private final PaymentRepository paymentRepository;

    // Repository thao tác với Subscription Plan
    private final SubscriptionPlanRepository planRepository;

    // Repository thao tác với Subscription
    private final SubscriptionRepository subscriptionRepository;

    // Đọc các thông số cấu hình VNPay
    private final VNPayConfig vnPayConfig;

    /**
     * Xử lý tạo giao dịch thanh toán
     */
    public String purchase(
            String email,
            PurchaseRequest request) {

        // Lấy thông tin người dùng theo email đăng nhập
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        // Kiểm tra gói đăng ký tồn tại
        SubscriptionPlan plan = planRepository
                .findById(request.getPlanId())
                .orElseThrow(() ->
                        new RuntimeException("Subscription plan not found"));

        // Không cho phép mua gói đã bị xóa
        if (!plan.isActive()) {
            throw new RuntimeException(
                    "Subscription plan is no longer available.");
        }

        // Kiểm tra phương thức thanh toán hợp lệ
        PaymentMethod paymentMethod;

        try {
            paymentMethod = PaymentMethod.valueOf(
                    request.getPaymentMethod().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid payment method.");
        }

        // Tạo bản ghi Payment với trạng thái PENDING
        Payment payment = Payment.builder()
                .user(user)
                .plan(plan)
                .amount(BigDecimal.valueOf(plan.getPrice()))
                .status(PaymentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        payment = paymentRepository.save(payment);

        // Trả về URL thanh toán tương ứng
        return createVNPayUrl(payment);
    }
        /**
         * Xử lý callback trả về từ VNPay
         */
        public void handleVNPayCallback (
                String transactionNo,
                String responseCode){

            Payment payment = paymentRepository
                    .findByTransactionNo(transactionNo)
                    .orElseThrow(() ->
                            new RuntimeException("Payment not found"));

            // Luôn lưu response code của VNPay
            payment.setResponseCode(responseCode);

            // Callback gửi lại nhiều lần thì bỏ qua
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                paymentRepository.save(payment);
                return;
            }

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

        /**
         * Lấy lịch sử thanh toán của người dùng
         */
        public List<PaymentHistoryResponse> getMyPaymentHistory (
                String email){

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new RuntimeException("User not found"));

            // Mapping Entity -> DTO
            return paymentRepository.findByUser(user)
                    .stream()
                    .map(payment ->
                            PaymentHistoryResponse.builder()
                                    .paymentId(payment.getId())
                                    .planName(payment.getPlan().getName())
                                    .amount(payment.getAmount())
                                    .paymentMethod(payment.getPaymentMethod())
                                    .status(payment.getStatus())
                                    .paidAt(payment.getPaidAt())
                                    .build()
                    )
                    .toList();
        }

        /**
         * Thống kê doanh thu
         */
        public RevenueResponse getRevenue () {

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

    /**
     * Lấy gói Subscription đang sử dụng
     */
    public SubscriptionResponse getMySubscription(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElseThrow(() ->
                        new RuntimeException("No active subscription"));

        SubscriptionPlan plan = subscription.getPlan();

        return SubscriptionResponse.builder()

                // ===== Subscription =====
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())

                // ===== Plan =====
                .planName(plan.getName())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())

                // ===== Storage =====
                .storageLimitGb(plan.getStorageLimitGb())
                .allowedFormats(plan.getAllowedFormats())
                .maxUploadSizeMb(plan.getMaxUploadSizeMb())

                // ===== Features =====
                .multipleDocuments(plan.getMultipleDocuments())
                .videoUpload(plan.getVideoUpload())
                .monthlyTokenLimit(plan.getMonthlyTokenLimit())

                .build();
    }

        /**
         * Tạo Subscription sau khi thanh toán thành công
         * Nếu user đã có gói ACTIVE thì chuyển sang EXPIRED
         * rồi tạo gói ACTIVE mới.
         */
        private void createSubscription (
                Payment payment){

            Subscription activeSubscription =
                    subscriptionRepository
                            .findByUserAndStatus(
                                    payment.getUser(),
                                    SubscriptionStatus.ACTIVE
                            )
                            .orElse(null);

            // Nếu đã có gói ACTIVE thì kết thúc gói cũ
            if (activeSubscription != null) {

                activeSubscription.setStatus(
                        SubscriptionStatus.EXPIRED);

                activeSubscription.setEndDate(
                        LocalDate.now());

                subscriptionRepository.save(
                        activeSubscription);
            }

            // Tạo gói mới
            Subscription newSubscription =
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

            subscriptionRepository.save(newSubscription);
        }

        /**
         * Sinh URL thanh toán VNPay
         */
        private String createVNPayUrl (
                Payment payment){

            // Tạo các tham số gửi sang VNPay
            Map<String, String> params =
                    new HashMap<>();

            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

            // VNPay yêu cầu amount nhân 100
            long amount = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            params.put("vnp_Amount",
                    String.valueOf(amount));

            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef",
                    payment.getTransactionNo());

            params.put(
                    "vnp_OrderInfo",
                    "Purchase " + payment.getPlan().getName());

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

            // Sinh chuỗi query
            String query =
                    VNPayUtil.buildQuery(params);

            // Tạo Secure Hash để xác thực
            String secureHash =
                    VNPayUtil.hmacSHA512(
                            vnPayConfig.getHashSecret(),
                            query
                    );

            // Trả về URL thanh toán
            return vnPayConfig.getPayUrl()
                    + "?"
                    + query
                    + "&vnp_SecureHash="
                    + secureHash;
        }

    }