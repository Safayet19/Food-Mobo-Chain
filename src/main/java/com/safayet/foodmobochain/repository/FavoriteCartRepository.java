package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.FavoriteCart;
import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteCartRepository extends JpaRepository<FavoriteCart, Long> {
    Optional<FavoriteCart> findByUserAndFoodCart(User user, FoodCart foodCart);
    boolean existsByUserAndFoodCart(User user, FoodCart foodCart);
    @EntityGraph(attributePaths = {"foodCart", "foodCart.owner"})
    List<FavoriteCart> findByUserOrderByIdDesc(User user);
}
