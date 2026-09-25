package com.project.taxratesystem.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.project.taxratesystem.user.entity.Address;
import com.project.taxratesystem.user.entity.NotificationSettings;
import com.project.taxratesystem.user.entity.User;

import java.time.Instant;
import java.time.LocalDate;

/**
 * The profile object of API.md §7.1, returned by {@code GET/PUT /users/me} and by
 * {@code PUT /users/me/address}.
 *
 * <p>{@code address} is omitted entirely when the user has never stored one; the optional
 * phone/gender/birth-date fields are present as {@code null} instead - exactly the field table of
 * §7.1. {@code notificationSettings} is always present (the §11 defaults when no row exists).
 */
public class UserResponse {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String status;

    /** Present only when the user has stored an address (§7.1). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AddressView address;

    private NotificationSettingsResponse notificationSettings;
    private Instant createdAt;

    /** Maps a user (and its optional address and settings) to the §7.1 wire shape. */
    public static UserResponse of(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.firstName = user.getFirstName();
        response.lastName = user.getLastName();
        response.email = user.getEmail();
        response.phoneNumber = user.getPhoneNumber();
        response.gender = user.getGender() == null ? null : user.getGender().name();
        response.dateOfBirth = user.getDateOfBirth();
        response.status = user.getStatus() == null ? null : user.getStatus().name();
        response.address = user.getAddress() == null ? null : AddressView.of(user.getAddress());
        NotificationSettings settings = user.getNotificationSettings();
        response.notificationSettings = NotificationSettingsResponse.of(
                settings != null ? settings : NotificationSettings.defaults());
        response.createdAt = user.getCreatedAt();
        return response;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public AddressView getAddress() {
        return address;
    }

    public void setAddress(AddressView address) {
        this.address = address;
    }

    public NotificationSettingsResponse getNotificationSettings() {
        return notificationSettings;
    }

    public void setNotificationSettings(NotificationSettingsResponse notificationSettings) {
        this.notificationSettings = notificationSettings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /** The address object of §7.1; the two optional lines are sent as {@code null} when unset. */
    public static class AddressView {

        private String street;
        private String barangay;
        private String city;
        private String province;
        private String postalCode;

        public static AddressView of(Address address) {
            AddressView view = new AddressView();
            view.street = address.getStreet();
            view.barangay = address.getBarangay();
            view.city = address.getCity();
            view.province = address.getProvince();
            view.postalCode = address.getPostalCode();
            return view;
        }

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
}

