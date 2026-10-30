package com.safayet.foodmobochain.model;

import com.safayet.foodmobochain.model.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private CustomerOrder order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.WAITING;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    @Column(nullable = false)
    private int estimatedMinutes;

    private LocalDateTime dispatchedAt;
    private LocalDateTime deliveredAt;
}
