package com.project.taxratesystem.security;

import com.project.taxratesystem.user.entity.User;
import com.project.taxratesystem.user.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated principal of every protected request.
 *
 * <p>There is no role model in this system (API.md §16): every authenticated user is a plain user
 * with no authorities, and access is scoped by ownership, not by role. Only the fields needed to
 * authenticate and authorise are exposed - never the whole entity.
 */
public record AuthenticatedUser(Integer id, String email, String passwordHash, UserStatus status,
                                String sessionId)
        implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.getStatus(), null);
    }

    /**
     * Returns a copy that carries the session (refresh rotation family) of the presented bearer
     * token - the {@code sid} claim of API.md §3. {@code PUT /users/me/password} (§7.4) uses it to
     * tell the caller's session apart from every other one.
     */
    public AuthenticatedUser withSessionId(String sessionId) {
        return new AuthenticatedUser(id, email, passwordHash, status, sessionId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED && status != UserStatus.DEACTIVATED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
