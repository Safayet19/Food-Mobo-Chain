package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.enums.OrderStatus;
import com.safayet.foodmobochain.model.enums.PaymentStatus;
import com.safayet.foodmobochain.model.enums.Role;
import com.safayet.foodmobochain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final FoodCartRepository foodCartRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboard adminDashboard() {
        return new AdminDashboard(
                userRepository.count(),
                foodCartRepository.count(),
                orderRepository.count(),
                paymentRepository.sumPaidAmount(),
                foodCartRepository.countByApprovedFalse(),
                paymentRepository.countByStatus(PaymentStatus.PENDING),
                weeklyOrders(null),
                orderStatusCounts(),
                topFoods()
        );
    }

    public SellerDashboard sellerDashboard(FoodCart cart) {
        long orders = orderRepository.countByFoodCart(cart);
        BigDecimal revenue = orderRepository.sumDeliveredRevenueByCart(cart);
        long menuItems = foodItemRepository.countByFoodCartAndArchivedFalse(cart);
        long thisWeek = orderRepository.countByCartSince(cart, LocalDateTime.now().minusDays(7));
        return new SellerDashboard(orders, revenue, menuItems, thisWeek, weeklyOrders(cart));
    }

    public Map<OrderStatus, Long> orderStatusCounts() {
        Map<OrderStatus, Long> data = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            data.put(status, orderRepository.countByStatus(status));
        }
        return data;
    }

    public List<ChartPoint> weeklyOrders(FoodCart cart) {
        List<ChartPoint> points = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("EEE");
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = LocalDate.now().minusDays(offset);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            long count = cart == null
                    ? orderRepository.countByCreatedAtBetween(start, end)
                    : orderRepository.countByFoodCartAndCreatedAtBetween(cart, start, end);
            points.add(new ChartPoint(day.format(label), count));
        }
        return points;
    }

    public List<TopFood> topFoods() {
        return orderItemRepository.findTopSellingFoods(PageRequest.of(0, 8)).stream()
                .map(row -> new TopFood(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    public long buyerCount() {
        return userRepository.countByRole(Role.BUYER);
    }

    public long sellerCount() {
        return userRepository.countByRole(Role.SELLER);
    }

    public record ChartPoint(String label, long value) {}
    public record TopFood(String name, long quantity) {}

    public record AdminDashboard(long users,
                                 long foodCarts,
                                 long orders,
                                 BigDecimal revenue,
                                 long pendingCarts,
                                 long pendingPayments,
                                 List<ChartPoint> weeklyOrders,
                                 Map<OrderStatus, Long> orderStatuses,
                                 List<TopFood> topFoods) {}

    public record SellerDashboard(long orders,
                                  BigDecimal revenue,
                                  long menuItems,
                                  long ordersThisWeek,
                                  List<ChartPoint> weeklyOrders) {}
}
