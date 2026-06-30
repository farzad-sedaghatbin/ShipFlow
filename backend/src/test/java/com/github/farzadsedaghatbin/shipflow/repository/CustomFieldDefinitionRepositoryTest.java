package com.github.farzadsedaghatbin.shipflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldDefinition;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class CustomFieldDefinitionRepositoryTest {

  @Autowired
  private CustomFieldDefinitionRepository repository;

  @Autowired
  private ProjectRepository projectRepository;

  private Project projectA;
  private Project projectB;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    projectA = projectRepository.save(Project.builder().name("Project A").projectKey("PA").build());
    projectB = projectRepository.save(Project.builder().name("Project B").projectKey("PB").build());
  }

  @Test
  void findApplicable_returnsOrgWideAndProjectScoped() {
    // org-wide
    repository.save(
        CustomFieldDefinition.builder()
            .name("Sprint Notes")
            .fieldType(CustomFieldType.TEXT)
            .entityType(CustomFieldEntityType.TASK)
            .required(false)
            .sortOrder(0)
            .build());

    // project A scoped
    repository.save(
        CustomFieldDefinition.builder()
            .name("Priority Tier")
            .fieldType(CustomFieldType.SELECT)
            .entityType(CustomFieldEntityType.TASK)
            .project(projectA)
            .required(false)
            .sortOrder(1)
            .build());

    // project B scoped — should NOT appear for project A
    repository.save(
        CustomFieldDefinition.builder()
            .name("B-Only Field")
            .fieldType(CustomFieldType.TEXT)
            .entityType(CustomFieldEntityType.TASK)
            .project(projectB)
            .required(false)
            .sortOrder(0)
            .build());

    List<CustomFieldDefinition> results =
        repository.findApplicable(CustomFieldEntityType.TASK, projectA.getId());

    assertThat(results).hasSize(2);
    assertThat(results).extracting(CustomFieldDefinition::getName)
        .containsExactlyInAnyOrder("Sprint Notes", "Priority Tier");
  }

  @Test
  void findApplicable_excludesDeletedDefinitions() {
    var deleted =
        CustomFieldDefinition.builder()
            .name("Old Field")
            .fieldType(CustomFieldType.TEXT)
            .entityType(CustomFieldEntityType.TASK)
            .required(false)
            .sortOrder(0)
            .build();
    deleted.setDeletedAt(java.time.OffsetDateTime.now());
    repository.save(deleted);

    List<CustomFieldDefinition> results =
        repository.findApplicable(CustomFieldEntityType.TASK, projectA.getId());

    assertThat(results).isEmpty();
  }

  @Test
  void findApplicable_excludesOtherEntityType() {
    repository.save(
        CustomFieldDefinition.builder()
            .name("Bug Field")
            .fieldType(CustomFieldType.DATE)
            .entityType(CustomFieldEntityType.BUG)
            .required(false)
            .sortOrder(0)
            .build());

    List<CustomFieldDefinition> results =
        repository.findApplicable(CustomFieldEntityType.TASK, projectA.getId());

    assertThat(results).isEmpty();
  }
}
