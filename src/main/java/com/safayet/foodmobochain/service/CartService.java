package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.model.*;
import com.safayet.foodmobochain.model.enums.SpiceLevel;
import com.safayet.foodmobochain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FoodItemRepository foodItemRepository;

    @Transactional
    public Cart getOrCreate(User buyer) {
        return cartRepository.findByBuyer(buyer)
                .orElseGet(() -> cartRepository.save(Cart.builder().buyer(buyer).build()));
    }

    public Optional<Cart> find(User buyer) {
        return cartRepository.findByBuyer(buyer);
    }

    @Transactional
    public void add(User buyer, Long foodId, int quantity, SpiceLevel spiceLevel) {
        FoodItem food = foodItemRepository.findDetailedById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        if (!food.isAvailable()
                || food.isArchived()
                || !food.getCategory().isActive()
                || !food.getFoodCart().isApproved()
                || !food.getFoodCart().isOpen()
                || !food.getFoodCart().getOwner().isEnabled()) {
            throw new IllegalArgumentException("This food item is not available right now.");
        }

        Cart cart = getOrCreate(buyer);
        if (cart.getFoodCart() != null && !cart.getFoodCart().getId().equals(food.getFoodCart().getId())) {
            throw new IllegalArgumentException("Your cart contains items from another food cart. Clear it before adding this item.");
        }
        cart.setFoodCart(food.getFoodCart());

        CartItem item = cartItemRepository.findByCartAndFoodItem(cart, food)
                .orElseGet(() -> CartItem.builder()
                        .cart(cart)
                        .foodItem(food)
                        .quantity(0)
                        .spiceLevel(SpiceLevel.REGULAR)
                        .build());

        item.setQuantity(Math.min(20, item.getQuantity() + Math.max(1, quantity)));
        item.setSpiceLevel(food.isSpicySupported() && spiceLevel != null ? spiceLevel : SpiceLevel.REGULAR);
        cartItemRepository.save(item);
        cartRepository.save(cart);
    }

    @Transactional
    public void update(User buyer, Long itemId, int quantity, SpiceLevel spiceLevel) {
        Cart cart = getOrCreate(buyer);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item was not found."));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new SecurityException("You cannot modify this cart item.");
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(Math.min(quantity, 20));
            item.setSpiceLevel(item.getFoodItem().isSpicySupported() && spiceLevel != null ? spiceLevel : SpiceLevel.REGULAR);
            cartItemRepository.save(item);
        }
        cleanupEmptyCart(cart);
    }

    @Transactional
    public void remove(User buyer, Long itemId) {
        Cart cart = getOrCreate(buyer);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item was not found."));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new SecurityException("You cannot remove this cart item.");
        }
        cartItemRepository.delete(item);
        cart.getItems().removeIf(existing -> existing.getId().equals(itemId));
        cleanupEmptyCart(cart);
    }

    @Transactional
    public void clear(User buyer) {
        Cart cart = getOrCreate(buyer);
        cart.getItems().clear();
        cart.setFoodCart(null);
        cartRepository.save(cart);
    }

    public BigDecimal subtotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getFoodItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int itemCount(User buyer) {
        return cartRepository.findByBuyer(buyer)
                .map(cart -> cart.getItems().stream().mapToInt(CartItem::getQuantity).sum())
                .orElse(0);
    }

    private void cleanupEmptyCart(Cart cart) {
        if (cartItemRepository.countByCart(cart) == 0) {
            cart.setFoodCart(null);
            cartRepository.save(cart);
        }
    }
}
