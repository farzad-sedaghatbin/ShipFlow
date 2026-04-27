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
 * Creates a default admin user if one doesn't exist. This runs before
 * SampleDataInitializer (Order=1 vs default Order). Disabled in test profile
 * via property.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
@ConditionalOnProperty(name = "app.admin.auto-create", havingValue = "true", matchIfMissing = true)
public class DefaultAdminInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PersonRepository personRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(String... args) {
    if (!userRepository.existsByUsername("admin")) {
      // Create admin person
      Person adminPerson = Person.builder().name("System Administrator").email("admin@shipflow.local")
          .skills("Administration, System Management").isActive(true).createdAt(LocalDateTime.now()).build();
      adminPerson = personRepository.save(adminPerson);

      // Create admin user
      User admin = User.builder().username("admin").password(passwordEncoder.encode("admin123"))
          .role(UserRole.ADMIN).person(adminPerson).isActive(true).build();
      userRepository.save(admin);
      log.info("===========================================");
      log.info("Default admin user created:");
      log.info("  Username: admin");
      log.info("  Password: admin123");
      log.info("  PLEASE CHANGE THE PASSWORD AFTER FIRST LOGIN!");
      log.info("===========================================");
    }
  }
}
