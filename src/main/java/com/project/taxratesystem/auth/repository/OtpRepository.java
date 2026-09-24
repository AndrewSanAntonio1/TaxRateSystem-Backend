package com.project.taxratesystem.auth.repository;

import com.project.taxratesystem.auth.entity.OtpCode;
import com.project.taxratesystem.auth.enums.OtpPurpose;
import com.project.taxratesystem.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpCode, Integer> {

    /** The newest unused code for a user and purpose - the only candidate a verify call may use. */
    Optional<OtpCode> findFirstByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(User user, OtpPurpose purpose);

    /** The newest code of any state - used for the resend cooldown (API.md §6.3). */
    Optional<OtpCode> findFirstByUserAndPurposeOrderByCreatedAtDesc(User user, OtpPurpose purpose);

    /** Invalidates every unused code before a new one is issued, so only one code is live. */
    void deleteAllByUserAndPurposeAndUsedAtIsNull(User user, OtpPurpose purpose);
}

