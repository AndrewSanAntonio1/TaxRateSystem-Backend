package com.project.taxratesystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates a request from its {@code Authorization: Bearer <JWT>} header (API.md §3, §16).
 *
 * <p>A missing, malformed, expired or otherwise invalid token simply leaves the request
 * unauthenticated - the authorisation rules in {@link SecurityConfig} then produce the {@code 401}.
 * Account status is deliberately not checked here: suspended or unverified accounts are rejected
 * per operation with a {@code 403} so the client gets the specific message the contract defines.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null
                && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            jwtService.parseAccessToken(token).ifPresent(payload -> authenticate(payload, request));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(JwtService.JwtPayload payload, HttpServletRequest request) {
        try {
            UserDetails principal = userDetailsService.loadUserByUsername(payload.subject());
            if (principal instanceof AuthenticatedUser authenticated) {
                // The bearer token names the session (refresh rotation family) that obtained it;
                // §7.4 uses it to keep the caller signed in through a password change.
                principal = authenticated.withSessionId(payload.sessionId());
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException ex) {
            // The account behind the token is gone: treat the token as invalid.
            SecurityContextHolder.clearContext();
        }
    }
}

