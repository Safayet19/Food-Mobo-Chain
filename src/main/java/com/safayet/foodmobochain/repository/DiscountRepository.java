package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    Optional<Discount> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Discount> findByActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqualOrderByEndsAtAsc(LocalDateTime now1, LocalDateTime now2);
    List<Discount> findAllByOrderByEndsAtDesc();
}
