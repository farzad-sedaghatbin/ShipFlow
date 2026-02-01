package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubPRState;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubPullRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubPullRequestRepository extends JpaRepository<GitHubPullRequest, Long> {
  Optional<GitHubPullRequest> findByRepositoryIdAndPrNumber(Long repositoryId, Integer prNumber);

  List<GitHubPullRequest> findByRepositoryId(Long repositoryId);

  List<GitHubPullRequest> findByRepositoryIdAndState(Long repositoryId, GitHubPRState state);
}
