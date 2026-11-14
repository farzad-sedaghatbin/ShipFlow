package com.github.farzadsedaghatbin.shipflow.service.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import org.junit.jupiter.api.Test;

class EmailAddressResolverTest {

  private static User user(String userEmail, String personEmail) {
    Person person = personEmail == null ? null : Person.builder().name("P").email(personEmail).build();
    return User.builder().username("u").email(userEmail).person(person).build();
  }

  @Test
  void usersOwnAddressWins() {
    assertThat(EmailAddressResolver.of(user("own@goldorigins.com", "person@goldorigins.com")))
        .isEqualTo("own@goldorigins.com");
  }

  @Test
  void fallsBackToThePersonsAddress_becauseUsersEmailIsUsuallyEmpty() {
    assertThat(EmailAddressResolver.of(user(null, "hooman@goldorigins.com"))).isEqualTo("hooman@goldorigins.com");
    assertThat(EmailAddressResolver.of(user("  ", "hooman@goldorigins.com"))).isEqualTo("hooman@goldorigins.com");
  }

  @Test
  void placeholderAndMalformedAddressesAreNotEmailed() {
    assertThat(EmailAddressResolver.of(user(null, "admin@shipflow.local"))).isNull();
    assertThat(EmailAddressResolver.of(user(null, "someone@example.com"))).isNull();
    assertThat(EmailAddressResolver.of(user(null, "not-an-address"))).isNull();
    assertThat(EmailAddressResolver.of(user(null, "a@b.c, d@e.f"))).isNull();
    assertThat(EmailAddressResolver.of(user(null, null))).isNull();
    assertThat(EmailAddressResolver.of(null)).isNull();
  }

  @Test
  void usableTrimsSurroundingSpace() {
    assertThat(EmailAddressResolver.usable("  a.nikoo@goldorigins.com ")).isEqualTo("a.nikoo@goldorigins.com");
  }
}
