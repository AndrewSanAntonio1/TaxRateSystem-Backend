package com.project.taxratesystem.user.repository;

import com.project.taxratesystem.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /** Login and duplicate checks are case-insensitive: e-mails are stored lower-cased. */
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Phone numbers are unique across accounts (API.md §7.2 {@code 409}). */
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumber(String phoneNumber);
}

