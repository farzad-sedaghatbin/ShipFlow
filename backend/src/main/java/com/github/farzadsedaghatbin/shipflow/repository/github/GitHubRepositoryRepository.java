package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, Long> {
    Optional<GitHubRepository> findByOwnerAndName(String owner, String name);
    Optional<GitHubRepository> findByFullName(String fullName);
}
