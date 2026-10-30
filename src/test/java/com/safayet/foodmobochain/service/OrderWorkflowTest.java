package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.model.CustomerOrder;
import com.safayet.foodmobochain.model.Delivery;
import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.User;
import com.safayet.foodmobochain.model.enums.OrderStatus;
import com.safayet.foodmobochain.repository.DeliveryRepository;
import com.safayet.foodmobochain.repository.FoodCartRepository;
import com.safayet.foodmobochain.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowTest {

    @Mock private OrderRepository orderRepository;
    @Mock private DeliveryRepository deliveryRepository;
    @Mock private FoodCartRepository foodCartRepository;
    @Mock private CartService cartService;
    @Mock private DiscountService discountService;
    @Mock private PaymentService paymentService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void sellerWorkflowOnlyAllowsExpectedNextStates() {
        assertThat(orderService.allowedNext(OrderStatus.CONFIRMED)).containsExactly(OrderStatus.ACCEPTED);
        assertThat(orderService.allowedNext(OrderStatus.ACCEPTED)).containsExactlyInAnyOrder(OrderStatus.COOKING, OrderStatus.CANCELLED);
        assertThat(orderService.allowedNext(OrderStatus.COOKING)).containsExactly(OrderStatus.READY);
        assertThat(orderService.allowedNext(OrderStatus.READY)).containsExactly(OrderStatus.ON_THE_WAY);
        assertThat(orderService.allowedNext(OrderStatus.ON_THE_WAY)).containsExactly(OrderStatus.DELIVERED);
        assertThat(orderService.allowedNext(OrderStatus.DELIVERED)).isEmpty();
        assertThat(orderService.allowedNext(OrderStatus.CANCELLED)).isEmpty();
    }

    @Test
    void cancelledOrdersDoNotPretendToBeDeliveredInProgressTracker() {
        assertThat(orderService.progressStep(OrderStatus.CANCELLED)).isEqualTo(-1);
        assertThat(orderService.progressStep(OrderStatus.DELIVERED)).isEqualTo(6);
    }
    @Test
    void sellerCancellationUpdatesPaymentState() {
        User seller = User.builder().id(7L).build();
        FoodCart cart = FoodCart.builder().id(8L).owner(seller).build();
        CustomerOrder order = CustomerOrder.builder()
                .id(9L)
                .orderNumber("FMC-CANCEL")
                .foodCart(cart)
                .status(OrderStatus.ACCEPTED)
                .build();
        Delivery delivery = Delivery.builder().order(order).build();

        when(orderRepository.findByOrderNumber("FMC-CANCEL")).thenReturn(Optional.of(order));
        when(deliveryRepository.findByOrder(order)).thenReturn(Optional.of(delivery));

        orderService.updateStatusBySeller(seller, "FMC-CANCEL", OrderStatus.CANCELLED);

        verify(paymentService).requestRefundIfNeeded(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

}
