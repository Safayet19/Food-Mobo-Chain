package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.Cart;
import com.safayet.foodmobochain.model.CartItem;
import com.safayet.foodmobochain.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndFoodItem(Cart cart, FoodItem foodItem);
    long countByCart(Cart cart);
}
