package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GitHubConfigurationRepository extends JpaRepository<GitHubConfiguration, Long> {
    Optional<GitHubConfiguration> findByRepositoryId(Long repositoryId);
}
