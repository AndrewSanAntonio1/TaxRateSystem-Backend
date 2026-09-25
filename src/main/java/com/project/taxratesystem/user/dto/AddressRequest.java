package com.project.taxratesystem.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * The single address of a user (API.md §7.3, §13.3), used by {@code PUT /users/me/address} and
 * optionally nested inside {@code POST /auth/register} (§6.1). The address is replaced wholesale,
 * never patched field by field.
 */

public class AddressRequest {

    @NotBlank(message = "Street is required.")
    @Size(min = 2, max = 160, message = "Street must be between 2 and 160 characters.")
    private String street;

    /** Optional barangay (§13.3). */
    @Size(min = 2, max = 80, message = "Barangay must be between 2 and 80 characters.")
    private String barangay;

    @NotBlank(message = "City is required.")
    @Size(min = 2, max = 80, message = "City must be between 2 and 80 characters.")
    private String city;

    @NotBlank(message = "Province is required.")
    @Size(min = 2, max = 80, message = "Province must be between 2 and 80 characters.")
    private String province;

    /** PH postal code: exactly four digits (§13.3). */
    @Pattern(regexp = "^\\d{4}$", message = "Postal code must be exactly 4 digits.")
    private String postalCode;

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}
