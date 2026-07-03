package com.se1908.group01.service;

import com.se1908.group01.config.VNPayConfig;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.PaymentRepository;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.util.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private SubscriptionLifecycleService subscriptionLifecycleService;

    private VNPayConfig vnPayConfig;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        vnPayConfig = new VNPayConfig();
        vnPayConfig.setTmnCode("DEMO");
        vnPayConfig.setHashSecret("demo-secret");
        vnPayConfig.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        vnPayConfig.setReturnUrl("http://localhost:8080/api/payments/vnpay-return");

        service = new PaymentService(
                userRepository,
                paymentRepository,
                planRepository,
                subscriptionLifecycleService,
                vnPayConfig
        );
    }

    @Test
    void purchaseReturnsStructuredPendingPayment() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .build();
        SubscriptionPlan plan = plan(true);
        PurchaseRequest request = purchaseRequest();

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    payment.setId(10L);
                    payment.setTransactionNo("txn-001");
                    return payment;
                });

        var response = service.purchase("user@example.com", request);

        assertEquals(10L, response.getPaymentId());
        assertEquals("txn-001", response.getTransactionNo());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertTrue(response.getPaymentUrl().contains("vnp_TxnRef=txn-001"));
    }

    @Test
    void purchaseRejectsInactivePlan() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .build();

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(planRepository.findById(1L))
                .thenReturn(Optional.of(plan(false)));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.purchase(
                        "user@example.com",
                        purchaseRequest())
        );
    }

    @Test
    void purchaseReturnsNotFoundForMissingPlan() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .build();

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(planRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.purchase(
                        "user@example.com",
                        purchaseRequest())
        );
    }

    @Test
    void purchaseRejectsFreePlan() {
        User user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .build();
        SubscriptionPlan freePlan = plan(true);
        freePlan.setName("FREE");
        freePlan.setPrice(0D);

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(planRepository.findById(1L))
                .thenReturn(Optional.of(freePlan));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.purchase(
                        "user@example.com",
                        purchaseRequest())
        );
    }

    @Test
    void purchaseRejectsIncompleteVNPayConfiguration() {
        vnPayConfig.setHashSecret("");

        assertThrows(
                IllegalStateException.class,
                () -> service.purchase(
                        "user@example.com",
                        purchaseRequest())
        );
    }

    @Test
    void callbackVerifiesSignatureAndCreatesSubscription() {
        Payment payment = payment(PaymentStatus.PENDING);
        Map<String, String> params = signedCallbackParams("9900000");

        when(paymentRepository.findByTransactionNoForUpdate("txn-001"))
                .thenReturn(Optional.of(payment));
        var response = service.handleVNPayCallback(params);

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(false, response.isAlreadyProcessed());
        verify(paymentRepository).save(payment);
        verify(subscriptionLifecycleService)
                .activatePaidSubscription(
                        payment.getUser(),
                        payment.getPlan());
    }

    @Test
    void callbackRejectsInvalidSignatureBeforeDatabaseLookup() {
        Map<String, String> params = signedCallbackParams("9900000");
        params.put("vnp_SecureHash", "invalid-signature");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.handleVNPayCallback(params)
        );

        verify(paymentRepository, never())
                .findByTransactionNoForUpdate(any());
    }

    @Test
    void callbackRejectsSignedButIncorrectAmount() {
        Payment payment = payment(PaymentStatus.PENDING);
        Map<String, String> params = signedCallbackParams("10000");

        when(paymentRepository.findByTransactionNoForUpdate("txn-001"))
                .thenReturn(Optional.of(payment));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.handleVNPayCallback(params)
        );

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(subscriptionLifecycleService, never())
                .activatePaidSubscription(any(), any());
    }

    @Test
    void callbackDoesNotCreateDuplicateSubscription() {
        Payment payment = payment(PaymentStatus.SUCCESS);
        Map<String, String> params = signedCallbackParams("9900000");

        when(paymentRepository.findByTransactionNoForUpdate("txn-001"))
                .thenReturn(Optional.of(payment));

        var response = service.handleVNPayCallback(params);

        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals(true, response.isAlreadyProcessed());
        verify(subscriptionLifecycleService, never())
                .activatePaidSubscription(any(), any());
    }

    private PurchaseRequest purchaseRequest() {
        PurchaseRequest request = new PurchaseRequest();
        request.setPlanId(1L);
        request.setPaymentMethod("VNPAY");
        return request;
    }

    private SubscriptionPlan plan(boolean active) {
        return SubscriptionPlan.builder()
                .id(1L)
                .name("PLUS")
                .price(99000D)
                .durationDays(30)
                .description("Plus plan")
                .storageLimitGb(10)
                .allowedFormats("pdf,docx")
                .maxUploadSizeMb(50)
                .multipleDocuments(true)
                .videoUpload(true)
                .monthlyTokenLimit(100000L)
                .active(active)
                .build();
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder()
                .id(10L)
                .transactionNo("txn-001")
                .amount(BigDecimal.valueOf(99000))
                .paymentMethod(PaymentMethod.VNPAY)
                .status(status)
                .user(User.builder()
                        .userId(1L)
                        .email("user@example.com")
                        .build())
                .plan(plan(true))
                .build();
    }

    private Map<String, String> signedCallbackParams(String amount) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "DEMO");
        params.put("vnp_TransactionNo", "vnp-transaction-001");
        params.put("vnp_TransactionStatus", "00");
        params.put("vnp_TxnRef", "txn-001");

        String signature = VNPayUtil.hmacSHA512(
                vnPayConfig.getHashSecret(),
                VNPayUtil.buildQuery(params)
        );
        params.put("vnp_SecureHash", signature);
        return params;
    }
}
