package com.safayet.foodmobochain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "food_carts", indexes = {
        @Index(name = "idx_food_cart_approved", columnList = "approved"),
        @Index(name = "idx_food_cart_open", columnList = "open_flag")
})
public class FoodCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 180)
    private String location;

    @Column(nullable = false, length = 100)
    private String cuisine;

    @Column(nullable = false, length = 220)
    private String coverImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false)
    private int estimatedDeliveryMinutes;

    @Column(name = "open_flag", nullable = false)
    @Builder.Default
    private boolean open = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean approved = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
