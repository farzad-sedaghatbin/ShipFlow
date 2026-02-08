package com.github.farzadsedaghatbin.shipflow.service.github;

import com.github.farzadsedaghatbin.shipflow.dto.github.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubPRState;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TaskStatus;
import com.github.farzadsedaghatbin.shipflow.entity.github.*;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GitHubIntegrationService {

  private final GitHubRepositoryRepository githubRepoRepository;
  private final GitHubCommitRepository commitRepository;
  private final GitHubPullRequestRepository pullRequestRepository;
  private final GitHubBranchRepository branchRepository;
  private final TaskGitHubLinkRepository taskLinkRepository;
  private final PitchGitHubLinkRepository pitchLinkRepository;
  private final GitHubConfigurationRepository configurationRepository;
  private final TaskRepository taskRepository;
  private final PitchRepository pitchRepository;
  private final UserRepository userRepository;

  // Patterns to detect task/pitch references in commit messages and PR
  // descriptions
  // Jira-style: PROJ-42, MYAPP-123 (project key + number)
  private static final Pattern JIRA_TASK_PATTERN = Pattern.compile("\\b([A-Z]{2,10})-?(\\d+)\\b");
  // Traditional: Task #42, #T42
  private static final Pattern TASK_PATTERN = Pattern.compile("(?:task|TASK|Task|#T)\\s*#?(\\d+)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern PITCH_PATTERN = Pattern.compile("(?:pitch|PITCH|Pitch|#P)\\s*#?(\\d+)",
      Pattern.CASE_INSENSITIVE);
  // Closes patterns supporting both styles
  private static final Pattern CLOSES_JIRA_PATTERN = Pattern.compile(
      "(?:closes|fixes|resolves|close|fix|resolve)\\s+([A-Z]{2,10})-?(\\d+)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern CLOSES_PATTERN = Pattern.compile(
      "(?:closes|fixes|resolves|close|fix|resolve)\\s+(?:task|TASK|Task|#T)?\\s*#?(\\d+)",
      Pattern.CASE_INSENSITIVE);

  /** Create or update a GitHub repository */
  public GitHubRepository createOrUpdateRepository(CreateGitHubRepositoryRequest request) {
    Optional<GitHubRepository> existingRepo = githubRepoRepository.findByOwnerAndName(request.getOwner(),
        request.getName());

    GitHubRepository repo;
    if (existingRepo.isPresent()) {
      repo = existingRepo.get();
      repo.setUrl(request.getUrl());
      repo.setDefaultBranch(request.getDefaultBranch() != null ? request.getDefaultBranch() : "main");
    } else {
      repo = GitHubRepository.builder().owner(request.getOwner()).name(request.getName())
          .fullName(request.getOwner() + "/" + request.getName()).url(request.getUrl())
          .defaultBranch(request.getDefaultBranch() != null ? request.getDefaultBranch() : "main")
          .isActive(true).build();
    }

    repo = githubRepoRepository.save(repo);

    // Create or update configuration
    createOrUpdateConfiguration(repo, request);

    return repo;
  }

  private void createOrUpdateConfiguration(GitHubRepository repo, CreateGitHubRepositoryRequest request) {
    Optional<GitHubConfiguration> existingConfig = configurationRepository.findByRepositoryId(repo.getId());

    GitHubConfiguration config;
    if (existingConfig.isPresent()) {
      config = existingConfig.get();
    } else {
      config = new GitHubConfiguration();
      config.setRepository(repo);
    }

    if (request.getWebhookSecret() != null) {
      config.setWebhookSecret(request.getWebhookSecret());
    }
    if (request.getAccessToken() != null) {
      config.setAccessToken(request.getAccessToken());
    }
    if (request.getAutoLinkEnabled() != null) {
      config.setAutoLinkEnabled(request.getAutoLinkEnabled());
    }
    if (request.getAutoCloseTasksOnMerge() != null) {
      config.setAutoCloseTasksOnMerge(request.getAutoCloseTasksOnMerge());
    }

    configurationRepository.save(config);
  }

  /** Process a commit from a webhook event */
  public GitHubCommit processCommit(String repositoryFullName, String sha, String message, String authorName,
      String authorEmail, String authorUsername, LocalDateTime commitDate, String branch, String url) {

    GitHubRepository repo = githubRepoRepository.findByFullName(repositoryFullName)
        .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryFullName));

    Optional<GitHubCommit> existingCommit = commitRepository.findBySha(sha);
    if (existingCommit.isPresent()) {
      return existingCommit.get();
    }

    GitHubCommit commit = GitHubCommit.builder().repository(repo).sha(sha).message(message).authorName(authorName)
        .authorEmail(authorEmail).authorUsername(authorUsername).commitDate(commitDate).branch(branch).url(url)
        .build();

    commit = commitRepository.save(commit);

    // Auto-link to tasks and pitches if enabled
    Optional<GitHubConfiguration> config = configurationRepository.findByRepositoryId(repo.getId());
    if (config.isPresent() && config.get().getAutoLinkEnabled()) {
      autoLinkCommit(commit, message);
    }

    return commit;
  }

  /** Process a pull request from a webhook event */
  public GitHubPullRequest processPullRequest(String repositoryFullName, Integer prNumber, String title,
      String description, String state, String headBranch, String baseBranch, String authorUsername, String url,
      LocalDateTime openedAt, LocalDateTime closedAt, LocalDateTime mergedAt, String mergedByUsername) {

    GitHubRepository repo = githubRepoRepository.findByFullName(repositoryFullName)
        .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repositoryFullName));

    Optional<GitHubPullRequest> existingPR = pullRequestRepository.findByRepositoryIdAndPrNumber(repo.getId(),
        prNumber);

    GitHubPullRequest pr;
    GitHubPRState prState = GitHubPRState.valueOf(state.toUpperCase());

    if (existingPR.isPresent()) {
      pr = existingPR.get();
      pr.setTitle(title);
      pr.setDescription(description);
      pr.setState(prState);
      pr.setClosedAt(closedAt);
      pr.setMergedAt(mergedAt);
      pr.setMergedByUsername(mergedByUsername);
    } else {
      pr = GitHubPullRequest.builder().repository(repo).prNumber(prNumber).title(title).description(description)
          .state(prState).headBranch(headBranch).baseBranch(baseBranch).authorUsername(authorUsername)
          .url(url).openedAt(openedAt).closedAt(closedAt).mergedAt(mergedAt)
          .mergedByUsername(mergedByUsername).build();
    }

    pr = pullRequestRepository.save(pr);

    // Auto-link to tasks and pitches if enabled
    Optional<GitHubConfiguration> config = configurationRepository.findByRepositoryId(repo.getId());
    if (config.isPresent() && config.get().getAutoLinkEnabled()) {
      String fullText = title + " " + (description != null ? description : "");
      autoLinkPullRequest(pr, fullText);
    }

    // Auto-close tasks if PR is merged and configuration allows
    if (prState == GitHubPRState.MERGED && config.isPresent() && config.get().getAutoCloseTasksOnMerge()) {
      autoCloseTasksFromPR(pr, title + " " + (description != null ? description : ""));
    }

    return pr;
  }

  /** Auto-link commit to tasks and pitches based on message */
  private void autoLinkCommit(GitHubCommit commit, String message) {
    if (message == null)
      return;

    // Find Jira-style task references (PROJ-42)
    Matcher jiraTaskMatcher = JIRA_TASK_PATTERN.matcher(message);
    while (jiraTaskMatcher.find()) {
      try {
        Long taskId = Long.parseLong(jiraTaskMatcher.group(2));
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent()) {
          linkCommitToTask(commit, task.get(), true);
          log.info("Auto-linked commit {} to task {} (Jira-style)", commit.getSha(),
              jiraTaskMatcher.group(0));
        }
      } catch (NumberFormatException e) {
        log.debug("Not a valid task ID in pattern: {}", jiraTaskMatcher.group(0));
      }
    }

    // Find traditional task references (Task #42)
    Matcher taskMatcher = TASK_PATTERN.matcher(message);
    while (taskMatcher.find()) {
      try {
        Long taskId = Long.parseLong(taskMatcher.group(1));
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent()) {
          linkCommitToTask(commit, task.get(), true);
          log.info("Auto-linked commit {} to task #{}", commit.getSha(), taskId);
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid task ID in commit message: {}", taskMatcher.group(1));
      }
    }

    // Find pitch references
    Matcher pitchMatcher = PITCH_PATTERN.matcher(message);
    while (pitchMatcher.find()) {
      try {
        Long pitchId = Long.parseLong(pitchMatcher.group(1));
        Optional<Pitch> pitch = pitchRepository.findById(pitchId);
        if (pitch.isPresent()) {
          linkCommitToPitch(commit, pitch.get());
          log.info("Auto-linked commit {} to pitch #{}", commit.getSha(), pitchId);
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid pitch ID in commit message: {}", pitchMatcher.group(1));
      }
    }
  }

  /**
   * Auto-link pull request to tasks and pitches based on title and description
   */
  private void autoLinkPullRequest(GitHubPullRequest pr, String text) {
    if (text == null)
      return;

    // Find Jira-style task references (PROJ-42)
    Matcher jiraTaskMatcher = JIRA_TASK_PATTERN.matcher(text);
    while (jiraTaskMatcher.find()) {
      try {
        Long taskId = Long.parseLong(jiraTaskMatcher.group(2));
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent()) {
          linkPullRequestToTask(pr, task.get(), true);
          log.info("Auto-linked PR #{} to task {} (Jira-style)", pr.getPrNumber(), jiraTaskMatcher.group(0));
        }
      } catch (NumberFormatException e) {
        log.debug("Not a valid task ID in pattern: {}", jiraTaskMatcher.group(0));
      }
    }

    // Find traditional task references (Task #42)
    Matcher taskMatcher = TASK_PATTERN.matcher(text);
    while (taskMatcher.find()) {
      try {
        Long taskId = Long.parseLong(taskMatcher.group(1));
        Optional<Task> task = taskRepository.findById(taskId);
        if (task.isPresent()) {
          linkPullRequestToTask(pr, task.get(), true);
          log.info("Auto-linked PR #{} to task #{}", pr.getPrNumber(), taskId);
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid task ID in PR: {}", taskMatcher.group(1));
      }
    }

    // Find pitch references
    Matcher pitchMatcher = PITCH_PATTERN.matcher(text);
    while (pitchMatcher.find()) {
      try {
        Long pitchId = Long.parseLong(pitchMatcher.group(1));
        Optional<Pitch> pitch = pitchRepository.findById(pitchId);
        if (pitch.isPresent()) {
          linkPullRequestToPitch(pr, pitch.get());
          log.info("Auto-linked PR #{} to pitch #{}", pr.getPrNumber(), pitchId);
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid pitch ID in PR: {}", pitchMatcher.group(1));
      }
    }
  }

  /**
   * Auto-close tasks when PR is merged if "closes #123" or "closes PROJ-42"
   * keywords are used
   */
  private void autoCloseTasksFromPR(GitHubPullRequest pr, String text) {
    if (text == null)
      return;

    // Check Jira-style closes patterns (closes PROJ-42)
    Matcher closesJiraMatcher = CLOSES_JIRA_PATTERN.matcher(text);
    while (closesJiraMatcher.find()) {
      try {
        Long taskId = Long.parseLong(closesJiraMatcher.group(2));
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
          Task task = taskOpt.get();
          if (task.getStatus() != TaskStatus.DONE) {
            task.setStatus(TaskStatus.DONE);
            task.setCompletedAt(pr.getMergedAt());
            taskRepository.save(task);
            log.info("Auto-closed task {} due to merged PR #{} (Jira-style)", closesJiraMatcher.group(0),
                pr.getPrNumber());
          }
        }
      } catch (NumberFormatException e) {
        log.debug("Not a valid task ID in closes pattern: {}", closesJiraMatcher.group(0));
      }
    }

    // Check traditional closes patterns (closes #123)
    Matcher closesMatcher = CLOSES_PATTERN.matcher(text);
    while (closesMatcher.find()) {
      try {
        Long taskId = Long.parseLong(closesMatcher.group(1));
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
          Task task = taskOpt.get();
          if (task.getStatus() != TaskStatus.DONE) {
            task.setStatus(TaskStatus.DONE);
            task.setCompletedAt(pr.getMergedAt());
            taskRepository.save(task);
            log.info("Auto-closed task #{} due to merged PR #{}", taskId, pr.getPrNumber());
          }
        }
      } catch (NumberFormatException e) {
        log.warn("Invalid task ID in closes statement: {}", closesMatcher.group(1));
      }
    }
  }

  /** Link a commit to a task */
  public void linkCommitToTask(GitHubCommit commit, Task task, boolean autoLinked) {
    // Check if link already exists
    List<TaskGitHubLink> existingLinks = taskLinkRepository.findByTaskId(task.getId());
    boolean alreadyLinked = existingLinks.stream()
        .anyMatch(link -> link.getCommit() != null && link.getCommit().getId().equals(commit.getId()));

    if (alreadyLinked) {
      return;
    }

    TaskGitHubLink link = TaskGitHubLink.builder().task(task).linkType(GitHubLinkType.COMMIT).commit(commit)
        .autoLinked(autoLinked).build();

    taskLinkRepository.save(link);
  }

  /** Link a commit to a pitch */
  public void linkCommitToPitch(GitHubCommit commit, Pitch pitch) {
    // Check if link already exists
    List<PitchGitHubLink> existingLinks = pitchLinkRepository.findByPitchId(pitch.getId());
    boolean alreadyLinked = existingLinks.stream()
        .anyMatch(link -> link.getCommit() != null && link.getCommit().getId().equals(commit.getId()));

    if (alreadyLinked) {
      return;
    }

    PitchGitHubLink link = PitchGitHubLink.builder().pitch(pitch).linkType(GitHubLinkType.COMMIT).commit(commit)
        .build();

    pitchLinkRepository.save(link);
  }

  /** Link a pull request to a task */
  public void linkPullRequestToTask(GitHubPullRequest pr, Task task, boolean autoLinked) {
    // Check if link already exists
    List<TaskGitHubLink> existingLinks = taskLinkRepository.findByTaskId(task.getId());
    boolean alreadyLinked = existingLinks.stream()
        .anyMatch(link -> link.getPullRequest() != null && link.getPullRequest().getId().equals(pr.getId()));

    if (alreadyLinked) {
      return;
    }

    TaskGitHubLink link = TaskGitHubLink.builder().task(task).linkType(GitHubLinkType.PULL_REQUEST).pullRequest(pr)
        .autoLinked(autoLinked).build();

    taskLinkRepository.save(link);
  }

  /** Link a pull request to a pitch */
  public void linkPullRequestToPitch(GitHubPullRequest pr, Pitch pitch) {
    // Check if link already exists
    List<PitchGitHubLink> existingLinks = pitchLinkRepository.findByPitchId(pitch.getId());
    boolean alreadyLinked = existingLinks.stream()
        .anyMatch(link -> link.getPullRequest() != null && link.getPullRequest().getId().equals(pr.getId()));

    if (alreadyLinked) {
      return;
    }

    PitchGitHubLink link = PitchGitHubLink.builder().pitch(pitch).linkType(GitHubLinkType.PULL_REQUEST)
        .pullRequest(pr).build();

    pitchLinkRepository.save(link);
  }

  /** Get all GitHub links for a task */
  public List<GitHubLinkDTO> getTaskGitHubLinks(Long taskId) {
    List<TaskGitHubLink> links = taskLinkRepository.findByTaskId(taskId);
    return links.stream().map(this::convertTaskLinkToDTO).collect(Collectors.toList());
  }

  /** Get all GitHub links for a pitch */
  public List<GitHubLinkDTO> getPitchGitHubLinks(Long pitchId) {
    List<PitchGitHubLink> links = pitchLinkRepository.findByPitchId(pitchId);
    return links.stream().map(this::convertPitchLinkToDTO).collect(Collectors.toList());
  }

  /** Get all repositories */
  public List<GitHubRepositoryDTO> getAllRepositories() {
    return githubRepoRepository.findAll().stream().map(this::convertRepoToDTO).collect(Collectors.toList());
  }

  private GitHubLinkDTO convertTaskLinkToDTO(TaskGitHubLink link) {
    return GitHubLinkDTO.builder().id(link.getId()).linkType(link.getLinkType())
        .commit(link.getCommit() != null ? convertCommitToDTO(link.getCommit()) : null)
        .pullRequest(link.getPullRequest() != null ? convertPRToDTO(link.getPullRequest()) : null)
        .branch(link.getBranch() != null ? convertBranchToDTO(link.getBranch()) : null)
        .linkedAt(link.getLinkedAt())
        .linkedByUsername(link.getLinkedByUser() != null ? link.getLinkedByUser().getUsername() : null)
        .autoLinked(link.getAutoLinked()).build();
  }

  private GitHubLinkDTO convertPitchLinkToDTO(PitchGitHubLink link) {
    return GitHubLinkDTO.builder().id(link.getId()).linkType(link.getLinkType())
        .commit(link.getCommit() != null ? convertCommitToDTO(link.getCommit()) : null)
        .pullRequest(link.getPullRequest() != null ? convertPRToDTO(link.getPullRequest()) : null)
        .branch(link.getBranch() != null ? convertBranchToDTO(link.getBranch()) : null)
        .linkedAt(link.getLinkedAt())
        .linkedByUsername(link.getLinkedByUser() != null ? link.getLinkedByUser().getUsername() : null).build();
  }

  private GitHubCommitDTO convertCommitToDTO(GitHubCommit commit) {
    return GitHubCommitDTO.builder().id(commit.getId()).sha(commit.getSha()).message(commit.getMessage())
        .authorName(commit.getAuthorName()).authorEmail(commit.getAuthorEmail())
        .authorUsername(commit.getAuthorUsername()).commitDate(commit.getCommitDate())
        .branch(commit.getBranch()).url(commit.getUrl())
        .repositoryFullName(commit.getRepository().getFullName()).build();
  }

  private GitHubPullRequestDTO convertPRToDTO(GitHubPullRequest pr) {
    return GitHubPullRequestDTO.builder().id(pr.getId()).prNumber(pr.getPrNumber()).title(pr.getTitle())
        .description(pr.getDescription()).state(pr.getState()).headBranch(pr.getHeadBranch())
        .baseBranch(pr.getBaseBranch()).authorUsername(pr.getAuthorUsername()).url(pr.getUrl())
        .openedAt(pr.getOpenedAt()).closedAt(pr.getClosedAt()).mergedAt(pr.getMergedAt())
        .mergedByUsername(pr.getMergedByUsername()).repositoryFullName(pr.getRepository().getFullName())
        .build();
  }

  private GitHubBranchDTO convertBranchToDTO(GitHubBranch branch) {
    return GitHubBranchDTO.builder().id(branch.getId()).name(branch.getName()).headSha(branch.getHeadSha())
        .isDefault(branch.getIsDefault()).repositoryFullName(branch.getRepository().getFullName()).build();
  }

  private GitHubRepositoryDTO convertRepoToDTO(GitHubRepository repo) {
    return GitHubRepositoryDTO.builder().id(repo.getId()).owner(repo.getOwner()).name(repo.getName())
        .fullName(repo.getFullName()).url(repo.getUrl()).defaultBranch(repo.getDefaultBranch())
        .isActive(repo.getIsActive()).build();
  }
}
