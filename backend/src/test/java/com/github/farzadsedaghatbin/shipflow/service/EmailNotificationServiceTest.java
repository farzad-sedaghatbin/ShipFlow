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
}
