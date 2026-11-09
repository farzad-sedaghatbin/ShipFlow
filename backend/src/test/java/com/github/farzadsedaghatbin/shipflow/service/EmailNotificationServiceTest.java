package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.service.email.EmailNotificationService;
import com.github.farzadsedaghatbin.shipflow.service.email.NoOpEmailNotificationService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for email notification service implementations.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private Environment environment;

    private EmailNotificationService emailService;
    private NoOpEmailNotificationService noOpService;

    /** Real MimeMessage backed by a no-op session (required for MimeMessageHelper). */
    private MimeMessage realMimeMessage;

    @BeforeEach
    void setUp() {
        emailService = new EmailNotificationService(mailSender, templateEngine, environment);
        // Inject @Value fields that Spring would normally bind
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@shipflow.dev");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "https://shipflow.dev");
        noOpService = new NoOpEmailNotificationService();
        realMimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    }

    // -----------------------------------------------------------------
    // isEnabled tests
    // -----------------------------------------------------------------

    @Test
    void isEnabled_returnsFalse_whenSmtpHostBlank() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("");
        assertThat(emailService.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_returnsTrue_whenSmtpHostConfigured() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        assertThat(emailService.isEnabled()).isTrue();
    }

    // -----------------------------------------------------------------
    // No-op stub
    // -----------------------------------------------------------------

    @Test
    void noOpService_isEnabled_returnsFalse() {
        assertThat(noOpService.isEnabled()).isFalse();
    }

    @Test
    void sendTaskAssigned_isNoOp_whenNotEnabled() {
        // NoOpEmailNotificationService — should never call anything
        noOpService.sendTaskAssigned("user@example.com", "Alice", "My Task", "http://localhost/tasks/1");
        verifyNoInteractions(mailSender, templateEngine);
    }

    // -----------------------------------------------------------------
    // Real implementation — sends mail when enabled
    // -----------------------------------------------------------------

    @Test
    void sendTaskAssigned_callsMailSender_whenEnabled() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendTaskAssigned(
                "alice@example.com", "Alice", "Fix the bug", "http://localhost/tasks/42");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendMentionedInComment_callsMailSender_whenEnabled() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendMentionedInComment(
                "bob@example.com", "Bob", "Sprint planning", "http://localhost/pitches/1");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendPitchStatusChanged_callsMailSender_whenEnabled() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendPitchStatusChanged(
                "carol@example.com", "Carol", "New Auth Module", "BETTING", "http://localhost/pitches/5");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendTaskAssigned_doesNotThrow_whenMailSenderFails() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("SMTP error"));

        // Should not propagate the exception
        emailService.sendTaskAssigned("fail@example.com", "Dave", "Task", "http://localhost/tasks/99");
        // No exception thrown — test passes
    }

    // -----------------------------------------------------------------
    // Sender identity, machine-generated headers, links
    // -----------------------------------------------------------------

    @Test
    void sentMail_carriesFromWithDisplayName_andAutoGeneratedHeaders() throws Exception {
        ReflectionTestUtils.setField(emailService, "fromAddress", "GoldOrigins ShipFlow <shipflow@goldorigins.com>");
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendTaskAssigned("alice@example.com", "Alice", "Fix the bug", "/backlog/42");

        realMimeMessage.saveChanges();
        assertThat(realMimeMessage.getHeader("From", null)).contains("GoldOrigins ShipFlow <shipflow@goldorigins.com>");
        assertThat(realMimeMessage.getFrom()[0].toString()).contains("shipflow@goldorigins.com");
        assertThat(realMimeMessage.getHeader("Auto-Submitted", null)).isEqualTo("auto-generated");
        assertThat(realMimeMessage.getHeader("Precedence", null)).isEqualTo("bulk");
        assertThat(realMimeMessage.getHeader("Reply-To")).isNull();
    }

    @Test
    void sentMail_addsReplyTo_whenConfigured() throws Exception {
        ReflectionTestUtils.setField(emailService, "replyTo", "support@goldorigins.com");
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>body</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendMentionedInComment("bob@example.com", "Bob", "task", "/backlog/1");

        assertThat(realMimeMessage.getReplyTo()[0].toString()).isEqualTo("support@goldorigins.com");
    }

    @Test
    void inAppPaths_becomeFullLinks_inTheTemplateModel() {
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "https://shipflow.example.org/");
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);
        org.mockito.ArgumentCaptor<Context> ctx = org.mockito.ArgumentCaptor.forClass(Context.class);
        when(templateEngine.process(anyString(), ctx.capture())).thenReturn("<html>body</html>");

        emailService.sendTaskAssigned("alice@example.com", "Alice", "Fix the bug", "/backlog/42");

        assertThat(ctx.getValue().getVariable("actionUrl")).isEqualTo("https://shipflow.example.org/backlog/42");
        assertThat(emailService.absoluteUrl("https://elsewhere.test/x")).isEqualTo("https://elsewhere.test/x");
        assertThat(emailService.absoluteUrl(null)).isNull();
    }

    @Test
    void automationEmail_sendsSubjectOnOneLine() throws Exception {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(eq("email/automation-triggered"), any(Context.class))).thenReturn("<html>x</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        emailService.sendAutomationEmail("alice@example.com", "Alice", "Cycle done\r\nBcc: evil@example.com", "Hi", "/cycles/3");

        verify(mailSender).send(any(MimeMessage.class));
        assertThat(realMimeMessage.getSubject()).isEqualTo("Cycle done Bcc: evil@example.com");
        assertThat(realMimeMessage.getHeader("Bcc")).isNull();
    }

    @Test
    void automationEmail_isNoOp_whenSmtpNotConfigured() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("");
        emailService.sendAutomationEmail("alice@example.com", "Alice", "s", "m", null);
        verifyNoInteractions(mailSender, templateEngine);
    }

    // -----------------------------------------------------------------
    // Test send reports the real outcome
    // -----------------------------------------------------------------

    @Test
    void sendTest_returnsNull_whenTheMailServerAcceptsTheMessage() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>x</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);

        assertThat(emailService.sendTest("farzad@goldorigins.com", "Farzad")).isNull();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendTest_returnsTheRealReason_whenSendingFails() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("smtp.example.com");
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>x</html>");
        when(mailSender.createMimeMessage()).thenReturn(realMimeMessage);
        doThrow(new org.springframework.mail.MailSendException("535 Authentication failed\r\nretry later"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThat(emailService.sendTest("farzad@goldorigins.com", "Farzad"))
                .isEqualTo("535 Authentication failed retry later");
    }

    @Test
    void sendTest_explainsWhenSmtpIsNotConfigured() {
        when(environment.getProperty("spring.mail.host", "")).thenReturn("");

        assertThat(emailService.sendTest("farzad@goldorigins.com", "Farzad")).contains("not configured");
        assertThat(noOpService.sendTest("farzad@goldorigins.com", "Farzad")).contains("not configured");
        verifyNoInteractions(mailSender);
    }
}
