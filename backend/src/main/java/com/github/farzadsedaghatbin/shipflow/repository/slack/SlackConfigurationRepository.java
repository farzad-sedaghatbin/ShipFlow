package com.github.farzadsedaghatbin.shipflow.repository.slack;

import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlackConfigurationRepository extends JpaRepository<SlackConfiguration, Long> {
    Optional<SlackConfiguration> findByWorkspaceName(String workspaceName);
    Optional<SlackConfiguration> findFirstByIsEnabledTrue();
}
