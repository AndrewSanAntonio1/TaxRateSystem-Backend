package com.project.taxratesystem.auth.repository;

import com.project.taxratesystem.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    /** Lookup is by hash - the raw token is never stored. */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByFamilyId(String familyId);

    /** Revokes every live token of a user ({@code allDevices} logout, password reset/change). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :when "
            + "where token.user.id = :userId and token.revokedAt is null")
    int revokeAllForUser(@Param("userId") Integer userId, @Param("when") Instant when);

    /** Revokes every live token of a user except one - the session performing the change. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :when "
            + "where token.user.id = :userId and token.id <> :keepTokenId and token.revokedAt is null")
    int revokeAllForUserExcept(@Param("userId") Integer userId,
                               @Param("keepTokenId") Integer keepTokenId,
                               @Param("when") Instant when);

    /** Reuse detection: presenting a rotated token kills the whole rotation family (API.md §6.5). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken token set token.revokedAt = :when "
            + "where token.familyId = :familyId and token.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("when") Instant when);
}

