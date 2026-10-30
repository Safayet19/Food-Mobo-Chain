package com.safayet.foodmobochain.config;

import com.safayet.foodmobochain.model.Category;
import com.safayet.foodmobochain.model.Discount;
import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.FoodItem;
import com.safayet.foodmobochain.model.User;
import com.safayet.foodmobochain.model.enums.DiscountType;
import com.safayet.foodmobochain.model.enums.Role;
import com.safayet.foodmobochain.repository.CategoryRepository;
import com.safayet.foodmobochain.repository.DiscountRepository;
import com.safayet.foodmobochain.repository.FoodCartRepository;
import com.safayet.foodmobochain.repository.FoodItemRepository;
import com.safayet.foodmobochain.repository.OrderItemRepository;
import com.safayet.foodmobochain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Map<String, String> FOOD_IMAGE_BY_NAME = Map.ofEntries(
            Map.entry("beef tehari", "/images/foods/beef-tehari.webp"),
            Map.entry("beef rice", "/images/foods/beef-rice.webp"),
            Map.entry("bhuna khichuri", "/images/foods/bhuna-khichuri.webp"),
            Map.entry("biryani plate", "/images/foods/biryani-plate.webp"),
            Map.entry("biryani combo", "/images/foods/biryani-combo.webp"),
            Map.entry("biryani meal", "/images/foods/biryani-meal.webp"),
            Map.entry("biryani & drink combo", "/images/foods/biryani-drink-combo.webp"),
            Map.entry("spicy aloo chop", "/images/foods/spicy-aloo-chop.webp"),
            Map.entry("chotpoti", "/images/foods/chotpoti.webp"),
            Map.entry("aloo chop", "/images/foods/aloo-chop.webp"),
            Map.entry("chickpea chaat", "/images/foods/chickpea-chaat.webp"),
            Map.entry("classic beef burger", "/images/foods/classic-beef-burger.webp"),
            Map.entry("cheese burger", "/images/foods/cheese-burger.webp"),
            Map.entry("spicy beef burger", "/images/foods/spicy-beef-burger.webp"),
            Map.entry("loaded beef burger", "/images/foods/loaded-beef-burger.webp"),
            Map.entry("double cheese burger", "/images/foods/double-cheese-burger.webp"),
            Map.entry("signature burger", "/images/foods/signature-burger.webp"),
            Map.entry("classic cheese burger", "/images/foods/classic-cheese-burger.webp"),
            Map.entry("smoky beef burger", "/images/foods/smoky-beef-burger.webp"),
            Map.entry("chicken noodles", "/images/foods/chicken-noodles.webp"),
            Map.entry("thai noodles", "/images/foods/thai-noodles.webp"),
            Map.entry("spicy noodle bowl", "/images/foods/spicy-noodle-bowl.webp"),
            Map.entry("fresh grain bowl", "/images/foods/fresh-grain-bowl.webp"),
            Map.entry("healthy grain bowl", "/images/foods/healthy-grain-bowl.webp"),
            Map.entry("vegetable rice bowl", "/images/foods/vegetable-rice-bowl.webp"),
            Map.entry("hilsa curry", "/images/foods/hilsa-curry.webp"),
            Map.entry("spiced rice curry", "/images/foods/spiced-rice-curry.webp"),
            Map.entry("beef rice bowl", "/images/foods/beef-rice-bowl.webp"),
            Map.entry("grilled chicken with vegetables", "/images/foods/grilled-chicken-with-vegetables.webp"),
            Map.entry("grilled chicken platter", "/images/foods/grilled-chicken-platter.webp"),
            Map.entry("chicken steak platter", "/images/foods/chicken-steak-platter.webp"),
            Map.entry("spicy grilled chicken", "/images/foods/spicy-grilled-chicken.webp"),
            Map.entry("herb grilled chicken", "/images/foods/herb-grilled-chicken.webp"),
            Map.entry("chicken grill plate", "/images/foods/chicken-grill-plate.webp"),
            Map.entry("chicken momo", "/images/foods/chicken-momo.webp"),
            Map.entry("jilapi", "/images/foods/jilapi.webp"),
            Map.entry("mini jilapi", "/images/foods/mini-jilapi.webp"),
            Map.entry("house special burger", "/images/foods/house-special-burger.webp"),
            Map.entry("stir-fried noodles", "/images/foods/stir-fried-noodles.webp"),
            Map.entry("fresh rice bowl", "/images/foods/fresh-rice-bowl.webp"),
            Map.entry("grilled chicken salad", "/images/foods/grilled-chicken-salad.webp"),
            Map.entry("grill house burger", "/images/foods/grill-house-burger.webp")
    );

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FoodCartRepository foodCartRepository;
    private final FoodItemRepository foodItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiscountRepository discountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfMissing();
        createDemoBuyerIfMissing();
        seedCatalogIfEmpty();
        seedDiscountsIfMissing();
        reconcileLegacyImagePaths();
    }

    private void createAdminIfMissing() {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        userRepository.save(User.builder()
                .fullName("Food Mobo Chain Admin")
                .email(adminEmail.toLowerCase())
                .phone("01700000000")
                .password(passwordEncoder.encode(adminPassword))
                .address("Administration")
                .role(Role.ADMIN)
                .enabled(true)
                .build());
    }

    private void createDemoBuyerIfMissing() {
        if (userRepository.existsByEmailIgnoreCase("buyer@foodmobo.local")) {
            return;
        }
        userRepository.save(User.builder()
                .fullName("Demo Buyer")
                .email("buyer@foodmobo.local")
                .phone("01710000999")
                .password(passwordEncoder.encode("Buyer123!"))
                .address("Southeast University, Tejgaon, Dhaka")
                .role(Role.BUYER)
                .enabled(true)
                .build());
    }

    private void seedCatalogIfEmpty() {
        if (categoryRepository.count() > 0 || foodCartRepository.count() > 0 || foodItemRepository.count() > 0) {
            return;
        }

        Map<String, Category> categories = new HashMap<>();
        for (String row : CATEGORY_DATA) {
            String[] p = row.split("\\|", -1);
            Category category = categoryRepository.save(Category.builder()
                    .name(p[0]).slug(p[1]).image(p[2]).active(true).build());
            categories.put(category.getSlug(), category);
        }

        Map<String, FoodCart> carts = new HashMap<>();
        for (String row : CART_DATA) {
            String[] p = row.split("\\|", -1);
            User seller = userRepository.findByEmailIgnoreCase(p[0]).orElseGet(() -> userRepository.save(User.builder()
                    .fullName(p[1])
                    .email(p[0])
                    .phone(p[2])
                    .password(passwordEncoder.encode("Seller123!"))
                    .address(p[6])
                    .role(Role.SELLER)
                    .enabled(true)
                    .build()));

            FoodCart cart = foodCartRepository.save(FoodCart.builder()
                    .owner(seller)
                    .name(p[3])
                    .slug(p[4])
                    .description(p[5])
                    .location(p[6])
                    .cuisine(p[7])
                    .coverImage(p[8])
                    .deliveryFee(new BigDecimal(p[9]))
                    .estimatedDeliveryMinutes(Integer.parseInt(p[10]))
                    .open(true)
                    .approved(true)
                    .build());
            carts.put(cart.getSlug(), cart);
        }

        for (String row : FOOD_DATA) {
            String[] p = row.split("\\|", -1);
            FoodCart cart = carts.get(p[0]);
            Category category = categories.get(p[1]);
            foodItemRepository.save(FoodItem.builder()
                    .foodCart(cart)
                    .category(category)
                    .name(p[2])
                    .slug(p[3])
                    .description(p[4])
                    .price(new BigDecimal(p[5]))
                    .image(p[6])
                    .featured(Boolean.parseBoolean(p[7]))
                    .spicySupported(Boolean.parseBoolean(p[8]))
                    .available(true)
                    .build());
        }
    }


    /**
     * Earlier demo versions contained more food image files than the final curated
     * food image set. Existing MySQL rows survive application upgrades, so a browser
     * can otherwise keep requesting filenames that were intentionally removed.
     *
     * This pass is deliberately conservative: valid local assets and external seller
     * URLs are left untouched. Only missing local image paths are repaired, and
     * historical order snapshots receive the same safe replacement without changing
     * order names, prices, quantities or statuses.
     */
    private void reconcileLegacyImagePaths() {
        List<Category> repairedCategories = new ArrayList<>();
        for (Category category : categoryRepository.findAll()) {
            if (!isUsableImagePath(category.getImage())) {
                category.setImage(bestCategoryImage(category.getSlug()));
                repairedCategories.add(category);
            }
        }
        if (!repairedCategories.isEmpty()) {
            categoryRepository.saveAll(repairedCategories);
        }

        List<FoodItem> repairedFoods = new ArrayList<>();
        for (FoodItem food : foodItemRepository.findAll()) {
            String exactImage = exactFoodImage(food.getName());
            if (exactImage != null) {
                if (!exactImage.equals(food.getImage())) {
                    food.setImage(exactImage);
                    repairedFoods.add(food);
                }
            } else if (!isUsableImagePath(food.getImage())) {
                food.setImage(bestFoodImage(food.getName(), food.getCategory() == null ? null : food.getCategory().getSlug()));
                repairedFoods.add(food);
            }
        }
        if (!repairedFoods.isEmpty()) {
            foodItemRepository.saveAll(repairedFoods);
        }

        var repairedOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> {
                    String exactImage = exactFoodImage(item.getFoodName());
                    return exactImage != null ? !exactImage.equals(item.getFoodImage()) : !isUsableImagePath(item.getFoodImage());
                })
                .peek(item -> {
                    String exactImage = exactFoodImage(item.getFoodName());
                    item.setFoodImage(exactImage != null ? exactImage : bestFoodImage(item.getFoodName(), null));
                })
                .toList();
        if (!repairedOrderItems.isEmpty()) {
            orderItemRepository.saveAll(repairedOrderItems);
        }
    }

    private String exactFoodImage(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return FOOD_IMAGE_BY_NAME.get(name.trim().toLowerCase(Locale.ROOT));
    }

    private String bestCategoryImage(String slug) {
        if (slug == null) {
            return "/images/ui/category-default.svg";
        }
        return switch (slug.toLowerCase(Locale.ROOT)) {
            case "biryani-rice" -> "/images/foods/beef-tehari.webp";
            case "burgers-sandwiches" -> "/images/foods/classic-beef-burger.webp";
            case "pizza-pasta" -> "/images/foods/chicken-noodles.webp";
            case "bangladeshi" -> "/images/foods/hilsa-curry.webp";
            case "street-food-snacks" -> "/images/foods/chotpoti.webp";
            case "chicken-grill" -> "/images/foods/grilled-chicken-with-vegetables.webp";
            case "noodles-momo" -> "/images/foods/chicken-momo.webp";
            case "desserts-drinks" -> "/images/foods/jilapi.webp";
            default -> "/images/ui/category-default.svg";
        };
    }

    private boolean isUsableImagePath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return true;
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return new ClassPathResource("static" + normalized).exists();
    }

    private String bestFoodImage(String name, String categorySlug) {
        String exactImage = exactFoodImage(name);
        if (exactImage != null) {
            return exactImage;
        }
        String value = (name == null ? "" : name).toLowerCase(Locale.ROOT);
        String category = categorySlug == null ? "" : categorySlug.toLowerCase(Locale.ROOT);

        if (value.contains("tehari")) return "/images/foods/beef-tehari.webp";
        if (value.contains("khichuri")) return "/images/foods/bhuna-khichuri.webp";
        if (value.contains("biryani") || value.contains("polao") || value.contains("pulao")) return "/images/foods/biryani-plate.webp";
        if (value.contains("fried rice") || value.contains("rice bowl")) return "/images/foods/vegetable-rice-bowl.webp";
        if (value.contains("beef") && value.contains("rice")) return "/images/foods/beef-tehari.webp";
        if (value.contains("wrap")) return "/images/foods/grilled-chicken-salad.webp";
        if (value.contains("sub") || value.contains("sandwich")) return "/images/foods/grill-house-burger.webp";
        if (value.contains("cheese") && value.contains("burger")) return "/images/foods/cheese-burger.webp";
        if (value.contains("burger")) return "/images/foods/loaded-beef-burger.webp";
        if (value.contains("momo")) return "/images/foods/chicken-momo.webp";
        if (value.contains("noodle")) return "/images/foods/chicken-noodles.webp";
        if (value.contains("hilsa") || value.contains("fish")) return "/images/foods/hilsa-curry.webp";
        if (value.contains("chotpoti")) return "/images/foods/chotpoti.webp";
        if (value.contains("chaat")) return "/images/foods/chickpea-chaat.webp";
        if (value.contains("chop")) return "/images/foods/aloo-chop.webp";
        if (value.contains("jilapi") || value.contains("jalebi")) return "/images/foods/jilapi.webp";
        if (value.contains("steak")) return "/images/foods/chicken-steak-platter.webp";
        if (value.contains("grill") || value.contains("roast") || value.contains("chicken")) return "/images/foods/chicken-grill-plate.webp";
        if (value.contains("rice") || category.contains("biryani") || category.contains("bangladeshi")) return "/images/foods/spiced-rice-curry.webp";
        if (category.contains("burger")) return "/images/foods/classic-beef-burger.webp";
        if (category.contains("noodle") || category.contains("momo")) return "/images/foods/chicken-noodles.webp";
        if (category.contains("street")) return "/images/foods/chotpoti.webp";
        if (category.contains("chicken") || category.contains("grill")) return "/images/foods/grilled-chicken-platter.webp";
        return "/images/foods/fresh-grain-bowl.webp";
    }

    private void seedDiscountsIfMissing() {
        LocalDateTime now = LocalDateTime.now();
        createDiscount("WELCOME15", "Welcome Offer", "15% off orders of at least ৳300.",
                DiscountType.PERCENTAGE, "15", "300", "150", now.plusMonths(3));
        createDiscount("FMC100", "Food Mobo ৳100", "৳100 off orders of at least ৳700.",
                DiscountType.FIXED_AMOUNT, "100", "700", null, now.plusMonths(2));
    }

    private void createDiscount(String code, String name, String description, DiscountType type,
                                String value, String minimum, String maximum, LocalDateTime endsAt) {
        if (discountRepository.existsByCodeIgnoreCase(code)) {
            return;
        }
        discountRepository.save(Discount.builder()
                .code(code)
                .name(name)
                .description(description)
                .type(type)
                .value(new BigDecimal(value))
                .minimumOrder(new BigDecimal(minimum))
                .maximumDiscount(maximum == null ? null : new BigDecimal(maximum))
                .startsAt(LocalDateTime.now().minusDays(1))
                .endsAt(endsAt)
                .active(true)
                .build());
    }

    private static final String[] CATEGORY_DATA = {
            "Rice & Biryani|biryani-rice|/images/foods/beef-tehari.webp",
            "Burgers|burgers-sandwiches|/images/foods/classic-beef-burger.webp",
            "Noodles & Bowls|pizza-pasta|/images/foods/chicken-noodles.webp",
            "Bangladeshi Meals|bangladeshi|/images/foods/hilsa-curry.webp",
            "Street Food & Snacks|street-food-snacks|/images/foods/chotpoti.webp",
            "Chicken & Grill|chicken-grill|/images/foods/grilled-chicken-with-vegetables.webp",
            "Momo & Asian|noodles-momo|/images/foods/chicken-momo.webp",
            "Sweets|desserts-drinks|/images/foods/jilapi.webp"
    };

    private static final String[] CART_DATA = {
            "biryani@foodmobo.local|Rafi Hasan|01710000001|Dhaka Biryani House|dhaka-biryani-house|Traditional rice dishes, tehari, khichuri and hearty meal combinations prepared fresh every day.|Dhanmondi 27, Dhaka|Rice & Bangladeshi|/images/carts/dhaka-biryani-house.webp|60.00|35",
            "street@foodmobo.local|Nusrat Jahan|01710000002|Street Bite|street-bite|Classic Dhaka street food and quick snacks served hot and fresh.|Banani 11, Dhaka|Street Food|/images/carts/street-bite.webp|45.00|25",
            "burger@foodmobo.local|Arif Rahman|01710000003|Burger Station|burger-station|Loaded burgers with cheese, sauces and bold flavours made to order.|Mirpur 10, Dhaka|Burgers|/images/carts/burger-station.webp|55.00|30",
            "pizza@foodmobo.local|Samia Ahmed|01710000004|Pasta & Bowl Corner|pizza-corner|Noodles, grain bowls and comforting quick meals for every craving.|Uttara Sector 7, Dhaka|Noodles & Bowls|/images/carts/pizza-corner.webp|70.00|35",
            "deshi@foodmobo.local|Mahin Islam|01710000005|Deshi Kitchen|deshi-kitchen|Homestyle Bangladeshi fish, rice and curry plates.|Bashundhara R/A, Dhaka|Bangladeshi|/images/carts/deshi-kitchen.webp|50.00|35",
            "spice@foodmobo.local|Tanjim Karim|01710000006|Spice Route|spice-route|Grilled chicken, momo and spicy Asian-style favourites.|Mohakhali DOHS, Dhaka|Asian & Grill|/images/carts/spice-route.webp|65.00|35",
            "sweet@foodmobo.local|Maliha Noor|01710000007|Sweet Street|sweet-and-sip|Fresh jilapi and simple sweet bites for an after-meal treat.|Bailey Road, Dhaka|Sweets|/images/carts/sweet-and-sip.webp|40.00|20",
            "campus@foodmobo.local|Fahim Chowdhury|01710000008|Campus Cravings|campus-cravings|Affordable student favourites including burgers, noodles, rice bowls and grilled meals.|Tejgaon, Dhaka|Student Favorites|/images/carts/campus-cravings.webp|35.00|25"
    };

    private static final String[] FOOD_DATA = {
            "dhaka-biryani-house|biryani-rice|Beef Tehari|beef-tehari|Fragrant rice cooked with tender beef and classic tehari spices.|290.00|/images/foods/beef-tehari.webp|true|true",
            "dhaka-biryani-house|biryani-rice|Beef Rice|beef-rice|Simple beef and rice plate with a homestyle flavour.|260.00|/images/foods/beef-rice.webp|false|true",
            "dhaka-biryani-house|biryani-rice|Bhuna Khichuri|bhuna-khichuri|Rich spiced khichuri served as a comforting rice meal.|220.00|/images/foods/bhuna-khichuri.webp|true|true",
            "dhaka-biryani-house|biryani-rice|Biryani Plate|biryani-plate|A hearty biryani-style rice plate with chicken and aromatic spices.|280.00|/images/foods/biryani-plate.webp|true|true",
            "dhaka-biryani-house|biryani-rice|Biryani Combo|biryani-combo|A generous rice-and-chicken combination for a filling meal.|320.00|/images/foods/biryani-combo.webp|false|true",
            "dhaka-biryani-house|biryani-rice|Biryani Meal|biryani-meal|Classic rice meal with chicken served as a complete plate.|300.00|/images/foods/biryani-meal.webp|false|true",
            "dhaka-biryani-house|biryani-rice|Biryani & Drink Combo|biryani-drink-combo|Biryani-style rice served with a chilled drink for a complete combo.|350.00|/images/foods/biryani-drink-combo.webp|true|false",

            "street-bite|street-food-snacks|Spicy Aloo Chop|spicy-aloo-chop|Crispy potato chop served hot with a spicy street-food kick.|120.00|/images/foods/spicy-aloo-chop.webp|true|true",
            "street-bite|street-food-snacks|Chotpoti|chotpoti|Warm chickpeas and potatoes with classic tamarind-style seasoning.|130.00|/images/foods/chotpoti.webp|true|true",
            "street-bite|street-food-snacks|Aloo Chop|aloo-chop|Crispy potato snack served hot as a quick street-food bite.|70.00|/images/foods/aloo-chop.webp|false|true",
            "street-bite|street-food-snacks|Chickpea Chaat|chickpea-chaat|A warm chickpea snack with tangy and spicy seasoning.|110.00|/images/foods/chickpea-chaat.webp|false|true",

            "burger-station|burgers-sandwiches|Classic Beef Burger|classic-beef-burger|Juicy beef patty with cheese, lettuce, onion and burger sauce.|260.00|/images/foods/classic-beef-burger.webp|true|true",
            "burger-station|burgers-sandwiches|Cheese Burger|cheese-burger|Beef burger with melted cheese, salad and creamy sauce.|290.00|/images/foods/cheese-burger.webp|true|false",
            "burger-station|burgers-sandwiches|Spicy Beef Burger|spicy-beef-burger|Beef burger with a spicy house sauce and fresh toppings.|300.00|/images/foods/spicy-beef-burger.webp|true|true",
            "burger-station|burgers-sandwiches|Loaded Beef Burger|loaded-beef-burger|A tall loaded beef burger with cheese, vegetables and sauce.|320.00|/images/foods/loaded-beef-burger.webp|false|true",
            "burger-station|burgers-sandwiches|Double Cheese Burger|double-cheese-burger|A rich burger stacked with extra cheese and fresh toppings.|330.00|/images/foods/double-cheese-burger.webp|false|false",
            "burger-station|burgers-sandwiches|Signature Burger|signature-burger|The house signature burger with a juicy patty and creamy sauce.|310.00|/images/foods/signature-burger.webp|false|true",
            "burger-station|burgers-sandwiches|Classic Cheese Burger|classic-cheese-burger|Classic burger combination with cheese, lettuce and tomato.|280.00|/images/foods/classic-cheese-burger.webp|false|false",
            "burger-station|burgers-sandwiches|Smoky Beef Burger|smoky-beef-burger|A smoky beef burger with cheese and crisp fresh vegetables.|315.00|/images/foods/smoky-beef-burger.webp|false|true",

            "pizza-corner|pizza-pasta|Chicken Noodles|chicken-noodles|Noodle bowl with chicken, vegetables and a savoury broth-style finish.|240.00|/images/foods/chicken-noodles.webp|true|true",
            "pizza-corner|pizza-pasta|Thai Noodles|thai-noodles|Sweet-spicy noodles tossed with vegetables and sauce.|270.00|/images/foods/thai-noodles.webp|true|true",
            "pizza-corner|pizza-pasta|Spicy Noodle Bowl|spicy-noodle-bowl|Colourful spicy noodles finished with vegetables and herbs.|260.00|/images/foods/spicy-noodle-bowl.webp|true|true",
            "pizza-corner|pizza-pasta|Fresh Grain Bowl|fresh-grain-bowl|Fresh grain-and-vegetable bowl with a light, colourful topping.|230.00|/images/foods/fresh-grain-bowl.webp|false|false",
            "pizza-corner|pizza-pasta|Healthy Grain Bowl|healthy-grain-bowl|Balanced grain bowl with vegetables and fresh garnish.|240.00|/images/foods/healthy-grain-bowl.webp|false|false",
            "pizza-corner|pizza-pasta|Vegetable Rice Bowl|vegetable-rice-bowl|Vegetable rice bowl with a fresh mix of colourful sides.|210.00|/images/foods/vegetable-rice-bowl.webp|false|false",

            "deshi-kitchen|bangladeshi|Hilsa Curry|hilsa-curry|Traditional hilsa fish cooked in a rich Bangladeshi curry with green chilli.|420.00|/images/foods/hilsa-curry.webp|true|true",
            "deshi-kitchen|bangladeshi|Spiced Rice Curry|spiced-rice-curry|A warm spiced rice plate served with curry and vegetables.|220.00|/images/foods/spiced-rice-curry.webp|false|true",
            "deshi-kitchen|bangladeshi|Beef Rice Bowl|beef-rice-bowl|Rice served with beef pieces in a simple homestyle preparation.|280.00|/images/foods/beef-rice-bowl.webp|false|true",

            "spice-route|chicken-grill|Grilled Chicken with Vegetables|grilled-chicken-with-vegetables|Grilled chicken breast served with colourful vegetables.|320.00|/images/foods/grilled-chicken-with-vegetables.webp|true|true",
            "spice-route|chicken-grill|Grilled Chicken Platter|grilled-chicken-platter|Juicy grilled chicken served as a light platter with vegetables.|310.00|/images/foods/grilled-chicken-platter.webp|true|true",
            "spice-route|chicken-grill|Chicken Steak Platter|chicken-steak-platter|Grilled chicken steak with vegetables and a savoury finish.|340.00|/images/foods/chicken-steak-platter.webp|false|true",
            "spice-route|chicken-grill|Spicy Grilled Chicken|spicy-grilled-chicken|Grilled chicken with a spicy seasoning and vegetable side.|330.00|/images/foods/spicy-grilled-chicken.webp|true|true",
            "spice-route|chicken-grill|Herb Grilled Chicken|herb-grilled-chicken|Herb-seasoned grilled chicken served with vegetables.|325.00|/images/foods/herb-grilled-chicken.webp|false|false",
            "spice-route|chicken-grill|Chicken Grill Plate|chicken-grill-plate|A simple grilled chicken plate with a colourful vegetable side.|315.00|/images/foods/chicken-grill-plate.webp|false|true",
            "spice-route|noodles-momo|Chicken Momo|chicken-momo|Steamed chicken dumplings with a spicy dipping sauce.|220.00|/images/foods/chicken-momo.webp|true|true",

            "sweet-and-sip|desserts-drinks|Jilapi|jilapi|Crisp syrupy jilapi served fresh.|100.00|/images/foods/jilapi.webp|true|false",
            "sweet-and-sip|desserts-drinks|Mini Jilapi|mini-jilapi|Small spiral jilapi pieces with a sweet syrupy finish.|120.00|/images/foods/mini-jilapi.webp|false|false",

            "campus-cravings|burgers-sandwiches|House Special Burger|house-special-burger|A loaded house burger with cheese, salad and sauce.|240.00|/images/foods/house-special-burger.webp|true|true",
            "campus-cravings|pizza-pasta|Stir-Fried Noodles|stir-fried-noodles|Quick stir-fried noodles with vegetables and savoury sauce.|190.00|/images/foods/stir-fried-noodles.webp|true|true",
            "campus-cravings|pizza-pasta|Fresh Rice Bowl|fresh-rice-bowl|Fresh rice-and-vegetable bowl for a light student meal.|180.00|/images/foods/fresh-rice-bowl.webp|false|false",
            "campus-cravings|chicken-grill|Grilled Chicken Salad|grilled-chicken-salad|Grilled chicken served with a colourful vegetable salad.|250.00|/images/foods/grilled-chicken-salad.webp|false|false",
            "campus-cravings|burgers-sandwiches|Grill House Burger|grill-house-burger|A filling burger with cheese, vegetables and signature sauce.|230.00|/images/foods/grill-house-burger.webp|false|true"    };

}