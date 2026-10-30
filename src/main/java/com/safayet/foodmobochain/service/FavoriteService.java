package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.model.*;
import com.safayet.foodmobochain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteFoodRepository favoriteFoodRepository;
    private final FavoriteCartRepository favoriteCartRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodCartRepository foodCartRepository;

    @Transactional
    public boolean toggleFood(User user, Long foodId) {
        FoodItem food = foodItemRepository.findDetailedById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        if (!isPublicFood(food)) {
            throw new IllegalArgumentException("This food item is not available to save right now.");
        }
        var existing = favoriteFoodRepository.findByUserAndFoodItem(user, food);
        if (existing.isPresent()) {
            favoriteFoodRepository.delete(existing.get());
            return false;
        }
        favoriteFoodRepository.save(FavoriteFood.builder().user(user).foodItem(food).build());
        return true;
    }

    @Transactional
    public boolean toggleCart(User user, Long cartId) {
        FoodCart cart = foodCartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Food cart was not found."));
        if (!cart.isApproved() || !cart.getOwner().isEnabled()) {
            throw new IllegalArgumentException("This food cart is not available to save right now.");
        }
        var existing = favoriteCartRepository.findByUserAndFoodCart(user, cart);
        if (existing.isPresent()) {
            favoriteCartRepository.delete(existing.get());
            return false;
        }
        favoriteCartRepository.save(FavoriteCart.builder().user(user).foodCart(cart).build());
        return true;
    }

    public List<FavoriteFood> foods(User user) {
        return favoriteFoodRepository.findByUserOrderByIdDesc(user).stream()
                .filter(favorite -> isPublicFood(favorite.getFoodItem()))
                .toList();
    }

    public List<FavoriteCart> carts(User user) {
        return favoriteCartRepository.findByUserOrderByIdDesc(user).stream()
                .filter(favorite -> favorite.getFoodCart().isApproved() && favorite.getFoodCart().getOwner().isEnabled())
                .toList();
    }

    public boolean isFoodFavorite(User user, FoodItem food) {
        return favoriteFoodRepository.existsByUserAndFoodItem(user, food);
    }

    public boolean isCartFavorite(User user, FoodCart cart) {
        return favoriteCartRepository.existsByUserAndFoodCart(user, cart);
    }

    private boolean isPublicFood(FoodItem food) {
        return food.isAvailable()
                && !food.isArchived()
                && food.getCategory().isActive()
                && food.getFoodCart().isApproved()
                && food.getFoodCart().getOwner().isEnabled();
    }
}
