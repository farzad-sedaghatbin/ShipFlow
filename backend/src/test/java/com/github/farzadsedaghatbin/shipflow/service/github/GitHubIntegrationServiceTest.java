package com.github.farzadsedaghatbin.shipflow.service.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskCategory;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskPriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.github.*;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for GitHubIntegrationService Tests pattern matching, auto-linking,
 * and auto-closing functionality
 */
@ExtendWith(MockitoExtension.class)
class GitHubIntegrationServiceTest {

  @Mock
  private GitHubRepositoryRepository githubRepoRepository;

  @Mock
  private GitHubCommitRepository commitRepository;

  @Mock
  private GitHubPullRequestRepository pullRequestRepository;

  @Mock
  private TaskGitHubLinkRepository taskLinkRepository;

  @Mock
  private PitchGitHubLinkRepository pitchLinkRepository;

  @Mock
  private GitHubConfigurationRepository configurationRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private PitchRepository pitchRepository;

  @InjectMocks
  private GitHubIntegrationService service;

  private GitHubRepository testRepo;
  private Task testTask;
  private GitHubConfiguration testConfig;
  private Cycle testCycle;
  private Project testProject;

  @BeforeEach
  void setUp() {
    // Setup test repository
    testRepo = GitHubRepository.builder().id(1L).owner("testorg").name("testrepo").fullName("testorg/testrepo")
        .defaultBranch("main").isActive(true).build();

    // Setup test project and cycle
    testProject = Project.builder().id(1L).name("Test Project").projectKey("TEST").build();

    testCycle = Cycle.builder().id(1L).name("Test Cycle").project(testProject).build();

    // Setup test task
    testTask = Task.builder().id(42L).title("Test Task").description("Test description")
        .status(TaskStatus.IN_PROGRESS).priority(TaskPriority.MEDIUM).category(TaskCategory.PITCH_SCOPE)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    // Setup test configuration
    testConfig = GitHubConfiguration.builder().id(1L).repository(testRepo).autoLinkEnabled(true)
        .autoCloseTasksOnMerge(true).build();
  }

  @Test
  void processCommit_WithJiraStyleReference_ShouldAutoLinkToTask() {
    // Given
    String commitMessage = "PROJ-42: Fixed login bug";
    String sha = "abc123def456";

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(commitRepository.findBySha(sha)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(taskRepository.findById(42L)).thenReturn(Optional.of(testTask));
    when(taskLinkRepository.findByTaskId(42L)).thenReturn(List.of());
    when(commitRepository.save(any(GitHubCommit.class))).thenAnswer(invocation -> {
      GitHubCommit commit = invocation.getArgument(0);
      commit.setId(1L);
      return commit;
    });

    // When
    GitHubCommit result = service.processCommitAndReturn("testorg/testrepo", sha, commitMessage, "John Doe",
        "john@example.com", "johndoe", LocalDateTime.now(), "main",
        "https://github.com/testorg/testrepo/commit/abc123");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getMessage()).isEqualTo(commitMessage);
    verify(taskLinkRepository).save(argThat(link -> link.getTask().getId().equals(42L)
        && link.getLinkType() == GitHubLinkType.COMMIT && link.getAutoLinked()));
  }

  @Test
  void processCommit_WithTraditionalStyleReference_ShouldAutoLinkToTask() {
    // Given
    String commitMessage = "Task #42: Fixed login bug";
    String sha = "abc123def456";

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(commitRepository.findBySha(sha)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(taskRepository.findById(42L)).thenReturn(Optional.of(testTask));
    when(taskLinkRepository.findByTaskId(42L)).thenReturn(List.of());
    when(commitRepository.save(any(GitHubCommit.class))).thenAnswer(invocation -> {
      GitHubCommit commit = invocation.getArgument(0);
      commit.setId(1L);
      return commit;
    });

    // When
    GitHubCommit result = service.processCommitAndReturn("testorg/testrepo", sha, commitMessage, "John Doe",
        "john@example.com", "johndoe", LocalDateTime.now(), "main",
        "https://github.com/testorg/testrepo/commit/abc123");

    // Then
    assertThat(result).isNotNull();
    verify(taskLinkRepository).save(argThat(link -> link.getTask().getId().equals(42L) && link.getAutoLinked()));
  }

  @Test
  void processCommit_WithMultipleTaskReferences_ShouldLinkToAll() {
    // Given
    String commitMessage = "PROJ-42 and MYAPP-100: Fixed bugs";
    String sha = "abc123def456";

    Task testTask2 = Task.builder().id(100L).title("Another Task").status(TaskStatus.TODO)
        .priority(TaskPriority.HIGH).category(TaskCategory.PITCH_SCOPE).cycle(testCycle)
        .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(commitRepository.findBySha(sha)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(taskRepository.findById(42L)).thenReturn(Optional.of(testTask));
    when(taskRepository.findById(100L)).thenReturn(Optional.of(testTask2));
    when(taskLinkRepository.findByTaskId(anyLong())).thenReturn(List.of());
    when(commitRepository.save(any(GitHubCommit.class))).thenAnswer(invocation -> {
      GitHubCommit commit = invocation.getArgument(0);
      commit.setId(1L);
      return commit;
    });

    // When
    service.processCommit("testorg/testrepo", sha, commitMessage, "John Doe", "john@example.com", "johndoe",
        LocalDateTime.now(), "main", "https://github.com/testorg/testrepo/commit/abc123");

    // Then
    verify(taskLinkRepository, times(2)).save(any(TaskGitHubLink.class));
  }

  @Test
  void processPullRequest_WithClosesKeyword_ShouldAutoCloseTaskWhenMerged() {
    // Given
    String prTitle = "PROJ-42: Fix login bug";
    String prDescription = "Closes PROJ-42\n\nThis fixes the login issue.";
    LocalDateTime mergedAt = LocalDateTime.now();

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(pullRequestRepository.findByRepositoryIdAndPrNumber(1L, 123)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(taskRepository.findById(42L)).thenReturn(Optional.of(testTask));
    when(taskLinkRepository.findByTaskId(42L)).thenReturn(List.of());
    when(pullRequestRepository.save(any(GitHubPullRequest.class))).thenAnswer(invocation -> {
      GitHubPullRequest pr = invocation.getArgument(0);
      pr.setId(1L);
      return pr;
    });

    // When
    service.processPullRequest("testorg/testrepo", 123, prTitle, prDescription, "MERGED", "feature/proj-42", "main",
        "johndoe", "https://github.com/testorg/testrepo/pull/123", LocalDateTime.now().minusDays(1), mergedAt,
        mergedAt, "janedoe");

    // Then
    verify(taskRepository).save(argThat(task -> task.getId().equals(42L) && task.getStatus() == TaskStatus.DONE
        && task.getCompletedAt().equals(mergedAt)));
  }

  @Test
  void processPullRequest_WithClosesKeywordButNotMerged_ShouldNotCloseTask() {
    // Given
    String prTitle = "PROJ-42: Fix login bug";
    String prDescription = "Closes PROJ-42";

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(pullRequestRepository.findByRepositoryIdAndPrNumber(1L, 123)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(taskRepository.findById(42L)).thenReturn(Optional.of(testTask));
    when(taskLinkRepository.findByTaskId(42L)).thenReturn(List.of());
    when(pullRequestRepository.save(any(GitHubPullRequest.class))).thenAnswer(invocation -> {
      GitHubPullRequest pr = invocation.getArgument(0);
      pr.setId(1L);
      return pr;
    });

    // When - PR is OPEN, not MERGED
    service.processPullRequest("testorg/testrepo", 123, prTitle, prDescription, "OPEN", "feature/proj-42", "main",
        "johndoe", "https://github.com/testorg/testrepo/pull/123", LocalDateTime.now(), null, null, null);

    // Then - Task should NOT be closed
    verify(taskRepository, never()).save(any(Task.class));
  }

  @Test
  void processPullRequest_WithAutoLinkDisabled_ShouldNotAutoLink() {
    // Given
    testConfig.setAutoLinkEnabled(false);
    String prTitle = "PROJ-42: Fix login bug";

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(pullRequestRepository.findByRepositoryIdAndPrNumber(1L, 123)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(pullRequestRepository.save(any(GitHubPullRequest.class))).thenAnswer(invocation -> {
      GitHubPullRequest pr = invocation.getArgument(0);
      pr.setId(1L);
      return pr;
    });

    // When
    service.processPullRequest("testorg/testrepo", 123, prTitle, null, "OPEN", "feature/proj-42", "main", "johndoe",
        "https://github.com/testorg/testrepo/pull/123", LocalDateTime.now(), null, null, null);

    // Then
    verify(taskRepository, never()).findById(anyLong());
    verify(taskLinkRepository, never()).save(any(TaskGitHubLink.class));
  }

  @Test
  void processPullRequest_WithAutoCloseDisabled_ShouldNotAutoClose() {
    // Given
    testConfig.setAutoCloseTasksOnMerge(false);
    String prDescription = "Closes PROJ-42";

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(pullRequestRepository.findByRepositoryIdAndPrNumber(1L, 123)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(pullRequestRepository.save(any(GitHubPullRequest.class))).thenAnswer(invocation -> {
      GitHubPullRequest pr = invocation.getArgument(0);
      pr.setId(1L);
      return pr;
    });

    // When
    service.processPullRequest("testorg/testrepo", 123, "Fix bug", prDescription, "MERGED", "feature/fix", "main",
        "johndoe", "https://github.com/testorg/testrepo/pull/123", LocalDateTime.now().minusDays(1),
        LocalDateTime.now(), LocalDateTime.now(), "janedoe");

    // Then
    verify(taskRepository, never()).save(any(Task.class));
  }

  @Test
  void getTaskGitHubLinks_ShouldReturnAllLinksForTask() {
    // Given
    GitHubCommit commit = GitHubCommit.builder().id(1L).repository(testRepo).sha("abc123")
        .message("PROJ-42: Test commit").commitDate(LocalDateTime.now()).build();

    TaskGitHubLink link = TaskGitHubLink.builder().id(1L).task(testTask).linkType(GitHubLinkType.COMMIT)
        .commit(commit).autoLinked(true).linkedAt(LocalDateTime.now()).build();

    when(taskLinkRepository.findByTaskId(42L)).thenReturn(List.of(link));

    // When
    var result = service.getTaskGitHubLinks(42L);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLinkType()).isEqualTo(GitHubLinkType.COMMIT);
    assertThat(result.get(0).getAutoLinked()).isTrue();
  }

  @Test
  void processCommit_WhenExistingCommit_ShouldReturnExisting() {
    // Given
    String sha = "abc123";
    GitHubCommit existingCommit = GitHubCommit.builder().id(1L).repository(testRepo).sha(sha).message("Test")
        .commitDate(LocalDateTime.now()).build();

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(commitRepository.findBySha(sha)).thenReturn(Optional.of(existingCommit));

    // When
    GitHubCommit result = service.processCommitAndReturn("testorg/testrepo", sha, "Test message", "John Doe",
        "john@example.com", "johndoe", LocalDateTime.now(), "main", "https://github.com/test/commit/abc123");

    // Then
    assertThat(result.getId()).isEqualTo(1L);
    verify(commitRepository, never()).save(any(GitHubCommit.class));
  }

  @Test
  void processCommit_WithNoTaskReference_ShouldNotCreateLink() {
    // Given
    String commitMessage = "Fixed a bug without task reference";
    String sha = "abc123def456";

    when(githubRepoRepository.findByFullName("testorg/testrepo")).thenReturn(Optional.of(testRepo));
    when(commitRepository.findBySha(sha)).thenReturn(Optional.empty());
    when(configurationRepository.findByRepositoryId(1L)).thenReturn(Optional.of(testConfig));
    when(commitRepository.save(any(GitHubCommit.class))).thenAnswer(invocation -> {
      GitHubCommit commit = invocation.getArgument(0);
      commit.setId(1L);
      return commit;
    });

    // When
    service.processCommit("testorg/testrepo", sha, commitMessage, "John Doe", "john@example.com", "johndoe",
        LocalDateTime.now(), "main", "https://github.com/testorg/testrepo/commit/abc123");

    // Then
    verify(taskLinkRepository, never()).save(any(TaskGitHubLink.class));
  }
}
