package com.github.farzadsedaghatbin.shipflow.service.email;

import com.github.farzadsedaghatbin.shipflow.dto.admin.EmailStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Reports the server's real email configuration (the SMTP environment variables) for the settings page. */
@Service
@RequiredArgsConstructor
public class EmailStatusService {

  private final Environment environment;

  public EmailStatusDTO status() {
    String host = environment.getProperty("spring.mail.host", "");
    boolean configured = !host.isBlank();
    return new EmailStatusDTO(configured, blankToNull(host), environment.getProperty("spring.mail.port", Integer.class),
        blankToNull(environment.getProperty("spring.mail.username", "")),
        blankToNull(environment.getProperty("app.mail.from", "")),
        blankToNull(environment.getProperty("app.mail.reply-to", "")),
        blankToNull(environment.getProperty("app.mail.base-url", "")),
        Boolean.parseBoolean(environment.getProperty("spring.mail.properties.mail.smtp.starttls.required", "false")));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }
}
