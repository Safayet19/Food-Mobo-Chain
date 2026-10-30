package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.CustomerOrder;
import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.User;
import com.safayet.foodmobochain.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    @EntityGraph(attributePaths = {"buyer", "foodCart", "items"})
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"foodCart", "items"})
    List<CustomerOrder> findByBuyerOrderByCreatedAtDesc(User buyer);

    @EntityGraph(attributePaths = {"buyer", "items"})
    List<CustomerOrder> findByFoodCartOrderByCreatedAtDesc(FoodCart foodCart);

    @EntityGraph(attributePaths = {"buyer", "foodCart"})
    List<CustomerOrder> findTop20ByOrderByCreatedAtDesc();

    long countByFoodCart(FoodCart foodCart);
    long countByStatus(OrderStatus status);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByFoodCartAndCreatedAtBetween(FoodCart cart, LocalDateTime start, LocalDateTime end);

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.status = com.safayet.foodmobochain.model.enums.OrderStatus.DELIVERED")
    BigDecimal sumDeliveredRevenue();

    @Query("select coalesce(sum(o.total), 0) from CustomerOrder o where o.foodCart = :cart and o.status = com.safayet.foodmobochain.model.enums.OrderStatus.DELIVERED")
    BigDecimal sumDeliveredRevenueByCart(@Param("cart") FoodCart cart);

    @Query("select count(o) from CustomerOrder o where o.foodCart = :cart and o.createdAt >= :from")
    long countByCartSince(@Param("cart") FoodCart cart, @Param("from") LocalDateTime from);

    @Query("select count(o) from CustomerOrder o where o.createdAt >= :from")
    long countSince(@Param("from") LocalDateTime from);

    @Query("select count(o) from CustomerOrder o join o.items i where o.buyer = :buyer and o.status = com.safayet.foodmobochain.model.enums.OrderStatus.DELIVERED and i.foodItemIdSnapshot = :foodId")
    long countDeliveredOrdersWithFood(@Param("buyer") User buyer, @Param("foodId") Long foodId);

    @Query("select count(o) from CustomerOrder o where o.buyer = :buyer and o.foodCart = :cart and o.status = com.safayet.foodmobochain.model.enums.OrderStatus.DELIVERED")
    long countDeliveredOrdersFromCart(@Param("buyer") User buyer, @Param("cart") FoodCart cart);
}

