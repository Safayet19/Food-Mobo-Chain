package com.safayet.foodmobochain.repository;

import com.safayet.foodmobochain.model.PasswordResetToken;
import com.safayet.foodmobochain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);
    void deleteByUser(User user);
}
