package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that {@link ApiKeyRepository#findByKeyHash(String)} eagerly fetches the owning user.
 *
 * <p>Regression test for the production {@link org.hibernate.LazyInitializationException} thrown from
 * {@code McpAuthFilter}: the filter reads {@code apiKey.getUser().getUsername()} after the validating
 * transaction has closed, so the {@code @ManyToOne(fetch = LAZY)} user must be initialized by the
 * lookup query itself.
 */
@DataJpaTest
@ActiveProfiles("test")
class ApiKeyRepositoryTest {

  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private UserRepository userRepository;

  @PersistenceContext private EntityManager entityManager;

  private User createUser(String username) {
    User user = new User();
    user.setUsername(username);
    user.setEmail(username + "@example.com");
    user.setPassword("password");
    user.setRole(UserRole.MEMBER);
    return userRepository.save(user);
  }

  @Test
  void findByKeyHash_eagerlyFetchesUser() {
    User owner = createUser("mcpuser");
    ApiKey key =
        ApiKey.builder()
            .name("ci-key")
            .keyPrefix("sf_test_")
            .keyHash("hash-abc-123")
            .user(owner)
            .isActive(true)
            .build();
    apiKeyRepository.save(key);

    // Detach everything so the lookup must materialize the user via its own query,
    // exactly as it does on a fresh MCP request.
    entityManager.flush();
    entityManager.clear();

    Optional<ApiKey> found = apiKeyRepository.findByKeyHash("hash-abc-123");

    assertThat(found).isPresent();
    // The crux: the user association must be initialized by the JOIN FETCH so that reading it
    // after the transaction closes does not throw LazyInitializationException.
    assertThat(Hibernate.isInitialized(found.get().getUser())).isTrue();
    assertThat(found.get().getUser().getUsername()).isEqualTo("mcpuser");
  }

  @Test
  void findByKeyHash_returnsEmptyWhenMissing() {
    assertThat(apiKeyRepository.findByKeyHash("does-not-exist")).isEmpty();
  }
}
