package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.FoodItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    @EntityGraph(attributePaths = {"foodCart", "category"})
    @Query("""
        select f from FoodItem f
        where f.available = true
          and f.archived = false
          and f.category.active = true
          and f.foodCart.approved = true
          and f.foodCart.open = true
          and f.foodCart.owner.enabled = true
          and (:q is null or lower(f.name) like lower(concat('%', :q, '%'))
               or lower(f.description) like lower(concat('%', :q, '%'))
               or lower(f.foodCart.name) like lower(concat('%', :q, '%')))
          and (:category is null or f.category.slug = :category)
          and (:cartId is null or f.foodCart.id = :cartId)
          and (:minPrice is null or f.price >= :minPrice)
          and (:maxPrice is null or f.price <= :maxPrice)
        """)
    Page<FoodItem> searchPublic(@Param("q") String q,
                                @Param("category") String category,
                                @Param("cartId") Long cartId,
                                @Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"foodCart", "foodCart.owner", "category"})
    @Query("select f from FoodItem f where f.id = :id")
    Optional<FoodItem> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"foodCart", "foodCart.owner", "category"})
    @Query("""
        select f from FoodItem f
        where f.featured = true
          and f.available = true
          and f.archived = false
          and f.category.active = true
          and f.foodCart.approved = true
          and f.foodCart.open = true
          and f.foodCart.owner.enabled = true
        order by f.createdAt desc
        """)
    List<FoodItem> findFeaturedPublic(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    List<FoodItem> findByFoodCartAndArchivedFalseOrderByNameAsc(FoodCart foodCart);

    @EntityGraph(attributePaths = {"foodCart", "category"})
    List<FoodItem> findAllByOrderByNameAsc();

    long countByFoodCartAndArchivedFalse(FoodCart foodCart);
    long countByAvailableTrue();
}
