package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.dto.CheckoutDTO;
import com.safayet.foodmobochain.model.*;
import com.safayet.foodmobochain.model.enums.*;
import com.safayet.foodmobochain.repository.DeliveryRepository;
import com.safayet.foodmobochain.repository.FoodCartRepository;
import com.safayet.foodmobochain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final FoodCartRepository foodCartRepository;
    private final CartService cartService;
    private final DiscountService discountService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Transactional
    public CustomerOrder createOrder(User buyer, CheckoutDTO dto) {
        Cart cart = cartService.getOrCreate(buyer);
        if (cart.getItems().isEmpty() || cart.getFoodCart() == null) {
            throw new IllegalArgumentException("Your cart is empty.");
        }
        if (!cart.getFoodCart().isApproved()
                || !cart.getFoodCart().isOpen()
                || !cart.getFoodCart().getOwner().isEnabled()) {
            throw new IllegalArgumentException("This food cart is currently unavailable.");
        }
        if (cart.getItems().stream().anyMatch(item ->
                !item.getFoodItem().isAvailable()
                        || item.getFoodItem().isArchived()
                        || !item.getFoodItem().getCategory().isActive()
                        || !item.getFoodItem().getFoodCart().getId().equals(cart.getFoodCart().getId()))) {
            throw new IllegalArgumentException("One or more items in your cart are no longer available.");
        }

        BigDecimal subtotal = cartService.subtotal(cart);
        DiscountService.AppliedDiscount applied = discountService.calculate(dto.getDiscountCode(), subtotal);
        BigDecimal total = subtotal
                .subtract(applied.amount())
                .add(cart.getFoodCart().getDeliveryFee());

        OrderStatus initialStatus = dto.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                ? OrderStatus.CONFIRMED
                : OrderStatus.PENDING_PAYMENT;

        CustomerOrder order = CustomerOrder.builder()
                .orderNumber(generateOrderNumber())
                .buyer(buyer)
                .foodCart(cart.getFoodCart())
                .status(initialStatus)
                .subtotal(subtotal)
                .discountAmount(applied.amount())
                .deliveryFee(cart.getFoodCart().getDeliveryFee())
                .total(total)
                .discountCode(applied.discount() == null ? null : applied.discount().getCode())
                .deliveryAddress(dto.getDeliveryAddress().trim())
                .phone(dto.getPhone().trim())
                .note(dto.getNote() == null || dto.getNote().isBlank() ? null : dto.getNote().trim())
                .build();

        List<OrderItem> snapshots = cart.getItems().stream()
                .map(item -> OrderItem.builder()
                        .order(order)
                        .foodItemIdSnapshot(item.getFoodItem().getId())
                        .foodName(item.getFoodItem().getName())
                        .foodImage(item.getFoodItem().getImage())
                        .unitPrice(item.getFoodItem().getPrice())
                        .quantity(item.getQuantity())
                        .spiceLevel(item.getSpiceLevel())
                        .subtotal(item.getFoodItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();
        order.setItems(new ArrayList<>(snapshots));
        CustomerOrder savedOrder = orderRepository.save(order);

        paymentService.createForOrder(savedOrder, dto.getPaymentMethod());
        deliveryRepository.save(Delivery.builder()
                .order(savedOrder)
                .status(DeliveryStatus.WAITING)
                .address(savedOrder.getDeliveryAddress())
                .contactNumber(savedOrder.getPhone())
                .estimatedMinutes(savedOrder.getFoodCart().getEstimatedDeliveryMinutes())
                .build());

        cartService.clear(buyer);

        notificationService.send(buyer, NotificationType.ORDER,
                dto.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY ? "Order confirmed" : "Order created — payment pending",
                "Order " + savedOrder.getOrderNumber() + " has been created.",
                "/orders/" + savedOrder.getOrderNumber());

        if (dto.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            notificationService.send(savedOrder.getFoodCart().getOwner(), NotificationType.ORDER,
                    "New order received",
                    "Order " + savedOrder.getOrderNumber() + " is ready for processing.",
                    "/seller/orders/" + savedOrder.getOrderNumber());
        }

        return savedOrder;
    }

    public List<CustomerOrder> buyerOrders(User buyer) {
        return orderRepository.findByBuyerOrderByCreatedAtDesc(buyer);
    }

    public CustomerOrder buyerOrder(User buyer, String orderNumber) {
        CustomerOrder order = getByNumber(orderNumber);
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new SecurityException("You cannot access this order.");
        }
        return order;
    }

    public List<CustomerOrder> sellerOrders(User seller) {
        FoodCart cart = sellerCart(seller);
        return orderRepository.findByFoodCartOrderByCreatedAtDesc(cart);
    }

    public CustomerOrder sellerOrder(User seller, String orderNumber) {
        CustomerOrder order = getByNumber(orderNumber);
        if (!order.getFoodCart().getOwner().getId().equals(seller.getId())) {
            throw new SecurityException("You cannot access this order.");
        }
        return order;
    }

    public CustomerOrder getByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order was not found."));
    }

    public List<CustomerOrder> recentOrders() {
        return orderRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<Delivery> recentDeliveries() {
        return deliveryRepository.findTop50ByOrderByOrderCreatedAtDesc();
    }

    public Payment paymentFor(CustomerOrder order) {
        return paymentService.paymentFor(order);
    }

    public Delivery deliveryFor(CustomerOrder order) {
        return deliveryRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalArgumentException("Delivery record was not found."));
    }

    @Transactional
    public void cancelByBuyer(User buyer, String orderNumber) {
        CustomerOrder order = buyerOrder(buyer, orderNumber);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new IllegalArgumentException("This order can no longer be cancelled.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        Delivery delivery = deliveryFor(order);
        delivery.setStatus(DeliveryStatus.CANCELLED);
        deliveryRepository.save(delivery);
        paymentService.requestRefundIfNeeded(order);

        notificationService.send(order.getFoodCart().getOwner(), NotificationType.ORDER,
                "Order cancelled",
                "Order " + order.getOrderNumber() + " was cancelled by the buyer.",
                "/seller/orders/" + order.getOrderNumber());
    }

    @Transactional
    public void updateStatusBySeller(User seller, String orderNumber, OrderStatus nextStatus) {
        CustomerOrder order = sellerOrder(seller, orderNumber);
        if (!allowedNext(order.getStatus()).contains(nextStatus)) {
            throw new IllegalArgumentException("Invalid order-status transition.");
        }
        order.setStatus(nextStatus);
        orderRepository.save(order);

        Delivery delivery = deliveryFor(order);
        switch (nextStatus) {
            case ACCEPTED, COOKING, READY -> delivery.setStatus(DeliveryStatus.PREPARING);
            case ON_THE_WAY -> {
                delivery.setStatus(DeliveryStatus.ON_THE_WAY);
                delivery.setDispatchedAt(LocalDateTime.now());
            }
            case DELIVERED -> {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                delivery.setDeliveredAt(LocalDateTime.now());
                paymentService.markCashOnDeliveryCollected(order);
            }
            case CANCELLED -> {
                delivery.setStatus(DeliveryStatus.CANCELLED);
                paymentService.requestRefundIfNeeded(order);
            }
            default -> { }
        }
        deliveryRepository.save(delivery);

        String friendly = nextStatus.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        notificationService.send(order.getBuyer(), NotificationType.ORDER,
                "Order update",
                "Order " + order.getOrderNumber() + " is now " + friendly + ".",
                "/orders/" + order.getOrderNumber());
    }


    @Transactional
    public void cancelByAdmin(String orderNumber) {
        CustomerOrder order = getByNumber(orderNumber);
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("This order is already in a final state.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        Delivery delivery = deliveryFor(order);
        delivery.setStatus(DeliveryStatus.CANCELLED);
        deliveryRepository.save(delivery);
        paymentService.requestRefundIfNeeded(order);

        notificationService.send(order.getBuyer(), NotificationType.ORDER,
                "Order cancelled by support",
                "Order " + order.getOrderNumber() + " was cancelled by an administrator.",
                "/orders/" + order.getOrderNumber());
        notificationService.send(order.getFoodCart().getOwner(), NotificationType.ORDER,
                "Order cancelled by support",
                "Order " + order.getOrderNumber() + " was cancelled by an administrator.",
                "/seller/orders/" + order.getOrderNumber());
    }

    public Set<OrderStatus> allowedNext(OrderStatus current) {
        return switch (current) {
            case CONFIRMED -> EnumSet.of(OrderStatus.ACCEPTED);
            case ACCEPTED -> EnumSet.of(OrderStatus.COOKING, OrderStatus.CANCELLED);
            case COOKING -> EnumSet.of(OrderStatus.READY);
            case READY -> EnumSet.of(OrderStatus.ON_THE_WAY);
            case ON_THE_WAY -> EnumSet.of(OrderStatus.DELIVERED);
            default -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canBuyerCancel(CustomerOrder order) {
        return order.getStatus() == OrderStatus.PENDING_PAYMENT
                || order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.ACCEPTED;
    }

    public int progressStep(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> 0;
            case CONFIRMED -> 1;
            case ACCEPTED -> 2;
            case COOKING -> 3;
            case READY -> 4;
            case ON_THE_WAY -> 5;
            case DELIVERED -> 6;
            case CANCELLED -> -1;
        };
    }

    private FoodCart sellerCart(User seller) {
        return foodCartRepository.findByOwner(seller)
                .orElseThrow(() -> new IllegalArgumentException("Seller food cart was not found."));
    }

    private String generateOrderNumber() {
        String prefix = "FMC" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String candidate;
        do {
            candidate = prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        } while (orderRepository.existsByOrderNumber(candidate));
        return candidate;
    }
}
