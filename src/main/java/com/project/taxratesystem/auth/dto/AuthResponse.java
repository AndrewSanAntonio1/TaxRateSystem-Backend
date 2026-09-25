package com.project.taxratesystem.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The token pair returned by {@code POST /auth/login} and {@code POST /auth/refresh}
 * (API.md §6.4, §6.5).
 *
 * <p>The raw refresh token is shown to the client exactly once per rotation and never logged;
 * only its SHA-256 hash lives in the database.
 */
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    /** Minimal public profile shown inside the auth response (never the password hash). */
    private UserSummary user;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserSummary user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    /** The four fields the client stores for the signed-in session (§6.4). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSummary {
        private Integer id;
        private String firstName;
        private String lastName;
        private String email;

        public UserSummary() {
        }

        public UserSummary(Integer id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
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
    }
}
