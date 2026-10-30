package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.CustomerOrder;
import com.safayet.foodmobochain.model.Delivery;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrder(CustomerOrder order);
    @EntityGraph(attributePaths = {"order", "order.buyer", "order.foodCart"})
    List<Delivery> findTop50ByOrderByOrderCreatedAtDesc();
}
