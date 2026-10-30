package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.model.CustomerOrder;
import com.safayet.foodmobochain.model.Payment;
import com.safayet.foodmobochain.model.User;
import com.safayet.foodmobochain.model.enums.NotificationType;
import com.safayet.foodmobochain.model.enums.OrderStatus;
import com.safayet.foodmobochain.model.enums.PaymentMethod;
import com.safayet.foodmobochain.model.enums.PaymentStatus;
import com.safayet.foodmobochain.repository.OrderRepository;
import com.safayet.foodmobochain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional
    public Payment createForOrder(CustomerOrder order, PaymentMethod method) {
        String transactionId = "FMC-" + order.getOrderNumber() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return paymentRepository.save(Payment.builder()
                .order(order)
                .method(method)
                .status(PaymentStatus.PENDING)
                .gateway(method == PaymentMethod.CASH_ON_DELIVERY ? "COD" : "DEMO")
                .transactionId(transactionId)
                .amount(order.getTotal())
                .build());
    }

    public Payment paymentFor(CustomerOrder order) {
        return paymentRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalArgumentException("Payment record was not found."));
    }

    public List<Payment> recentPayments() {
        return paymentRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public String paymentModeLabel() {
        return "Demo payment";
    }

    @Transactional
    public CustomerOrder completeDemoPayment(User buyer, String orderNumber) {
        CustomerOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new SecurityException("You cannot pay for this order.");
        }

        Payment payment = paymentFor(order);
        if (payment.getMethod() != PaymentMethod.SSLCOMMERZ) {
            throw new IllegalArgumentException("This order does not require online payment.");
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            return order;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("This payment can no longer be completed.");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setGateway("DEMO");
        payment.setBankTransactionId("DEMO-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }

        notificationService.send(order.getBuyer(), NotificationType.PAYMENT,
                "Payment successful",
                "Demo payment for order " + order.getOrderNumber() + " was confirmed.",
                "/orders/" + order.getOrderNumber());
        notificationService.send(order.getFoodCart().getOwner(), NotificationType.ORDER,
                "New paid order",
                "Order " + order.getOrderNumber() + " is ready for processing.",
                "/seller/orders/" + order.getOrderNumber());

        return order;
    }

    @Transactional
    public void requestRefundIfNeeded(CustomerOrder order) {
        Payment payment = paymentFor(order);
        if (payment.getStatus() == PaymentStatus.PAID) {
            payment.setStatus(PaymentStatus.REFUND_REQUIRED);
        } else if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }
        paymentRepository.save(payment);
    }

    @Transactional
    public void markCashOnDeliveryCollected(CustomerOrder order) {
        Payment payment = paymentFor(order);
        if (payment.getMethod() != PaymentMethod.CASH_ON_DELIVERY || payment.getStatus() == PaymentStatus.PAID) {
            return;
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        notificationService.send(order.getBuyer(), NotificationType.PAYMENT,
                "Cash payment recorded",
                "Cash on delivery for order " + order.getOrderNumber() + " was received.",
                "/orders/" + order.getOrderNumber());
    }
}
