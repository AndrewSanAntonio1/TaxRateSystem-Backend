package com.project.taxratesystem.user.dto;

/**
 * Body of {@code PUT /users/me} (API.md §7.2, §13.4).
 *
 * <p>Every field is optional and the semantics are "absent = unchanged, explicit {@code null} =
 * clear". Jackson only calls a setter for a field that is present in the JSON, so each setter also
 * flips a presence flag - that is how {@code UserService} tells "not sent" apart from "sent as
 * null" without adding a dependency. The §13.4 rules (2-60 character names, PH phone format,
 * gender vocabulary, past birth date with 18+ age) are enforced there, after trimming, so
 * whitespace-only values are rejected too.
 */
public class UpdateProfileRequest {

    private String firstName;
    private boolean firstNamePresent;

    private String lastName;
    private boolean lastNamePresent;

    /** PH mobile, 11 digits starting {@code 09}; {@code null} clears it (§7.2). */
    private String phoneNumber;
    private boolean phoneNumberPresent;

    private String gender;
    private boolean genderPresent;

    /** ISO date ({@code YYYY-MM-DD}); {@code null} clears it (§7.2). */
    private String dateOfBirth;
    private boolean dateOfBirthPresent;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        this.firstNamePresent = true;
    }

    public boolean isFirstNamePresent() {
        return firstNamePresent;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        this.lastNamePresent = true;
    }

    public boolean isLastNamePresent() {
        return lastNamePresent;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.phoneNumberPresent = true;
    }

    public boolean isPhoneNumberPresent() {
        return phoneNumberPresent;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
        this.genderPresent = true;
    }

    public boolean isGenderPresent() {
        return genderPresent;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        this.dateOfBirthPresent = true;
    }

    public boolean isDateOfBirthPresent() {
        return dateOfBirthPresent;
    }
}

