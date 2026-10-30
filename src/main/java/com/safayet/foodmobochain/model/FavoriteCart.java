package com.safayet.foodmobochain.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "favorite_carts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorite_cart", columnNames = {"user_id", "food_cart_id"})
})
public class FavoriteCart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_cart_id", nullable = false)
    private FoodCart foodCart;
}
