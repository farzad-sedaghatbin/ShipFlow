package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the "Mobile App — Scrum Demo" project (key: MAS) on every startup if it is absent.
 *
 * <p>Unlike {@link SampleDataInitializer}, this initializer runs unconditionally — it is NOT
 * gated by {@code app.sample-data.enabled}. That allows production deployments that were
 * created before v1.1.0 (and therefore never had sample-data enabled) to still receive the
 * Scrum demo project automatically on the next restart.
 *
 * <p>Guard: the project is only seeded when at least one user already exists. A completely
 * fresh database will be handled by SampleDataInitializer (Order=2) instead.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(3)
public class ScrumDemoInitializer implements CommandLineRunner {

  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final PersonRepository personRepository;

  @Override
  @Transactional
  public void run(String... args) {
    if (projectRepository.existsByProjectKeyNotDeleted("MAS")) {
      // Project exists — verify tasks were also seeded. A prior run could have committed
      // the project + cycles but crashed before the task inserts (e.g. missing DB column).
      projectRepository.findByProjectKey("MAS").ifPresent(masProject -> {
        List<Cycle> cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(masProject.getId());
        long totalTasks = cycles.stream()
            .mapToLong(c -> taskRepository.countByCycleId(c.getId()))
            .sum();
        if (totalTasks == 0 && !cycles.isEmpty()) {
          log.info("ScrumDemoInitializer: MAS cycles exist but have 0 tasks — back-filling tasks now");
          seedTasksForCycles(cycles);
        } else {
          log.info("ScrumDemoInitializer: MAS already fully seeded ({} cycles, {} tasks) — skipping",
              cycles.size(), totalTasks);
        }
      });
      return;
    }

    // Prefer the demo 'sara' manager; fall back to any user so custom DBs work too.
    User owner = userRepository.findByUsername("sara")
        .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    if (owner == null) {
      log.info("ScrumDemoInitializer: no users in DB yet — skipping (will run after first user is created)");
      return;
    }

    log.info("ScrumDemoInitializer: seeding Mobile App — Scrum Demo (MAS) with owner '{}'", owner.getUsername());

    Person aliPerson = personRepository.findByEmail("ali@shipflow.dev").orElse(null);
    Person minaPerson = personRepository.findByEmail("mina@shipflow.dev").orElse(null);
    Person saraPerson = personRepository.findByEmail("sara@shipflow.dev").orElse(null);

    Project scrumProject = Project.builder()
        .name("Mobile App — Scrum Demo")
        .projectKey("MAS")
        .description(
            "Cross-platform mobile app delivered in two-week sprints — showcases ShipFlow's "
                + "Scrum mode with story points, burndown, and velocity tracking.")
        .color("#A855F7")
        .projectType(ProjectType.SCRUM)
        .owner(owner)
        .isActive(true)
        .enableRetrospectives(true)
        .createdAt(LocalDateTime.of(2026, 3, 1, 9, 0))
        .build();
    projectRepository.save(scrumProject);

    LocalDate now = LocalDate.now();

    // Sprint 1 — completed (8–6 weeks ago, 13 pts)
    LocalDate s1Start = now.minusWeeks(8);
    LocalDate s1End = now.minusWeeks(6);
    Cycle sprint1 = cycleRepository.save(Cycle.builder()
        .project(scrumProject).name("Sprint 1")
        .startDate(s1Start).endDate(s1End)
        .phase(CyclePhase.SHAPING_BUILDING).isActive(false)
        .sprintGoal("Ship onboarding flow with email + social sign-in")
        .build());

    // Sprint 2 — completed (5–3 weeks ago, 16 pts)
    LocalDate s2Start = now.minusWeeks(5);
    LocalDate s2End = now.minusWeeks(3);
    Cycle sprint2 = cycleRepository.save(Cycle.builder()
        .project(scrumProject).name("Sprint 2")
        .startDate(s2Start).endDate(s2End)
        .phase(CyclePhase.SHAPING_BUILDING).isActive(false)
        .sprintGoal("Add push notifications and in-app messaging")
        .build());

    // Sprint 3 — active (2 weeks ago → 3 days from now)
    LocalDate s3Start = now.minusWeeks(2);
    LocalDate s3End = now.plusDays(3);
    Cycle sprint3 = cycleRepository.save(Cycle.builder()
        .project(scrumProject).name("Sprint 3")
        .startDate(s3Start).endDate(s3End)
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true)
        .sprintGoal("Polish UX, fix top customer-reported bugs, ship dark mode")
        .build());

    // Sprint 1 tasks — all DONE, staggered completedAt for burndown staircase
    task("Email sign-up endpoint",       TaskStatus.DONE, TaskPriority.HIGH,   5, sprint1, aliPerson,  saraPerson, "backend,auth",          s1Start.plusDays(3).atTime(11, 0));
    task("Google OAuth integration",     TaskStatus.DONE, TaskPriority.HIGH,   3, sprint1, aliPerson,  saraPerson, "backend,oauth",          s1Start.plusDays(7).atTime(15, 30));
    task("Onboarding wizard screens",    TaskStatus.DONE, TaskPriority.MEDIUM, 5, sprint1, minaPerson, saraPerson, "frontend,ux",            s1Start.plusDays(12).atTime(9, 45));

    // Sprint 2 tasks — all DONE
    task("APNs + FCM token registration", TaskStatus.DONE, TaskPriority.HIGH,   8, sprint2, aliPerson,  saraPerson, "backend,notifications",  s2Start.plusDays(4).atTime(14, 0));
    task("Notification preferences UI",   TaskStatus.DONE, TaskPriority.MEDIUM, 3, sprint2, minaPerson, saraPerson, "frontend",               s2Start.plusDays(8).atTime(10, 15));
    task("In-app message center",         TaskStatus.DONE, TaskPriority.MEDIUM, 5, sprint2, minaPerson, saraPerson, "frontend,messaging",      s2Start.plusDays(11).atTime(16, 0));

    // Sprint 3 tasks — mixed statuses for realistic active burndown
    task("Dark-mode theme tokens",        TaskStatus.DONE,        TaskPriority.MEDIUM, 2, sprint3, minaPerson, saraPerson, "frontend,theme",      s3Start.plusDays(3).atTime(11, 0));
    task("Fix top 5 crash reports",       TaskStatus.DONE,        TaskPriority.HIGH,   5, sprint3, aliPerson,  saraPerson, "bugfix",              s3Start.plusDays(8).atTime(17, 0));
    task("Accessibility audit pass",      TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, 3, sprint3, minaPerson, saraPerson, "a11y,frontend",       null);
    task("Performance: lazy-load images", TaskStatus.TODO,        TaskPriority.LOW,    2, sprint3, minaPerson, saraPerson, "performance,frontend", null);

    log.info("ScrumDemoInitializer: complete — 3 sprints + 10 tasks seeded for Mobile App — Scrum Demo");
  }

  /**
   * Back-fills tasks into existing MAS cycles when a prior startup seeded the project and cycles
   * but crashed before reaching the task-insert section (e.g. due to a missing DB column or
   * constraint). Cycles are matched by start-date order: oldest → Sprint 1, …, newest → Sprint 3.
   */
  private void seedTasksForCycles(List<Cycle> cycles) {
    Person aliPerson = personRepository.findByEmail("ali@shipflow.dev").orElse(null);
    Person minaPerson = personRepository.findByEmail("mina@shipflow.dev").orElse(null);
    Person saraPerson = personRepository.findByEmail("sara@shipflow.dev").orElse(null);

    List<Cycle> ordered = cycles.stream()
        .sorted(Comparator.comparing(Cycle::getStartDate))
        .toList();

    if (ordered.size() >= 1) {
      Cycle s1 = ordered.get(0);
      LocalDate s1Start = s1.getStartDate();
      task("Email sign-up endpoint",    TaskStatus.DONE, TaskPriority.HIGH,   5, s1, aliPerson,  saraPerson, "backend,auth",    s1Start.plusDays(3).atTime(11, 0));
      task("Google OAuth integration",  TaskStatus.DONE, TaskPriority.HIGH,   3, s1, aliPerson,  saraPerson, "backend,oauth",   s1Start.plusDays(7).atTime(15, 30));
      task("Onboarding wizard screens", TaskStatus.DONE, TaskPriority.MEDIUM, 5, s1, minaPerson, saraPerson, "frontend,ux",     s1Start.plusDays(12).atTime(9, 45));
    }
    if (ordered.size() >= 2) {
      Cycle s2 = ordered.get(1);
      LocalDate s2Start = s2.getStartDate();
      task("APNs + FCM token registration", TaskStatus.DONE, TaskPriority.HIGH,   8, s2, aliPerson,  saraPerson, "backend,notifications", s2Start.plusDays(4).atTime(14, 0));
      task("Notification preferences UI",   TaskStatus.DONE, TaskPriority.MEDIUM, 3, s2, minaPerson, saraPerson, "frontend",              s2Start.plusDays(8).atTime(10, 15));
      task("In-app message center",         TaskStatus.DONE, TaskPriority.MEDIUM, 5, s2, minaPerson, saraPerson, "frontend,messaging",     s2Start.plusDays(11).atTime(16, 0));
    }
    if (ordered.size() >= 3) {
      Cycle s3 = ordered.get(2);
      LocalDate s3Start = s3.getStartDate();
      task("Dark-mode theme tokens",        TaskStatus.DONE,        TaskPriority.MEDIUM, 2, s3, minaPerson, saraPerson, "frontend,theme",       s3Start.plusDays(3).atTime(11, 0));
      task("Fix top 5 crash reports",       TaskStatus.DONE,        TaskPriority.HIGH,   5, s3, aliPerson,  saraPerson, "bugfix",               s3Start.plusDays(8).atTime(17, 0));
      task("Accessibility audit pass",      TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, 3, s3, minaPerson, saraPerson, "a11y,frontend",        null);
      task("Performance: lazy-load images", TaskStatus.TODO,        TaskPriority.LOW,    2, s3, minaPerson, saraPerson, "performance,frontend",  null);
    }
    log.info("ScrumDemoInitializer: task back-fill complete");
  }

  private void task(String title, TaskStatus status, TaskPriority priority, int points,
      Cycle cycle, Person assignee, Person createdBy, String tags, LocalDateTime completedAt) {
    Task t = Task.builder()
        .title(title)
        .description(title + " — Scrum demo task")
        .status(status).priority(priority).storyPoints(points)
        .cycle(cycle).project(cycle.getProject())
        .assignee(assignee).createdBy(createdBy).tags(tags)
        .build();
    if (completedAt != null) t.setCompletedAt(completedAt);
    taskRepository.save(t);
  }
}
