package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the scope-aware lookups on {@link KnowledgeSourceRepository}.
 *
 * <p>Single-org deployment: queries do not take an {@code organizationId} parameter. ORG-scope
 * sources are visible to all authenticated users; TEAM- and PROJECT-scope sources are filtered by
 * their owning id.
 */
@DataJpaTest
@ActiveProfiles("test")
class KnowledgeSourceRepositoryTest {

  @Autowired private KnowledgeSourceRepository repository;

  private KnowledgeSource newSource(
      String name, KnowledgeSourceScope scope, Long teamId, Long projectId) {
    return KnowledgeSource.builder()
        .name(name)
        .description("desc")
        .providerType(KnowledgeProviderType.URL)
        .scope(scope)
        .teamId(teamId)
        .projectId(projectId)
        .config("{}")
        .status(KnowledgeSourceStatus.READY)
        .createdBy(1L)
        .build();
  }

  @Test
  void findActiveByOrgScope_returnsOrgScopedSources() {
    KnowledgeSource org = repository.save(newSource("org-doc", KnowledgeSourceScope.ORG, null, null));
    repository.save(newSource("team-doc", KnowledgeSourceScope.TEAM, 7L, null));
    repository.save(newSource("project-doc", KnowledgeSourceScope.PROJECT, null, 9L));

    List<KnowledgeSource> results = repository.findActiveByOrgScope();

    assertThat(results).extracting(KnowledgeSource::getId).containsExactly(org.getId());
  }

  @Test
  void findActiveByOrgScope_excludesSoftDeleted() {
    KnowledgeSource deleted = newSource("gone", KnowledgeSourceScope.ORG, null, null);
    deleted.setDeletedAt(OffsetDateTime.now());
    repository.save(deleted);

    assertThat(repository.findActiveByOrgScope()).isEmpty();
  }

  @Test
  void findActiveByTeamScope_filtersByTeamId() {
    KnowledgeSource team7 = repository.save(newSource("team7-doc", KnowledgeSourceScope.TEAM, 7L, null));
    repository.save(newSource("team8-doc", KnowledgeSourceScope.TEAM, 8L, null));
    repository.save(newSource("org-doc", KnowledgeSourceScope.ORG, null, null));

    List<KnowledgeSource> results = repository.findActiveByTeamScope(7L);

    assertThat(results).extracting(KnowledgeSource::getId).containsExactly(team7.getId());
  }
}
