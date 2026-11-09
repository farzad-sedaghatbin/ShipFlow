package com.github.farzadsedaghatbin.shipflow.dto.admin;

/**
 * What the server is actually configured to do with email — read from its environment, never the
 * password. {@code configured} is false when {@code SMTP_HOST} is not set.
 */
public record EmailStatusDTO(boolean configured, String host, Integer port, String username, String from,
    String replyTo, String baseUrl, boolean starttlsRequired) {
}
