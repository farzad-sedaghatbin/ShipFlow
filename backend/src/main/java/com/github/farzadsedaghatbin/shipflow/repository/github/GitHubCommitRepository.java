package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitHubCommitRepository extends JpaRepository<GitHubCommit, Long> {
    Optional<GitHubCommit> findBySha(String sha);
    List<GitHubCommit> findByRepositoryId(Long repositoryId);
    List<GitHubCommit> findByRepositoryIdAndBranch(Long repositoryId, String branch);
}
