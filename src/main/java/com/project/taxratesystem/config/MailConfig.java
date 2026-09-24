package com.project.taxratesystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Mail delivery settings that are not covered by {@code spring.mail.*} (API.md §6, §19).
 *
 * <p>{@code app.mail.enabled=false} switches the application into local development mode: e-mails
 * - including OTP codes - are written to the log instead of being sent, so the whole registration
 * and password-reset flow can be exercised without SMTP credentials. It must never be disabled in
 * production.
 */
@Configuration
public class MailConfig {

    private final boolean enabled;
    private final String from;

    public MailConfig(@Value("${app.mail.enabled:true}") boolean enabled,
                      @Value("${app.mail.from:}") String from) {
        this.enabled = enabled;
        this.from = from;
    }

    /** Whether e-mails are actually handed to SMTP (or only logged in dev mode). */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * The From address for outgoing mail; blank means "use {@code spring.mail.username}", which
     * Gmail requires to match the authenticated account.
     */
    public String getFrom() {
        return from;
    }
}

