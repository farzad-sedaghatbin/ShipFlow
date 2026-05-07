package com.github.farzadsedaghatbin.shipflow.repository.slack;

import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlackConfigurationRepository extends JpaRepository<SlackConfiguration, Long> {
  Optional<SlackConfiguration> findByWorkspaceName(String workspaceName);

  Optional<SlackConfiguration> findFirstByIsEnabledTrue();
}
