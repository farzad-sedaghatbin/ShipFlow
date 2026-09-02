package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
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
 * Regression tests for the Kanban-only demo seeder — the isolated fixture for the
 * "org whose only project type is Kanban" scenario (see the class javadoc and the
 * CHANGELOG's [Unreleased] entry for the bug class this exists to catch).
 */
class KanbanDemoInitializerTest {

  private final ProjectRepository projectRepository = mock(ProjectRepository.class);
  private final CycleRepository cycleRepository = mock(CycleRepository.class);
  private final TaskRepository taskRepository = mock(TaskRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final PersonRepository personRepository = mock(PersonRepository.class);

  private KanbanDemoInitializer initializer() {
    return new KanbanDemoInitializer(projectRepository, cycleRepository, taskRepository, userRepository,
        personRepository);
  }

  @Test
  void doesNotReinsertWhenDemoProjectAlreadyExists() {
    when(projectRepository.existsByProjectKey("SUP")).thenReturn(true);

    initializer().run();

    verify(projectRepository, never()).save(any(Project.class));
  }

  @Test
  void skipsWhenNoUsersExist() {
    when(projectRepository.existsByProjectKey("SUP")).thenReturn(false);
    when(userRepository.findByUsername("ali")).thenReturn(Optional.empty());
    when(userRepository.findAll()).thenReturn(List.of());

    initializer().run();

    verify(projectRepository, never()).save(any(Project.class));
  }

  @Test
  void seedsAKanbanProjectWithAHiddenContinuousFlowCycleAndTasks() {
    User admin = User.builder().id(1L).username("admin").build();
    when(projectRepository.existsByProjectKey("SUP")).thenReturn(false);
    when(userRepository.findByUsername("ali")).thenReturn(Optional.empty());
    when(userRepository.findAll()).thenReturn(List.of(admin));
    when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(cycleRepository.save(any(Cycle.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    initializer().run();

    verify(projectRepository, times(1)).save(any(Project.class));
    verify(cycleRepository, times(1)).save(any(Cycle.class));
    verify(taskRepository, times(6)).save(any(Task.class));
  }

  /** Opt-in: unlike ScrumDemoInitializer, off even in the dev profile — see class javadoc. */
  @Test
  void beanIsOptInViaProperty() {
    ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of())
        .withBean(ProjectRepository.class, () -> projectRepository)
        .withBean(CycleRepository.class, () -> cycleRepository)
        .withBean(TaskRepository.class, () -> taskRepository)
        .withBean(UserRepository.class, () -> userRepository)
        .withBean(PersonRepository.class, () -> personRepository)
        .withUserConfiguration(KanbanDemoInitializer.class);

    runner.run(ctx -> assertThat(ctx).doesNotHaveBean(KanbanDemoInitializer.class));
    runner.withPropertyValues("app.kanban-demo.auto-create=false")
        .run(ctx -> assertThat(ctx).doesNotHaveBean(KanbanDemoInitializer.class));
    runner.withPropertyValues("app.kanban-demo.auto-create=true")
        .run(ctx -> assertThat(ctx).hasSingleBean(KanbanDemoInitializer.class));
  }
}
