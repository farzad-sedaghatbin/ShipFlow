package com.github.farzadsedaghatbin.shipflow.repository.github;

import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubWebhookEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubWebhookEventRepository extends JpaRepository<GitHubWebhookEvent, Long> {
  List<GitHubWebhookEvent> findByProcessed(Boolean processed);

  List<GitHubWebhookEvent> findByProcessedAndCreatedAtBefore(Boolean processed, LocalDateTime before);

  List<GitHubWebhookEvent> findByRepositoryFullNameAndProcessed(String repositoryFullName, Boolean processed);
}
