package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the "Customer Support — Kanban Demo" project (key: SUP) on startup if it is absent.
 *
 * <p><b>Opt-in, off by default even in the dev profile.</b> Gated by
 * {@code app.kanban-demo.auto-create}. Unlike {@link ScrumDemoInitializer} (on by default in dev,
 * additive alongside {@link SampleDataInitializer}'s mixed-type seed), this one is deliberately
 * off everywhere by default: its purpose is not everyday dev variety — {@code SampleDataInitializer}
 * already seeds one Kanban project ("DevOps Platform" / DVP) for that — it is to reproduce, on a
 * fresh database, an organization whose <em>only</em> project type is Kanban. That scenario (a
 * white-label deployment with no Shape Up/Scrum projects at all) is what surfaced the
 * "All Projects mode defaults to Shape Up regardless of what the org actually has" class of bug
 * fixed alongside this initializer — see the CHANGELOG's [Unreleased] entry and
 * {@code PROJECT_TYPE_ARCHITECTURE.md}. A mixed-type dev database (the normal case) can never
 * exercise that code path, since {@code resolveOrgCapabilities} would legitimately resolve to
 * Shape Up whenever a Shape Up project exists anywhere in the deployment.
 *
 * <p>To reproduce a Kanban-only org locally: start against a <em>fresh</em> database with
 * {@code app.sample-data.enabled=false}, {@code app.scrum-demo.auto-create=false}, and
 * {@code app.kanban-demo.auto-create=true}. {@link DefaultAdminInitializer} still runs
 * unconditionally, so an {@code admin} user exists to own the project.
 *
 * <p>For the Scrum-only equivalent, no new code is needed: {@code app.sample-data.enabled=false}
 * with {@code app.scrum-demo.auto-create=true} already seeds only {@link ScrumDemoInitializer}'s
 * Scrum project, since it falls back to any existing user (the always-present {@code admin}) when
 * its preferred demo user ({@code sara}) doesn't exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(4)
@ConditionalOnProperty(name = "app.kanban-demo.auto-create", havingValue = "true")
public class KanbanDemoInitializer implements CommandLineRunner {

  private static final String PROJECT_KEY = "SUP";

  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final PersonRepository personRepository;

  @Override
  @Transactional
  public void run(String... args) {
    if (projectRepository.existsByProjectKey(PROJECT_KEY)) {
      log.info("KanbanDemoInitializer: {} already exists — skipping", PROJECT_KEY);
      return;
    }

    // Prefer the demo 'ali' engineer; fall back to any user (the always-present 'admin')
    // so this also works standalone against a fresh DB with no other seed data.
    User owner = userRepository.findByUsername("ali")
        .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));
    if (owner == null) {
      log.info("KanbanDemoInitializer: no users in DB yet — skipping (will run after first user is created)");
      return;
    }

    log.info("KanbanDemoInitializer: seeding Customer Support — Kanban Demo ({}) with owner '{}'",
        PROJECT_KEY, owner.getUsername());

    Person aliPerson = personRepository.findByEmail("ali@shipflow.dev").orElse(null);
    Person minaPerson = personRepository.findByEmail("mina@shipflow.dev").orElse(null);
    Person saraPerson = personRepository.findByEmail("sara@shipflow.dev").orElse(null);

    Project kanbanProject = Project.builder()
        .name("Customer Support — Kanban Demo")
        .projectKey(PROJECT_KEY)
        .description(
            "Continuous-flow support ticket triage — showcases ShipFlow's Kanban mode with no "
                + "cycles, pitches, or betting; work flows straight across a status board.")
        .color("#F97316")
        .projectType(ProjectType.KANBAN)
        .owner(owner)
        .isActive(true)
        .enableRetrospectives(false)
        .createdAt(LocalDateTime.now().minusDays(10))
        .build();
    projectRepository.save(kanbanProject);

    // Mirrors ProjectService#createDefaultKanbanCycle exactly — this initializer inserts
    // directly via repositories rather than through ProjectService, so it must create the
    // hidden "Continuous Flow" cycle itself for tasks to attach to.
    Cycle continuousFlow = cycleRepository.save(Cycle.builder()
        .name("Continuous Flow").project(kanbanProject)
        .startDate(LocalDate.now())
        .endDate(LocalDate.of(2099, 12, 31))
        .phase(CyclePhase.SHAPING_BUILDING).isActive(true)
        .build());

    task("Investigate failed webhook deliveries", TaskStatus.DONE, TaskPriority.HIGH,
        continuousFlow, aliPerson, saraPerson, "support,bug");
    task("Password reset email lands in spam", TaskStatus.DONE, TaskPriority.MEDIUM,
        continuousFlow, minaPerson, saraPerson, "support,email");
    task("Update help-center article for SSO login", TaskStatus.IN_REVIEW, TaskPriority.LOW,
        continuousFlow, minaPerson, saraPerson, "docs");
    task("Customer export CSV missing a column", TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
        continuousFlow, aliPerson, saraPerson, "support,bug");
    task("Triage backlog of billing questions", TaskStatus.TODO, TaskPriority.MEDIUM,
        continuousFlow, aliPerson, saraPerson, "support,billing");
    task("Third-party auth provider outage follow-up", TaskStatus.BLOCKED, TaskPriority.URGENT,
        continuousFlow, minaPerson, saraPerson, "support,incident");

    log.info("KanbanDemoInitializer: complete — 1 project + 6 tasks seeded for Customer Support — Kanban Demo");
  }

  private void task(String title, TaskStatus status, TaskPriority priority,
      Cycle cycle, Person assignee, Person createdBy, String tags) {
    Task t = Task.builder()
        .title(title)
        .description(title + " — Kanban demo task")
        .status(status).priority(priority)
        .cycle(cycle).project(cycle.getProject())
        .assignee(assignee).createdBy(createdBy).tags(tags)
        .build();
    if (status == TaskStatus.DONE) {
      t.setCompletedAt(LocalDateTime.now().minusDays(2));
    }
    taskRepository.save(t);
  }
}
