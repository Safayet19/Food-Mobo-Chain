package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.FoodCart;
import com.safayet.foodmobochain.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FoodCartRepository extends JpaRepository<FoodCart, Long> {
    @EntityGraph(attributePaths = "owner")
    Optional<FoodCart> findBySlug(String slug);

    Optional<FoodCart> findByOwner(User owner);
    boolean existsBySlug(String slug);

    @EntityGraph(attributePaths = "owner")
    @Query("select c from FoodCart c where c.approved = true and c.owner.enabled = true order by c.name asc")
    List<FoodCart> findPublicApproved();

    @EntityGraph(attributePaths = "owner")
    List<FoodCart> findAllByOrderByCreatedAtDesc();

    long countByApprovedTrue();
    long countByApprovedFalse();
}
