package com.github.farzadsedaghatbin.shipflow.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs with NO surrounding transaction, like an automation action or a controller (open-in-view is
 * off): the address lives on the lazy {@code Person}, and handing a detached user to code that reads
 * {@code user.getPerson()} threw LazyInitializationException on the first real run.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(EmailRecipientService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailRecipientServiceTest {

  @Autowired
  private EmailRecipientService recipients;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PersonRepository personRepository;

  private User save(String username, String personEmail) {
    Person person = personRepository.save(Person.builder().name(username + " Name").email(personEmail)
        .skills("x").isActive(true).createdAt(LocalDateTime.now()).build());
    return userRepository.save(
        User.builder().username(username).password("x").role(UserRole.MEMBER).person(person).isActive(true).build());
  }

  @Test
  void resolvesThePersonsAddressWithoutAnOpenSession() {
    User saved = save("recipient-ok", "recipient.ok@goldorigins.com");

    // a freshly loaded user is detached with a lazy, uninitialised person — exactly what callers hold
    User detached = userRepository.findByUsername("recipient-ok").orElseThrow();
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> detached.getPerson().getEmail())
        .isInstanceOf(org.hibernate.LazyInitializationException.class);

    assertThat(recipients.forUserId(saved.getId())).hasValueSatisfying(r -> {
      assertThat(r.address()).isEqualTo("recipient.ok@goldorigins.com");
      assertThat(r.name()).isEqualTo("recipient-ok Name");
    });
  }

  @Test
  void placeholderAddressAndUnknownIdsResolveToNothing() {
    User placeholder = save("recipient-placeholder", "admin@shipflow.local");

    assertThat(recipients.forUserId(placeholder.getId())).isEmpty();
    assertThat(recipients.forUserId(987654321L)).isEmpty();
    assertThat(recipients.forUserId(null)).isEmpty();
  }
}
