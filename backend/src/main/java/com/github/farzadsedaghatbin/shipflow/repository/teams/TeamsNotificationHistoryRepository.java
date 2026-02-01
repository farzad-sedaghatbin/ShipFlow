package com.github.farzadsedaghatbin.shipflow.repository.teams;

import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsNotificationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamsNotificationHistoryRepository
    extends JpaRepository<TeamsNotificationHistory, Long> {

  List<TeamsNotificationHistory> findByTeamsConfigurationIdOrderBySentAtDesc(Long teamsConfigId);

  List<TeamsNotificationHistory> findTop50ByTeamsConfigurationIdOrderBySentAtDesc(
      Long teamsConfigId);
}
