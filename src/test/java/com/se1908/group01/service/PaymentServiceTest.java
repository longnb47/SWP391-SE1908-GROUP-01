package com.se1908.group01.service;

import com.se1908.group01.config.VNPayConfig;
import com.se1908.group01.dto.PurchaseRequest;
import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentStatus;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.PaymentRepository;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.SubscriptionRepository;
import com.se1908.group01.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private SubscriptionRepository subscriptionRepository;

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
                subscriptionRepository,
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
    void purchaseRejectsIncompleteVNPayConfiguration() {
        vnPayConfig.setHashSecret("");

        assertThrows(
                IllegalStateException.class,
                () -> service.purchase(
                        "user@example.com",
                        purchaseRequest())
        );
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
}
