package com.project.taxratesystem.auth.repository;

import com.project.taxratesystem.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    /** Lookup is by hash - the raw reset token is never stored. */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
