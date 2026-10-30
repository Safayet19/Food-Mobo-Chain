package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.FoodItem;
import com.safayet.foodmobochain.model.Review;
import com.safayet.foodmobochain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBuyerAndFoodItem(User buyer, FoodItem foodItem);
    boolean existsByBuyerAndFoodCart(User buyer, FoodCart foodCart);

    @EntityGraph(attributePaths = "buyer")
    List<Review> findByFoodItemAndApprovedTrueAndHiddenFalseOrderByCreatedAtDesc(FoodItem foodItem);

    @EntityGraph(attributePaths = "buyer")
    List<Review> findByFoodCartAndApprovedTrueAndHiddenFalseOrderByCreatedAtDesc(FoodCart foodCart);

    @EntityGraph(attributePaths = {"buyer", "foodItem", "foodCart"})
    List<Review> findByApprovedFalseAndHiddenFalseOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = {"buyer", "foodItem", "foodCart"})
    List<Review> findAllByOrderByCreatedAtDesc();

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.foodItem = :food and r.approved = true and r.hidden = false")
    Double averageForFood(@Param("food") FoodItem food);

    @Query("select coalesce(avg(r.rating), 0) from Review r where r.foodCart = :cart and r.approved = true and r.hidden = false")
    Double averageForCart(@Param("cart") FoodCart cart);
}
