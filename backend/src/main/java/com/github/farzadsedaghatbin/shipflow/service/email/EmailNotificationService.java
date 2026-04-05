package com.github.farzadsedaghatbin.shipflow.service.email;

import com.github.farzadsedaghatbin.shipflow.service.IEmailNotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Real SMTP-backed implementation of email notifications.
 * Only registered when {@code spring.mail.host} is configured.
 */
@Service
@ConditionalOnProperty(name = "spring.mail.host", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService implements IEmailNotificationService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final Environment environment;

    @Value("${app.mail.from:noreply@shipflow.dev}")
    private String fromAddress;

    @Value("${app.mail.base-url:https://shipflow.dev}")
    private String appBaseUrl;

    @Override
    public boolean isEnabled() {
        String host = environment.getProperty("spring.mail.host", "");
        return !host.isBlank();
    }

    @Override
    public void sendTaskAssigned(
            String toEmail, String recipientName, String taskTitle, String taskUrl) {
        if (!isEnabled()) {
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("entityTitle", taskTitle);
            ctx.setVariable("actionUrl", taskUrl);
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/task-assigned", ctx);
            send(toEmail, "New Task Assigned: " + taskTitle, html);
        } catch (Exception e) {
            log.warn("Failed to send task-assigned email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendMentionedInComment(
            String toEmail, String recipientName, String entityTitle, String actionUrl) {
        if (!isEnabled()) {
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("entityTitle", entityTitle);
            ctx.setVariable("actionUrl", actionUrl);
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/mentioned-in-comment", ctx);
            send(toEmail, "You were mentioned: " + entityTitle, html);
        } catch (Exception e) {
            log.warn("Failed to send mention email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendPitchStatusChanged(
            String toEmail,
            String recipientName,
            String pitchTitle,
            String newStatus,
            String actionUrl) {
        if (!isEnabled()) {
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("entityTitle", pitchTitle);
            ctx.setVariable("newStatus", newStatus);
            ctx.setVariable("actionUrl", actionUrl);
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/pitch-status-changed", ctx);
            send(toEmail, "Pitch Status Changed: " + pitchTitle, html);
        } catch (Exception e) {
            log.warn("Failed to send pitch-status email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void send(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(msg);
        log.debug("Email sent to {} — subject: {}", to, subject);
    }
}
