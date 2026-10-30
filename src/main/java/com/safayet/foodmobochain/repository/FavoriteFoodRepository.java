package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.FavoriteFood;
import com.safayet.foodmobochain.model.FoodItem;
import com.safayet.foodmobochain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteFoodRepository extends JpaRepository<FavoriteFood, Long> {
    Optional<FavoriteFood> findByUserAndFoodItem(User user, FoodItem foodItem);
    boolean existsByUserAndFoodItem(User user, FoodItem foodItem);
    @EntityGraph(attributePaths = {"foodItem", "foodItem.foodCart", "foodItem.category"})
    List<FavoriteFood> findByUserOrderByIdDesc(User user);
}
