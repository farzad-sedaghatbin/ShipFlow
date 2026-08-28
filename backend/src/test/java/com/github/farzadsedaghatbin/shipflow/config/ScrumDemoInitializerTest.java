package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Regression tests for the Scrum demo seeder.
 *
 * <p>Both behaviours here were production incidents on a real PostgreSQL deployment and neither
 * is reachable from the rest of the suite: tests run on H2 with a schema generated from entities,
 * so the {@code projects.project_key} unique index and these startup runners never participate.
 */
class ScrumDemoInitializerTest {

  private final ProjectRepository projectRepository = mock(ProjectRepository.class);
  private final CycleRepository cycleRepository = mock(CycleRepository.class);
  private final TaskRepository taskRepository = mock(TaskRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final PersonRepository personRepository = mock(PersonRepository.class);

  private ScrumDemoInitializer initializer() {
    return new ScrumDemoInitializer(projectRepository, cycleRepository, taskRepository, userRepository,
        personRepository);
  }

  /**
   * The bug that crash-looped production: an archived MAS project still owns the unique
   * {@code project_key}, so the seeder must treat it as present and never re-INSERT. Checking
   * {@code isActive = true} reported it as missing and the insert died with
   * "duplicate key value violates unique constraint projects_project_key_key", which is fatal
   * inside a CommandLineRunner.
   */
  @Test
  void doesNotReinsertWhenExistingDemoProjectIsArchived() {
    Project archived = Project.builder().name("Mobile App — Scrum Demo").projectKey("MAS").isActive(false).build();
    archived.setId(1L);

    when(projectRepository.existsByProjectKey("MAS")).thenReturn(true);
    when(projectRepository.findByProjectKey("MAS")).thenReturn(Optional.of(archived));
    when(cycleRepository.findByProjectIdOrderByStartDateDesc(anyLong())).thenReturn(List.of(mock(Cycle.class)));
    when(taskRepository.countByCycleId(anyLong())).thenReturn(5);

    initializer().run();

    verify(projectRepository, never()).save(any(Project.class));
    // The active-only lookup is what made an archived project look absent.
    verify(projectRepository, never()).existsByProjectKeyNotDeleted(anyString());
  }

  /** With the project genuinely absent and no users, it must bail out rather than seed. */
  @Test
  void skipsWhenNoUsersExist() {
    when(projectRepository.existsByProjectKey("MAS")).thenReturn(false);
    when(userRepository.findByUsername("sara")).thenReturn(Optional.empty());
    when(userRepository.findAll()).thenReturn(List.of());

    initializer().run();

    verify(projectRepository, never()).save(any(Project.class));
  }

  /**
   * The seeder is opt-in. A production install leaves {@code app.scrum-demo.auto-create} unset,
   * and must not get demo data — previously it ran unconditionally and seeded MAS on every clean
   * production database, because DefaultAdminInitializer had already created a user.
   */
  @Test
  void beanIsOptInViaProperty() {
    ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of())
        .withBean(ProjectRepository.class, () -> projectRepository)
        .withBean(CycleRepository.class, () -> cycleRepository)
        .withBean(TaskRepository.class, () -> taskRepository)
        .withBean(UserRepository.class, () -> userRepository)
        .withBean(PersonRepository.class, () -> personRepository)
        .withUserConfiguration(ScrumDemoInitializer.class);

    runner.run(ctx -> assertThat(ctx).doesNotHaveBean(ScrumDemoInitializer.class));
    runner.withPropertyValues("app.scrum-demo.auto-create=false")
        .run(ctx -> assertThat(ctx).doesNotHaveBean(ScrumDemoInitializer.class));
    runner.withPropertyValues("app.scrum-demo.auto-create=true")
        .run(ctx -> assertThat(ctx).hasSingleBean(ScrumDemoInitializer.class));
  }
}
