package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.dto.ReviewDTO;
import com.safayet.foodmobochain.model.*;
import com.safayet.foodmobochain.model.enums.NotificationType;
import com.safayet.foodmobochain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodCartRepository foodCartRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional
    public void submit(User buyer, ReviewDTO dto) {
        if ((dto.getFoodItemId() == null) == (dto.getFoodCartId() == null)) {
            throw new IllegalArgumentException("Choose exactly one food item or food cart to review.");
        }
        if (dto.getFoodItemId() != null) {
            FoodItem food = foodItemRepository.findById(dto.getFoodItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
            if (orderRepository.countDeliveredOrdersWithFood(buyer, food.getId()) == 0) {
                throw new IllegalArgumentException("You can review a food item after a delivered order.");
            }
            if (reviewRepository.existsByBuyerAndFoodItem(buyer, food)) {
                throw new IllegalArgumentException("You have already reviewed this food item.");
            }
            reviewRepository.save(Review.builder()
                    .buyer(buyer)
                    .foodItem(food)
                    .rating(dto.getRating())
                    .comment(dto.getComment().trim())
                    .approved(false)
                    .hidden(false)
                    .build());
            return;
        }

        FoodCart cart = foodCartRepository.findById(dto.getFoodCartId())
                .orElseThrow(() -> new IllegalArgumentException("Food cart was not found."));
        if (orderRepository.countDeliveredOrdersFromCart(buyer, cart) == 0) {
            throw new IllegalArgumentException("You can review a food cart after a delivered order.");
        }
        if (reviewRepository.existsByBuyerAndFoodCart(buyer, cart)) {
            throw new IllegalArgumentException("You have already reviewed this food cart.");
        }
        reviewRepository.save(Review.builder()
                .buyer(buyer)
                .foodCart(cart)
                .rating(dto.getRating())
                .comment(dto.getComment().trim())
                .approved(false)
                .hidden(false)
                .build());
    }

    public List<Review> pending() {
        return reviewRepository.findByApprovedFalseAndHiddenFalseOrderByCreatedAtAsc();
    }

    public List<Review> all() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void approve(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review was not found."));
        review.setApproved(true);
        review.setHidden(false);
        reviewRepository.save(review);
        notificationService.send(review.getBuyer(), NotificationType.SYSTEM,
                "Review approved",
                "Your review is now visible to other customers.",
                review.getFoodItem() != null
                        ? "/foods/" + review.getFoodItem().getId()
                        : "/food-carts/" + review.getFoodCart().getSlug());
    }

    @Transactional
    public void hide(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review was not found."));
        review.setHidden(true);
        reviewRepository.save(review);
    }
}
