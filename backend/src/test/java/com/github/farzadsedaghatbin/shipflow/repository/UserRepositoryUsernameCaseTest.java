package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Usernames are matched case-insensitively ("Hooman" signs in as "hooman"). */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryUsernameCaseTest {

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  private User save(String username) {
    return userRepository
        .save(User.builder().username(username).password("x").role(UserRole.MEMBER).isActive(true).build());
  }

  @Test
  void findByUsername_ignoresCaseAndReturnsTheStoredSpelling() {
    save("hooman");

    assertThat(userRepository.findByUsername("Hooman")).hasValueSatisfying(u -> assertThat(u.getUsername()).isEqualTo("hooman"));
    assertThat(userRepository.findByUsername("HOOMAN")).isPresent();
    assertThat(userRepository.findByUsernameWithPerson("Hooman")).isPresent();
  }

  @Test
  void findByUsername_matchesEmailStyleUsernames() {
    save("a.nikoo@goldorigins.com");

    assertThat(userRepository.findByUsername("A.Nikoo@GoldOrigins.com")).isPresent();
  }

  @Test
  void findByUsername_unknownOrNull_isEmpty() {
    save("hooman");

    assertThat(userRepository.findByUsername("someone-else")).isEmpty();
    assertThat(userRepository.findByUsername(null)).isEmpty();
    assertThat(userRepository.findByUsernameWithPerson(null)).isEmpty();
  }

  @Test
  void existingAccountsDifferingOnlyByCase_exactMatchWins() {
    // An install created before this change may already hold both spellings.
    User lower = save("sam");
    User upper = save("SAM");

    assertThat(userRepository.findByUsername("sam")).hasValueSatisfying(u -> assertThat(u.getId()).isEqualTo(lower.getId()));
    assertThat(userRepository.findByUsername("SAM")).hasValueSatisfying(u -> assertThat(u.getId()).isEqualTo(upper.getId()));
  }

  @Test
  void existingAccountsDifferingOnlyByCase_ambiguousSpellingMatchesNobody() {
    save("sam");
    save("SAM");

    assertThat(userRepository.findByUsername("Sam")).isEmpty();
  }

  @Test
  void existsByUsername_ignoresCase_soTheSameNameCannotBeRegisteredTwice() {
    save("hooman");

    assertThat(userRepository.existsByUsername("Hooman")).isTrue();
    assertThat(userRepository.existsByUsername("hooman")).isTrue();
    assertThat(userRepository.existsByUsername("nobody")).isFalse();
  }
}
