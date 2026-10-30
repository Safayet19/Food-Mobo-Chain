package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.Cart;
import com.safayet.foodmobochain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @EntityGraph(attributePaths = {"foodCart", "items", "items.foodItem", "items.foodItem.category", "items.foodItem.foodCart"})
    Optional<Cart> findByBuyer(User buyer);
}
