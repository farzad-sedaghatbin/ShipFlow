package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import com.github.farzadsedaghatbin.shipflow.entity.github.TaskGitHubLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskGitHubLinkRepository extends JpaRepository<TaskGitHubLink, Long> {
  List<TaskGitHubLink> findByTaskId(Long taskId);

  List<TaskGitHubLink> findByTaskIdAndLinkType(Long taskId, GitHubLinkType linkType);

  List<TaskGitHubLink> findByCommitId(Long commitId);

  List<TaskGitHubLink> findByPullRequestId(Long pullRequestId);

  List<TaskGitHubLink> findByBranchId(Long branchId);

  List<TaskGitHubLink> findByAutoLinked(Boolean autoLinked);
}
