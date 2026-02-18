package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture builders for retrospective-related tests.
 * Provides fluent builders for creating test entities with sensible defaults.
 */
public class RetroTestFixtures {

  // ==================== PROJECT FIXTURES ====================

  public static ProjectFixtureBuilder aProject() {
    return new ProjectFixtureBuilder();
  }

  public static class ProjectFixtureBuilder {
    private Long id = 1L;
    private String name = "Test Project";
    private String projectKey = "TST";
    private boolean isActive = true;
    private boolean enableRetrospectives = true;

    public ProjectFixtureBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public ProjectFixtureBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ProjectFixtureBuilder withProjectKey(String key) {
      this.projectKey = key;
      return this;
    }

    public ProjectFixtureBuilder retroEnabled(boolean enabled) {
      this.enableRetrospectives = enabled;
      return this;
    }

    public ProjectFixtureBuilder inactive() {
      this.isActive = false;
      return this;
    }

    public Project build() {
      return Project.builder()
          .id(id)
          .name(name)
          .projectKey(projectKey)
          .isActive(isActive)
          .enableRetrospectives(enableRetrospectives)
          .build();
    }
  }

  // ==================== CYCLE FIXTURES ====================

  public static CycleFixtureBuilder aCycle() {
    return new CycleFixtureBuilder();
  }

  public static class CycleFixtureBuilder {
    private Long id = 1L;
    private String name = "Test Cycle";
    private Project project;
    private LocalDate startDate = LocalDate.now();
    private LocalDate endDate = LocalDate.now().plusWeeks(6);
    private CyclePhase phase = CyclePhase.SHAPING_BUILDING;
    private boolean isActive = true;

    public CycleFixtureBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public CycleFixtureBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public CycleFixtureBuilder withProject(Project project) {
      this.project = project;
      return this;
    }

    public CycleFixtureBuilder withPhase(CyclePhase phase) {
      this.phase = phase;
      return this;
    }

    public CycleFixtureBuilder startingIn(int days) {
      this.startDate = LocalDate.now().plusDays(days);
      this.endDate = this.startDate.plusWeeks(6);
      return this;
    }

    public CycleFixtureBuilder inactive() {
      this.isActive = false;
      return this;
    }

    public Cycle build() {
      return Cycle.builder()
          .id(id)
          .name(name)
          .project(project)
          .startDate(startDate)
          .endDate(endDate)
          .phase(phase)
          .isActive(isActive)
          .build();
    }
  }

  // ==================== USER FIXTURES ====================

  public static UserFixtureBuilder aUser() {
    return new UserFixtureBuilder();
  }

  public static class UserFixtureBuilder {
    private Long id = 1L;
    private String username = "testuser";

    public UserFixtureBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public UserFixtureBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public User build() {
      return User.builder()
          .id(id)
          .username(username)
          .build();
    }
  }

  // ==================== RETROSPECTIVE FIXTURES ====================

  public static RetroFixtureBuilder aRetro() {
    return new RetroFixtureBuilder();
  }

  public static class RetroFixtureBuilder {
    private Long id = 1L;
    private String title = "Test Retro";
    private String notes = "Test notes";
    private RetroStatus status = RetroStatus.DRAFT;
    private Cycle cycle;
    private Project project;
    private User createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime closedAt;
    private List<RetroItem> items = new ArrayList<>();

    public RetroFixtureBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public RetroFixtureBuilder withTitle(String title) {
      this.title = title;
      return this;
    }

    public RetroFixtureBuilder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public RetroFixtureBuilder withStatus(RetroStatus status) {
      this.status = status;
      if (status == RetroStatus.CLOSED) {
        this.closedAt = LocalDateTime.now();
      }
      return this;
    }

    public RetroFixtureBuilder draft() {
      return withStatus(RetroStatus.DRAFT);
    }

    public RetroFixtureBuilder open() {
      return withStatus(RetroStatus.OPEN);
    }

    public RetroFixtureBuilder closed() {
      return withStatus(RetroStatus.CLOSED);
    }

    public RetroFixtureBuilder withCycle(Cycle cycle) {
      this.cycle = cycle;
      return this;
    }

    public RetroFixtureBuilder withProject(Project project) {
      this.project = project;
      return this;
    }

    public RetroFixtureBuilder createdBy(User user) {
      this.createdBy = user;
      return this;
    }

    public RetroFixtureBuilder withItems(List<RetroItem> items) {
      this.items = items;
      return this;
    }

    public Retrospective build() {
      Retrospective retro = Retrospective.builder()
          .id(id)
          .title(title)
          .notes(notes)
          .status(status)
          .cycle(cycle)
          .project(project)
          .createdBy(createdBy)
          .createdAt(createdAt)
          .closedAt(closedAt)
          .build();
      
      // Set items if provided
      if (!items.isEmpty()) {
        retro.setItems(items);
        items.forEach(item -> item.setRetrospective(retro));
      }
      
      return retro;
    }
  }

  // ==================== RETRO ITEM FIXTURES ====================

  public static RetroItemFixtureBuilder aRetroItem() {
    return new RetroItemFixtureBuilder();
  }

  public static class RetroItemFixtureBuilder {
    private Long id = 1L;
    private String content = "Test item";
    private RetroColumnType columnType = RetroColumnType.WENT_WELL;
    private Retrospective retrospective;
    private User author;
    private Boolean isAnonymous = false;
    private Integer voteCount = 0;
    private RetroItem mergedInto;
    private Boolean actedOn = false;
    private String actedOnNotes;
    private LocalDateTime actedOnAt;
    private User actedOnBy;
    private LocalDateTime createdAt = LocalDateTime.now();

    public RetroItemFixtureBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public RetroItemFixtureBuilder withContent(String content) {
      this.content = content;
      return this;
    }

    public RetroItemFixtureBuilder withColumnType(RetroColumnType type) {
      this.columnType = type;
      return this;
    }

    public RetroItemFixtureBuilder wentWell() {
      return withColumnType(RetroColumnType.WENT_WELL);
    }

    public RetroItemFixtureBuilder didntGoWell() {
      return withColumnType(RetroColumnType.DID_NOT_GO_WELL);
    }

    public RetroItemFixtureBuilder action() {
      return withColumnType(RetroColumnType.ACTIONS);
    }

    public RetroItemFixtureBuilder tryNext() {
      return withColumnType(RetroColumnType.TRY_NEXT);
    }

    public RetroItemFixtureBuilder withRetrospective(Retrospective retro) {
      this.retrospective = retro;
      return this;
    }

    public RetroItemFixtureBuilder withAuthor(User author) {
      this.author = author;
      return this;
    }

    public RetroItemFixtureBuilder anonymous() {
      this.isAnonymous = true;
      this.author = null;
      return this;
    }

    public RetroItemFixtureBuilder withVotes(int count) {
      this.voteCount = count;
      return this;
    }

    public RetroItemFixtureBuilder mergedInto(RetroItem target) {
      this.mergedInto = target;
      return this;
    }

    public RetroItemFixtureBuilder actedOn(String notes, User by) {
      this.actedOn = true;
      this.actedOnNotes = notes;
      this.actedOnAt = LocalDateTime.now();
      this.actedOnBy = by;
      return this;
    }

    public RetroItem build() {
      return RetroItem.builder()
          .id(id)
          .content(content)
          .columnType(columnType)
          .retrospective(retrospective)
          .author(author)
          .isAnonymous(isAnonymous)
          .voteCount(voteCount)
          .mergedInto(mergedInto)
          .actedOn(actedOn)
          .actedOnNotes(actedOnNotes)
          .actedOnAt(actedOnAt)
          .actedOnBy(actedOnBy)
          .createdAt(createdAt)
          .build();
    }
  }

  // ==================== PITCH FIXTURES ====================

  public static PitchFixtureBuilder aPitch() {
    return new PitchFixtureBuilder();
  }

  public static class PitchFixtureBuilder {
    private Long id = 1L;
    private String title = "Test Pitch";
    private String description = "Test description";
    private String problemStatement = "Test problem";
    private Integer appetiteDays = 1;
    private Cycle cycle;
    private PitchStatus status = PitchStatus.PENDING;

    public PitchFixtureBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public PitchFixtureBuilder withTitle(String title) {
      this.title = title;
      return this;
    }

    public PitchFixtureBuilder withCycle(Cycle cycle) {
      this.cycle = cycle;
      return this;
    }

    public PitchFixtureBuilder withStatus(PitchStatus status) {
      this.status = status;
      return this;
    }

    public Pitch build() {
      return Pitch.builder()
          .id(id)
          .title(title)
          .description(description)
          .problemStatement(problemStatement)
          .appetiteDays(appetiteDays)
          .cycle(cycle)
          .status(status)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();
    }
  }
}
