package com.emirio.auth.passwordreset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findTopByEmailOrderByCreatedAtDesc(String email);

    long deleteByEmail(String email);

    long deleteByExpiresAtBefore(Instant now);
}
