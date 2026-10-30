package com.safayet.foodmobochain.service;

import com.safayet.foodmobochain.dto.FoodCartDTO;
import com.safayet.foodmobochain.dto.FoodItemDTO;
import com.safayet.foodmobochain.model.Category;
import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.FoodItem;
import com.safayet.foodmobochain.model.Review;
import com.safayet.foodmobochain.model.User;
import com.safayet.foodmobochain.model.enums.NotificationType;
import com.safayet.foodmobochain.repository.CategoryRepository;
import com.safayet.foodmobochain.repository.FoodCartRepository;
import com.safayet.foodmobochain.repository.FoodItemRepository;
import com.safayet.foodmobochain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final FoodCartRepository foodCartRepository;
    private final FoodItemRepository foodItemRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    // ---------- Public catalog ----------

    public List<FoodCart> carts() {
        return foodCartRepository.findPublicApproved();
    }

    public FoodCart cart(String slug) {
        FoodCart cart = foodCartRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Food cart was not found."));
        if (!cart.isApproved() || !cart.getOwner().isEnabled()) {
            throw new IllegalArgumentException("Food cart was not found.");
        }
        return cart;
    }

    public List<FoodItem> menu(FoodCart cart) {
        return foodItemRepository.findByFoodCartAndArchivedFalseOrderByNameAsc(cart).stream()
                .filter(FoodItem::isAvailable)
                .filter(food -> food.getCategory().isActive())
                .toList();
    }

    public Page<FoodItem> searchFoods(String query, String category, Long cartId,
                                      BigDecimal minPrice, BigDecimal maxPrice,
                                      String sort, int page, int size) {
        Sort sorting = switch (sort == null ? "popular" : sort) {
            case "priceLow", "price-low" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceHigh", "price-high" -> Sort.by(Sort.Direction.DESC, "price");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("createdAt"));
        };
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 48), sorting);
        return foodItemRepository.searchPublic(blankToNull(query), blankToNull(category), cartId, minPrice, maxPrice, pageable);
    }

    public FoodItem food(Long id) {
        FoodItem food = foodItemRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        if (food.isArchived() || !food.isAvailable() || !food.getCategory().isActive() || !food.getFoodCart().isApproved()) {
            throw new IllegalArgumentException("Food item was not found.");
        }
        return food;
    }

    public List<FoodItem> featuredFoods() {
        return foodItemRepository.findFeaturedPublic(PageRequest.of(0, 8));
    }

    public List<Category> categories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc();
    }

    public List<Review> foodReviews(FoodItem food) {
        return reviewRepository.findByFoodItemAndApprovedTrueAndHiddenFalseOrderByCreatedAtDesc(food);
    }

    public List<Review> cartReviews(FoodCart cart) {
        return reviewRepository.findByFoodCartAndApprovedTrueAndHiddenFalseOrderByCreatedAtDesc(cart);
    }

    public double foodRating(FoodItem food) {
        return roundRating(reviewRepository.averageForFood(food));
    }

    public double cartRating(FoodCart cart) {
        return roundRating(reviewRepository.averageForCart(cart));
    }

    // ---------- Seller catalog ----------

    public FoodCart sellerCart(User seller) {
        return foodCartRepository.findByOwner(seller)
                .orElseThrow(() -> new IllegalArgumentException("Seller food cart was not found."));
    }

    public List<FoodItem> sellerMenu(User seller) {
        return foodItemRepository.findByFoodCartAndArchivedFalseOrderByNameAsc(sellerCart(seller));
    }

    public FoodCartDTO sellerCartDto(User seller) {
        FoodCart cart = sellerCart(seller);
        return FoodCartDTO.builder()
                .name(cart.getName())
                .description(cart.getDescription())
                .location(cart.getLocation())
                .cuisine(cart.getCuisine())
                .coverImage(cart.getCoverImage())
                .deliveryFee(cart.getDeliveryFee())
                .estimatedDeliveryMinutes(cart.getEstimatedDeliveryMinutes())
                .build();
    }

    @Transactional
    public void updateSellerCart(User seller, FoodCartDTO dto) {
        FoodCart cart = sellerCart(seller);
        if (!cart.getName().equalsIgnoreCase(dto.getName().trim())) {
            cart.setSlug(uniqueCartSlug(dto.getName()));
        }
        cart.setName(dto.getName().trim());
        cart.setDescription(dto.getDescription().trim());
        cart.setLocation(dto.getLocation().trim());
        cart.setCuisine(dto.getCuisine().trim());
        cart.setCoverImage(dto.getCoverImage().trim());
        cart.setDeliveryFee(dto.getDeliveryFee());
        cart.setEstimatedDeliveryMinutes(dto.getEstimatedDeliveryMinutes());
        foodCartRepository.save(cart);
    }

    @Transactional
    public void toggleSellerCartOpen(User seller) {
        FoodCart cart = sellerCart(seller);
        if (!cart.isApproved() || !cart.getOwner().isEnabled()) {
            throw new IllegalArgumentException("Your food cart must be approved by an administrator before it can open.");
        }
        cart.setOpen(!cart.isOpen());
        foodCartRepository.save(cart);
    }

    @Transactional
    public FoodItem createFood(User seller, FoodItemDTO dto) {
        FoodCart cart = sellerCart(seller);
        Category category = activeCategory(dto.getCategoryId());
        return foodItemRepository.save(FoodItem.builder()
                .foodCart(cart)
                .category(category)
                .name(dto.getName().trim())
                .slug(slugify(dto.getName()))
                .description(dto.getDescription().trim())
                .price(dto.getPrice())
                .image(dto.getImage().trim())
                .available(dto.isAvailable())
                .featured(dto.isFeatured())
                .spicySupported(dto.isSpicySupported())
                .build());
    }

    public FoodItem sellerFood(User seller, Long foodId) {
        FoodItem food = foodItemRepository.findDetailedById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        if (!food.getFoodCart().getOwner().getId().equals(seller.getId())) {
            throw new SecurityException("You cannot manage this food item.");
        }
        return food;
    }

    public FoodItemDTO sellerFoodDto(User seller, Long foodId) {
        FoodItem food = sellerFood(seller, foodId);
        return FoodItemDTO.builder()
                .name(food.getName())
                .categoryId(food.getCategory().getId())
                .description(food.getDescription())
                .price(food.getPrice())
                .image(food.getImage())
                .available(food.isAvailable())
                .featured(food.isFeatured())
                .spicySupported(food.isSpicySupported())
                .build();
    }

    @Transactional
    public void updateFood(User seller, Long foodId, FoodItemDTO dto) {
        FoodItem food = sellerFood(seller, foodId);
        Category category = activeCategory(dto.getCategoryId());
        food.setName(dto.getName().trim());
        food.setSlug(slugify(dto.getName()));
        food.setCategory(category);
        food.setDescription(dto.getDescription().trim());
        food.setPrice(dto.getPrice());
        food.setImage(dto.getImage().trim());
        food.setAvailable(dto.isAvailable());
        food.setFeatured(dto.isFeatured());
        food.setSpicySupported(dto.isSpicySupported());
        foodItemRepository.save(food);
    }

    @Transactional
    public void archiveFood(User seller, Long foodId) {
        FoodItem food = sellerFood(seller, foodId);
        food.setArchived(true);
        food.setAvailable(false);
        food.setFeatured(false);
        foodItemRepository.save(food);
    }

    @Transactional
    public void toggleSellerFoodAvailability(User seller, Long foodId) {
        FoodItem food = sellerFood(seller, foodId);
        food.setAvailable(!food.isAvailable());
        foodItemRepository.save(food);
    }

    // ---------- Admin catalog ----------

    public List<FoodCart> allCarts() {
        return foodCartRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<FoodItem> allFoods() {
        return foodItemRepository.findAllByOrderByNameAsc();
    }

    public List<Category> allCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public void toggleCartApproval(Long cartId) {
        FoodCart cart = foodCartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Food cart was not found."));
        cart.setApproved(!cart.isApproved());
        if (!cart.isApproved() || !cart.getOwner().isEnabled()) {
            cart.setOpen(false);
        }
        foodCartRepository.save(cart);
        notificationService.send(cart.getOwner(), NotificationType.ACCOUNT,
                cart.isApproved() ? "Food cart approved" : "Food cart suspended",
                cart.isApproved()
                        ? "Your food cart has been approved. You can now open it for orders."
                        : "Your food cart has been suspended by an administrator.",
                "/seller/food-cart");
    }

    @Transactional
    public void updateFoodPrice(Long foodId, BigDecimal price) {
        if (price == null || price.signum() <= 0 || price.compareTo(new BigDecimal("100000")) > 0) {
            throw new IllegalArgumentException("Enter a valid food price.");
        }
        FoodItem food = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        food.setPrice(price);
        foodItemRepository.save(food);
    }

    @Transactional
    public void toggleFoodAvailability(Long foodId) {
        FoodItem food = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        if (food.isArchived()) {
            throw new IllegalArgumentException("Restore this food item before changing availability.");
        }
        food.setAvailable(!food.isAvailable());
        foodItemRepository.save(food);
    }

    @Transactional
    public void toggleFoodFeatured(Long foodId) {
        FoodItem food = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        if (food.isArchived()) {
            throw new IllegalArgumentException("Restore this food item before changing featured status.");
        }
        food.setFeatured(!food.isFeatured());
        foodItemRepository.save(food);
    }

    @Transactional
    public void toggleFoodArchived(Long foodId) {
        FoodItem food = foodItemRepository.findById(foodId)
                .orElseThrow(() -> new IllegalArgumentException("Food item was not found."));
        food.setArchived(!food.isArchived());
        if (food.isArchived()) {
            food.setAvailable(false);
            food.setFeatured(false);
        }
        foodItemRepository.save(food);
    }

    @Transactional
    public Category createCategory(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.length() < 2) {
            throw new IllegalArgumentException("Category name is too short.");
        }
        if (categoryRepository.findByNameIgnoreCase(clean).isPresent()) {
            throw new IllegalArgumentException("This category already exists.");
        }
        String slug = slugify(clean);
        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("A category with this slug already exists.");
        }
        return categoryRepository.save(Category.builder()
                .name(clean)
                .slug(slug)
                .image("/images/ui/category-default.svg")
                .active(true)
                .build());
    }

    @Transactional
    public void toggleCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category was not found."));
        category.setActive(!category.isActive());
        categoryRepository.save(category);
    }

    public static String slugify(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return normalized.isBlank() ? "item" : normalized;
    }

    private Category activeCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category was not found."));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Choose an active food category.");
        }
        return category;
    }

    private String uniqueCartSlug(String value) {
        String base = slugify(value);
        String candidate = base;
        int counter = 2;
        while (foodCartRepository.existsBySlug(candidate)) {
            candidate = base + "-" + counter++;
        }
        return candidate;
    }

    private double roundRating(Double value) {
        return value == null ? 0.0 : Math.round(value * 10.0) / 10.0;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
