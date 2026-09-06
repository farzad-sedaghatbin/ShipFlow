package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a default admin user only on a genuinely fresh database (no users at
 * all). This runs before SampleDataInitializer (Order=1 vs default Order).
 * Disabled entirely via {@code app.admin.auto-create=false} (e.g. test profile).
 *
 * <p>Deliberately does NOT gate on {@code existsByUsername("admin")}: on a
 * non-fresh database this initializer must never re-create or re-activate the
 * default admin account, even if it was later renamed, deactivated, or deleted —
 * disabling/removing it must stick across restarts. The only thing checked on
 * every startup (regardless of whether this run created anything) is whether an
 * "admin"-named account still exists with the default password, so operators get
 * a loud, repeated reminder until they change it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
@ConditionalOnProperty(name = "app.admin.auto-create", havingValue = "true", matchIfMissing = true)
public class DefaultAdminInitializer implements CommandLineRunner {

  private static final String DEFAULT_ADMIN_USERNAME = "admin";
  private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

  private final UserRepository userRepository;
  private final PersonRepository personRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    if (userRepository.count() == 0) {
      createDefaultAdmin();
    }
    warnIfDefaultAdminStillUsesDefaultPassword();
  }

  /** Only ever called when the database has no users at all — a fresh install. */
  private void createDefaultAdmin() {
    Person adminPerson = Person.builder().name("System Administrator").email("admin@shipflow.local")
        .skills("Administration, System Management").isActive(true).createdAt(LocalDateTime.now()).build();
    adminPerson = personRepository.save(adminPerson);

    User admin = User.builder().username(DEFAULT_ADMIN_USERNAME)
        .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD)).role(UserRole.ADMIN).person(adminPerson)
        .isActive(true).build();
    userRepository.save(admin);
    log.info("===========================================");
    log.info("Default admin user created:");
    log.info("  Username: {}", DEFAULT_ADMIN_USERNAME);
    log.info("  Password: {}", DEFAULT_ADMIN_PASSWORD);
    log.info("  PLEASE CHANGE THE PASSWORD AFTER FIRST LOGIN!");
    log.info("===========================================");
  }

  /**
   * Runs on every startup, independent of whether {@link #createDefaultAdmin()}
   * ran this time. If an "admin"-named account exists and its password still
   * encodes to the well-known default, log a loud warning — this is the only
   * ongoing signal an operator gets that the account is still at its insecure
   * default, since we never touch the row again after creation.
   */
  private void warnIfDefaultAdminStillUsesDefaultPassword() {
    userRepository.findByUsername(DEFAULT_ADMIN_USERNAME).ifPresent(admin -> {
      if (passwordEncoder.matches(DEFAULT_ADMIN_PASSWORD, admin.getPassword())) {
        log.warn("===========================================");
        log.warn("SECURITY WARNING: the default admin account ('{}') still uses its", DEFAULT_ADMIN_USERNAME);
        log.warn("default, well-known password. Anyone who has read this source code (or the");
        log.warn("public ShipFlow repository) can log in as an administrator on this instance.");
        log.warn("Change the password immediately: log in and update it from Profile settings.");
        log.warn("===========================================");
      }
    });
  }
}
