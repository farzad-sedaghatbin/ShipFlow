package com.github.farzadsedaghatbin.shipflow.service.email;

import com.github.farzadsedaghatbin.shipflow.service.IEmailNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * No-op stub that is registered when SMTP is not configured.
 * All methods are no-ops so the application can start without mail settings.
 */
@Service
@ConditionalOnMissingBean(IEmailNotificationService.class)
@Slf4j
public class NoOpEmailNotificationService implements IEmailNotificationService {

    public NoOpEmailNotificationService() {
        log.info("Email notifications disabled — SMTP host not configured");
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void sendTaskAssigned(
            String toEmail, String recipientName, String taskTitle, String taskUrl) {
        // no-op
    }

    @Override
    public void sendMentionedInComment(
            String toEmail, String recipientName, String entityTitle, String actionUrl) {
        // no-op
    }

    @Override
    public void sendPitchStatusChanged(
            String toEmail,
            String recipientName,
            String pitchTitle,
            String newStatus,
            String actionUrl) {
        // no-op
    }
}
