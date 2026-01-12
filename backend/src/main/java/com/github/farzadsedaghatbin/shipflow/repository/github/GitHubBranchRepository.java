package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitHubBranchRepository extends JpaRepository<GitHubBranch, Long> {
    Optional<GitHubBranch> findByRepositoryIdAndName(Long repositoryId, String name);
    List<GitHubBranch> findByRepositoryId(Long repositoryId);
}
