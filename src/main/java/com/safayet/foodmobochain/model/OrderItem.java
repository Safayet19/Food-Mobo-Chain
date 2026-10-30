package com.safayet.foodmobochain.model;

import com.safayet.foodmobochain.model.enums.SpiceLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(nullable = false)
    private Long foodItemIdSnapshot;

    @Column(nullable = false, length = 120)
    private String foodName;

    @Column(nullable = false, length = 220)
    private String foodImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpiceLevel spiceLevel;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
