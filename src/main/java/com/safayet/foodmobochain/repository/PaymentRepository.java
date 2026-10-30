package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.CustomerOrder;
import com.safayet.foodmobochain.model.Payment;
import com.safayet.foodmobochain.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @EntityGraph(attributePaths = {"order", "order.buyer", "order.foodCart"})
    Optional<Payment> findByOrder(CustomerOrder order);
    Optional<Payment> findByTransactionId(String transactionId);

    @EntityGraph(attributePaths = {"order", "order.buyer", "order.foodCart"})
    List<Payment> findTop50ByOrderByCreatedAtDesc();

    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = com.safayet.foodmobochain.model.enums.PaymentStatus.PAID")
    BigDecimal sumPaidAmount();
}
