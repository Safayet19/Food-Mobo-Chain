package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.model.CustomerOrder;
import com.safayet.foodmobochain.model.Payment;
import com.safayet.foodmobochain.model.User;
import com.safayet.foodmobochain.model.enums.OrderStatus;
import com.safayet.foodmobochain.model.enums.PaymentMethod;
import com.safayet.foodmobochain.model.enums.PaymentStatus;
import com.safayet.foodmobochain.repository.OrderRepository;
import com.safayet.foodmobochain.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceStateTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void cancelledOnlineOrderCannotBePaidAfterCancellation() {
        User buyer = User.builder().id(1L).build();
        CustomerOrder order = CustomerOrder.builder()
                .id(10L)
                .orderNumber("FMC-TEST")
                .buyer(buyer)
                .status(OrderStatus.CANCELLED)
                .build();
        Payment payment = Payment.builder()
                .id(20L)
                .order(order)
                .method(PaymentMethod.SSLCOMMERZ)
                .status(PaymentStatus.CANCELLED)
                .build();

        when(orderRepository.findByOrderNumber("FMC-TEST")).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder(order)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.completeDemoPayment(buyer, "FMC-TEST"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("can no longer be completed");
    }
}
