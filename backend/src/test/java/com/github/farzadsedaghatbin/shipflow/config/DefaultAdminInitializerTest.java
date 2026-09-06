package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for the fixed seeding contract: the default admin is created only
 * on a genuinely fresh database, is never re-created or re-activated
 * afterwards, and a warning is logged (best-effort, not asserted here) whenever
 * an "admin" account still has the well-known default password.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultAdminInitializerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private DefaultAdminInitializer initializer;

  @BeforeEach
  void setUp() {
    initializer = new DefaultAdminInitializer(userRepository, personRepository, passwordEncoder);
    lenient().when(passwordEncoder.encode(any())).thenReturn("encoded-admin123");
    lenient().when(personRepository.save(any(Person.class))).thenAnswer(inv -> {
      Person p = inv.getArgument(0);
      if (p.getId() == null) {
        p.setId(1L);
      }
      return p;
    });
    lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
      User u = inv.getArgument(0);
      if (u.getId() == null) {
        u.setId(1L);
      }
      return u;
    });
  }

  @Test
  void run_FreshDatabase_CreatesDefaultAdminWithAdminRole() {
    when(userRepository.count()).thenReturn(0L);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

    initializer.run();

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User created = captor.getValue();
    assertThat(created.getUsername()).isEqualTo("admin");
    assertThat(created.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(created.getIsActive()).isTrue();
    verify(passwordEncoder).encode("admin123");
  }

  @Test
  void run_NonFreshDatabase_AdminAccountRenamedOrDeleted_DoesNotRecreateIt() {
    when(userRepository.count()).thenReturn(5L);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

    initializer.run();

    verify(userRepository, never()).save(any());
    verify(personRepository, never()).save(any());
  }

  @Test
  void run_NonFreshDatabase_ExistingAdminDeactivated_DoesNotReactivateOrRecreate() {
    User deactivatedAdmin = User.builder().id(9L).username("admin").role(UserRole.ADMIN).isActive(false)
        .password("encoded-admin123").build();
    when(userRepository.count()).thenReturn(3L);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(deactivatedAdmin));
    when(passwordEncoder.matches("admin123", "encoded-admin123")).thenReturn(true);

    initializer.run();

    verify(userRepository, never()).save(any());
    assertThat(deactivatedAdmin.getIsActive()).isFalse();
  }

  @Test
  void run_NonFreshDatabase_AdminPasswordAlreadyChanged_ChecksButDoesNotWarnOrMutate() {
    User admin = User.builder().id(9L).username("admin").role(UserRole.ADMIN).isActive(true)
        .password("some-other-encoded-hash").build();
    when(userRepository.count()).thenReturn(3L);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(passwordEncoder.matches("admin123", "some-other-encoded-hash")).thenReturn(false);

    initializer.run();

    verify(passwordEncoder).matches("admin123", "some-other-encoded-hash");
    verify(userRepository, never()).save(any());
  }

  @Test
  void run_FreshDatabase_NeverCallsExistsByUsername() {
    // Regression guard: the old gate was existsByUsername("admin"), which let a
    // renamed/soft-deleted admin row resurrect on restart. The new gate must be
    // userRepository.count() only.
    when(userRepository.count()).thenReturn(0L);
    when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

    initializer.run();

    verify(userRepository, never()).existsByUsername(any());
  }
}
