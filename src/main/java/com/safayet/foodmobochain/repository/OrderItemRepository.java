package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("""
        select oi.foodName, sum(oi.quantity)
        from OrderItem oi
        where oi.order.status = com.safayet.foodmobochain.model.enums.OrderStatus.DELIVERED
        group by oi.foodName
        order by sum(oi.quantity) desc
        """)
    List<Object[]> findTopSellingFoods(Pageable pageable);
}
