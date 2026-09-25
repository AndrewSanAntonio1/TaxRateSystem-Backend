package com.project.taxratesystem.auth.dto;

import com.project.taxratesystem.user.dto.AddressRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /auth/register} (API.md §6.1).
 *
 * <p>The optional nested {@link AddressRequest} may be supplied at signup or later via
 * {@code PUT /users/me/address} (§7.3); it is never required here.
 */
public class RegisterRequest {

    @NotBlank(message = "First name is required.")
    @Size(min = 2, max = 60, message = "First name must be between 2 and 60 characters.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(min = 2, max = 60, message = "Last name must be between 2 and 60 characters.")
    private String lastName;

    @Email(message = "Invalid e-mail address.")
    @NotBlank(message = "E-mail is required.")
    private String email;

    @NotBlank(message = "Password is required.")
    private String password;

    /** PH mobile, 11 digits starting {@code 09}; optional (§13.1). */
    @Pattern(regexp = "^09\\d{9}$", message = "Phone number must be 11 digits starting with 09.")
    private String phoneNumber;

    /** Optional address; may be provided at signup or later. */
    private AddressRequest address;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public AddressRequest getAddress() {
        return address;
    }

    public void setAddress(AddressRequest address) {
        this.address = address;
    }
}
