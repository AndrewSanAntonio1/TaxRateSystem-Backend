package com.project.taxratesystem.notification;

import com.project.taxratesystem.config.MailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Single entry point for outgoing e-mail (API.md §6).
 *
 * <p>When {@code app.mail.enabled=false} the message is written to the log instead of being sent,
 * which keeps the registration and password-reset flows usable without SMTP credentials. Real
 * delivery failures propagate as a {@link org.springframework.mail.MailException}, which the
 * global handler turns into the {@code 500} the contract requires.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String FALLBACK_FROM = "no-reply@taxratesystem.local";

    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        MailConfig mailConfig,
                        @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.mailConfig = mailConfig;
        this.fromAddress = resolveFromAddress(mailConfig.getFrom(), mailUsername);
    }

    /** Sends a plain-text message, or logs it when mail is disabled. */
    public void sendPlainText(String to, String subject, String body) {
        if (!mailConfig.isEnabled()) {
            log.warn("DEV MAIL (app.mail.enabled=false) -> to={} | subject={} | body={}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.debug("Sent '{}' e-mail to {}", subject, to);
    }

    private static String resolveFromAddress(String configured, String mailUsername) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername;
        }
        return FALLBACK_FROM;
    }
}

