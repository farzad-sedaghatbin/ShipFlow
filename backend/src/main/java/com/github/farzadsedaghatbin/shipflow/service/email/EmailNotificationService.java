package com.github.farzadsedaghatbin.shipflow.service.email;

import com.github.farzadsedaghatbin.shipflow.service.IEmailNotificationService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Real SMTP-backed implementation of email notifications.
 * Only registered when {@code spring.mail.host} is configured.
 *
 * <p>Sending is {@link Async}: the caller (assigning a task, posting a comment) never waits on the
 * mail server, and a slow or unreachable relay cannot hold up the request that triggered the mail.
 * Failures are logged, never thrown.
 *
 * <p>Every message is marked as machine-generated ({@code Auto-Submitted: auto-generated},
 * {@code Precedence: bulk}) so out-of-office replies and mail clients do not answer a sender
 * mailbox nobody reads. {@code app.mail.from} may be a bare address or {@code "Name <address>"};
 * {@code app.mail.reply-to}, when set, is added as Reply-To.
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

    @Value("${app.mail.reply-to:}")
    private String replyTo;

    @Value("${app.mail.base-url:https://shipflow.dev}")
    private String appBaseUrl;

    @Override
    public boolean isEnabled() {
        String host = environment.getProperty("spring.mail.host", "");
        return !host.isBlank();
    }

    @Override
    @Async
    public void sendTaskAssigned(
            String toEmail, String recipientName, String taskTitle, String taskUrl) {
        if (!isEnabled()) {
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("entityTitle", taskTitle);
            ctx.setVariable("actionUrl", absoluteUrl(taskUrl));
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/task-assigned", ctx);
            send(toEmail, "New Task Assigned: " + taskTitle, html);
        } catch (Exception e) {
            log.warn("Failed to send task-assigned email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendMentionedInComment(
            String toEmail, String recipientName, String entityTitle, String actionUrl) {
        if (!isEnabled()) {
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("entityTitle", entityTitle);
            ctx.setVariable("actionUrl", absoluteUrl(actionUrl));
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/mentioned-in-comment", ctx);
            send(toEmail, "You were mentioned: " + entityTitle, html);
        } catch (Exception e) {
            log.warn("Failed to send mention email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
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
            ctx.setVariable("actionUrl", absoluteUrl(actionUrl));
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/pitch-status-changed", ctx);
            send(toEmail, "Pitch Status Changed: " + pitchTitle, html);
        } catch (Exception e) {
            log.warn("Failed to send pitch-status email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendAutomationEmail(
            String toEmail, String recipientName, String subject, String message, String actionUrl) {
        if (!isEnabled()) {
            return;
        }
        try {
            String cleanSubject = oneLine(subject);
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("subject", cleanSubject);
            ctx.setVariable("message", message == null ? "" : message);
            ctx.setVariable("actionUrl", absoluteUrl(actionUrl));
            ctx.setVariable("appBaseUrl", appBaseUrl);
            String html = templateEngine.process("email/automation-triggered", ctx);
            send(toEmail, cleanSubject, html);
        } catch (Exception e) {
            log.warn("Failed to send automation email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public String sendTest(String toEmail, String recipientName) {
        if (!isEnabled()) {
            return "Email is not configured on this server (SMTP_HOST is not set).";
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("recipientName", recipientName);
            ctx.setVariable("subject", "ShipFlow test email");
            ctx.setVariable("message",
                    "If you can read this, ShipFlow can send email from " + appBaseUrl + ".");
            ctx.setVariable("actionUrl", absoluteUrl("/dashboard"));
            ctx.setVariable("appBaseUrl", appBaseUrl);
            send(toEmail, "ShipFlow test email", templateEngine.process("email/automation-triggered", ctx));
            return null;
        } catch (Exception e) {
            log.warn("Test email to {} failed: {}", toEmail, e.getMessage());
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return oneLine(reason);
        }
    }

    private void send(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        // InternetAddress understands both "a@b.c" and "Display Name <a@b.c>".
        helper.setFrom(new InternetAddress(fromAddress.trim()));
        if (replyTo != null && !replyTo.isBlank()) {
            helper.setReplyTo(new InternetAddress(replyTo.trim()));
        }
        helper.setTo(to);
        helper.setSubject(oneLine(subject));
        helper.setText(htmlBody, true);
        msg.addHeader("Auto-Submitted", "auto-generated");
        msg.addHeader("Precedence", "bulk");
        mailSender.send(msg);
        log.debug("Email sent to {} — subject: {}", to, subject);
    }

    /** In-app paths ("/backlog/12") become full links; an email has no "current site" to resolve them against. */
    public String absoluteUrl(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return null;
        }
        if (actionUrl.startsWith("/")) {
            String base = appBaseUrl == null ? "" : appBaseUrl.trim();
            while (base.endsWith("/")) {
                base = base.substring(0, base.length() - 1);
            }
            return base + actionUrl;
        }
        return actionUrl;
    }

    /** Subjects are single-line; a line break would otherwise be a header-injection vector. */
    static String oneLine(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\r\\n]+", " ").trim();
    }
}
