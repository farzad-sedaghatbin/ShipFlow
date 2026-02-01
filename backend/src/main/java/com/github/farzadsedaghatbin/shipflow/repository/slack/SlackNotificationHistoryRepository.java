package com.github.farzadsedaghatbin.shipflow.repository.slack;

import com.github.farzadsedaghatbin.shipflow.entity.slack.SlackNotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SlackNotificationHistoryRepository extends JpaRepository<SlackNotificationHistory, Long> {
    @Query("SELECT h FROM SlackNotificationHistory h LEFT JOIN FETCH h.slackConfiguration WHERE h.slackConfiguration.id = :slackConfigId ORDER BY h.sentAt DESC")
    List<SlackNotificationHistory> findBySlackConfigurationIdOrderBySentAtDesc(@Param("slackConfigId") Long slackConfigId);
    
    @Query("SELECT h FROM SlackNotificationHistory h LEFT JOIN FETCH h.slackConfiguration WHERE h.entityType = :entityType AND h.entityId = :entityId")
    List<SlackNotificationHistory> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    @Query("SELECT h FROM SlackNotificationHistory h LEFT JOIN FETCH h.slackConfiguration WHERE h.sentAt > :after")
    List<SlackNotificationHistory> findBySentAtAfter(@Param("after") LocalDateTime after);
    
    Long countBySuccessFalse();
}
