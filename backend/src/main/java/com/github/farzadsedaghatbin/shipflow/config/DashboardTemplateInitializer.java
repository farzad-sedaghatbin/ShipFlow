package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.CustomDashboard;
import com.github.farzadsedaghatbin.shipflow.entity.CustomDashboard.TemplateCategory;
import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidgetConfig;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.CustomDashboardRepository;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardWidgetConfigRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates default dashboard templates after admin user is created. This runs
 * AFTER DefaultAdminInitializer (Order=2 vs Order=1). Templates are system-wide
 * and available to all users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DefaultAdminInitializer
@ConditionalOnProperty(name = "app.admin.auto-create", havingValue = "true", matchIfMissing = true)
public class DashboardTemplateInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final CustomDashboardRepository customDashboardRepository;
  private final DashboardWidgetConfigRepository widgetConfigRepository;

  private static final String DEFAULT_LAYOUT_CONFIG = "{\"columns\": 12, \"rowHeight\": 60, \"breakpoints\": {\"lg\": 1200, \"md\": 996, \"sm\": 768}}";

  @Override
  @Transactional
  public void run(String... args) {
    // Find any user to own the templates (preferably admin)
    User templateOwner = userRepository.findByUsername("admin")
        .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

    if (templateOwner == null) {
      log.warn("No users found, cannot create dashboard templates");
      return;
    }

    int createdCount = 0;

    // Create each template only if it doesn't exist (idempotent)
    if (!templateExists("Executive Summary")) {
      createExecutiveSummaryTemplate(templateOwner);
      createdCount++;
    }
    if (!templateExists("Developer Dashboard")) {
      createDeveloperDashboardTemplate(templateOwner);
      createdCount++;
    }
    if (!templateExists("Manager Overview")) {
      createManagerOverviewTemplate(templateOwner);
      createdCount++;
    }
    if (!templateExists("QA Dashboard")) {
      createQADashboardTemplate(templateOwner);
      createdCount++;
    }

    if (createdCount > 0) {
      log.info("Created {} dashboard template(s)", createdCount);
    } else {
      log.debug("All dashboard templates already exist, skipping initialization");
    }
  }

  /**
   * Check if a template with the given name already exists. Uses the unique
   * constraint on (user_id, name) indirectly by checking templates.
   */
  private boolean templateExists(String templateName) {
    return customDashboardRepository.findByIsTemplateOrderByTemplateCategoryAsc(true).stream()
        .anyMatch(d -> templateName.equals(d.getName()));
  }

  private void createExecutiveSummaryTemplate(User owner) {
    CustomDashboard dashboard = CustomDashboard.builder().user(owner).name("Executive Summary")
        .description("High-level overview with key metrics, cycle progress, and risk distribution")
        .isTemplate(true).isDefault(false).templateCategory(TemplateCategory.EXECUTIVE)
        .layoutConfig(DEFAULT_LAYOUT_CONFIG).createdBy(owner).build();
    dashboard = customDashboardRepository.save(dashboard);

    // Widget 1: Active Cycles
    createWidget(dashboard, "TABLE", 0, 0, 6, 4, 1,
        "{\"title\":\"Active Cycles\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"CYCLE_SUMMARY\",\"sortBy\":\"startDate\",\"sortOrder\":\"desc\",\"limit\":5}");

    // Widget 2: Team Workload
    createWidget(dashboard, "TABLE", 6, 0, 6, 4, 2,
        "{\"title\":\"Team Workload\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"TEAM_STATS\",\"limit\":5}");

    // Widget 3: Upcoming Deadlines
    createWidget(dashboard, "TABLE", 0, 4, 6, 4, 3,
        "{\"title\":\"Upcoming Deadlines\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"TASK_LIST\",\"sortBy\":\"dueDate\",\"sortOrder\":\"asc\",\"limit\":5,\"filters\":[{\"field\":\"status\",\"operator\":\"not_equals\",\"value\":\"COMPLETED\"}]}");

    // Widget 4: Recent Activity
    createWidget(dashboard, "TABLE", 6, 4, 6, 4, 4,
        "{\"title\":\"Recent Activity\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"PITCH_LIST\",\"sortBy\":\"updatedAt\",\"sortOrder\":\"desc\",\"limit\":5}");

    log.debug("Created Executive Summary template with {} widgets", 4);
  }

  private void createDeveloperDashboardTemplate(User owner) {
    CustomDashboard dashboard = CustomDashboard.builder().user(owner).name("Developer Dashboard")
        .description("Task-focused view with my tasks, team workload, and recent activity").isTemplate(true)
        .isDefault(false).templateCategory(TemplateCategory.DEVELOPER).layoutConfig(DEFAULT_LAYOUT_CONFIG)
        .createdBy(owner).build();
    dashboard = customDashboardRepository.save(dashboard);

    // Widget 1: My Active Tasks - larger size for better visibility
    createWidget(dashboard, "TABLE", 0, 0, 8, 4, 1,
        "{\"title\":\"My Active Tasks\",\"pageSize\":10,\"searchable\":true,\"sourceType\":\"TASK_LIST\",\"sortBy\":\"priority\",\"sortOrder\":\"desc\",\"limit\":10,\"filters\":[{\"field\":\"status\",\"operator\":\"in\",\"value\":[\"TODO\",\"IN_PROGRESS\"]}]}");

    // Widget 2: Recent Activity
    createWidget(dashboard, "TABLE", 8, 0, 4, 4, 2,
        "{\"title\":\"Recent Activity\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"TASK_LIST\",\"sortBy\":\"updatedAt\",\"sortOrder\":\"desc\",\"limit\":5}");

    log.debug("Created Developer Dashboard template with {} widgets", 2);
  }

  private void createManagerOverviewTemplate(User owner) {
    CustomDashboard dashboard = CustomDashboard.builder().user(owner).name("Manager Overview")
        .description("Team performance, cycle health, budget tracking, and delivery metrics").isTemplate(true)
        .isDefault(false).templateCategory(TemplateCategory.MANAGER).layoutConfig(DEFAULT_LAYOUT_CONFIG)
        .createdBy(owner).build();
    dashboard = customDashboardRepository.save(dashboard);

    // Widget 1: Team Workload
    createWidget(dashboard, "TABLE", 0, 0, 6, 4, 1,
        "{\"title\":\"Team Workload\",\"pageSize\":8,\"searchable\":true,\"sourceType\":\"TEAM_STATS\",\"limit\":8}");

    // Widget 2: Cycle Progress
    createWidget(dashboard, "TABLE", 6, 0, 6, 4, 2,
        "{\"title\":\"Cycle Progress\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"CYCLE_SUMMARY\",\"sortBy\":\"startDate\",\"sortOrder\":\"desc\",\"limit\":5}");

    log.debug("Created Manager Overview template with {} widgets", 2);
  }

  private void createQADashboardTemplate(User owner) {
    CustomDashboard dashboard = CustomDashboard.builder().user(owner).name("QA Dashboard")
        .description("Bug tracking, test coverage, and quality metrics").isTemplate(true).isDefault(false)
        .templateCategory(TemplateCategory.QA).layoutConfig(DEFAULT_LAYOUT_CONFIG).createdBy(owner).build();
    dashboard = customDashboardRepository.save(dashboard);

    // Widget 1: My Test Tasks
    createWidget(dashboard, "TABLE", 0, 0, 6, 4, 1,
        "{\"title\":\"My Test Tasks\",\"pageSize\":8,\"searchable\":true,\"sourceType\":\"TASK_LIST\",\"sortBy\":\"priority\",\"sortOrder\":\"desc\",\"limit\":8,\"filters\":[{\"field\":\"category\",\"operator\":\"equals\",\"value\":\"QA\"}]}");

    // Widget 2: Bug Reports
    createWidget(dashboard, "TABLE", 6, 0, 6, 4, 2,
        "{\"title\":\"Bug Reports\",\"pageSize\":5,\"searchable\":false,\"sourceType\":\"BUG_LIST\",\"sortBy\":\"severity\",\"sortOrder\":\"desc\",\"limit\":5}");

    log.debug("Created QA Dashboard template with {} widgets", 2);
  }

  private void createWidget(CustomDashboard dashboard, String widgetType, int posX, int posY, int width, int height,
      int displayOrder, String settings) {
    DashboardWidgetConfig widget = DashboardWidgetConfig.builder().dashboard(dashboard).widgetType(widgetType)
        .positionX(posX).positionY(posY).width(width).height(height).displayOrder(displayOrder)
        .settings(settings).build();
    widgetConfigRepository.save(widget);
  }
}
