package com.github.farzadsedaghatbin.shipflow.repository.slack;

import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackNotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlackNotificationHistoryRepository extends JpaRepository<SlackNotificationHistory, Long> {
    List<SlackNotificationHistory> findBySlackConfigurationIdOrderBySentAtDesc(Long slackConfigId);
    List<SlackNotificationHistory> findByEntityTypeAndEntityId(String entityType, Long entityId);
    List<SlackNotificationHistory> findBySentAtAfter(LocalDateTime after);
    Long countBySuccessFalse();
}
