package com.github.farzadsedaghatbin.shipflow.repository.slack;

import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackChannelConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlackChannelConfigRepository extends JpaRepository<SlackChannelConfig, Long> {
  List<SlackChannelConfig> findBySlackConfigurationId(Long slackConfigId);

  Optional<SlackChannelConfig> findBySlackConfigurationIdAndChannelName(
      Long slackConfigId, String channelName);
}
