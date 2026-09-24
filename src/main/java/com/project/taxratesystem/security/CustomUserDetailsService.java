package com.project.taxratesystem.security;

import com.project.taxratesystem.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a bearer token's {@code sub} (the e-mail address) to the principal of the request
 * (API.md §3, §16). Unknown or deleted accounts raise the standard Spring Security exception, which
 * the JWT filter treats as "not authenticated" - the caller sees {@code 401}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(username)
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account for that e-mail address."));
    }
}

