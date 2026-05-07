package com.github.farzadsedaghatbin.shipflow.repository.teams;

import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsChannelConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamsChannelConfigRepository extends JpaRepository<TeamsChannelConfig, Long> {

  List<TeamsChannelConfig> findByTeamsConfigurationId(Long teamsConfigId);

  Optional<TeamsChannelConfig> findByTeamsConfigurationIdAndChannelName(Long teamsConfigId, String channelName);
}
