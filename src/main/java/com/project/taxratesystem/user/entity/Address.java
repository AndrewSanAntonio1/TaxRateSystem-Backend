package com.project.taxratesystem.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * The single optional address of a user (API.md §7.3): {@code PUT /users/me/address} replaces it
 * wholesale rather than patching individual fields.
 */
@Entity
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "street", nullable = false, length = 160)
    private String street;

    @Column(name = "barangay", length = 80)
    private String barangay;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "province", nullable = false, length = 80)
    private String province;

    @Column(name = "postal_code", length = 4)
    private String postalCode;
}

