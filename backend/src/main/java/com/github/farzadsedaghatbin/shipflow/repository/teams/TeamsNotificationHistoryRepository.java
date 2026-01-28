package com.github.farzadsedaghatbin.shipflow.repository.teams;

import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsNotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamsNotificationHistoryRepository extends JpaRepository<TeamsNotificationHistory, Long> {
    
    List<TeamsNotificationHistory> findByTeamsConfigurationIdOrderBySentAtDesc(Long teamsConfigId);
    
    List<TeamsNotificationHistory> findTop50ByTeamsConfigurationIdOrderBySentAtDesc(Long teamsConfigId);
}
