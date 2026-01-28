package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.DashboardNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for dashboard notifications.
 */
@Repository
public interface DashboardNotificationRepository extends JpaRepository<DashboardNotification, Long> {

    /**
     * Find all notifications for a user, ordered by creation date descending
     */
    List<DashboardNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find unread notifications for a user
     */
    List<DashboardNotification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead);

    /**
     * Count unread notifications for a user
     */
    Long countByUserIdAndIsRead(Long userId, Boolean isRead);

    /**
     * Find notifications by type for a user
     */
    List<DashboardNotification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    /**
     * Find non-expired notifications for a user
     */
    @Query("SELECT n FROM DashboardNotification n WHERE n.user.id = :userId " +
           "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
           "ORDER BY n.createdAt DESC")
    List<DashboardNotification> findActiveNotifications(@Param("userId") Long userId, 
                                                         @Param("now") LocalDateTime now);

    /**
     * Find expired notifications
     */
    @Query("SELECT n FROM DashboardNotification n WHERE n.expiresAt IS NOT NULL " +
           "AND n.expiresAt <= :now")
    List<DashboardNotification> findExpiredNotifications(@Param("now") LocalDateTime now);

    /**
     * Delete old read notifications
     */
    @Query("DELETE FROM DashboardNotification n WHERE n.isRead = true " +
           "AND n.readAt < :cutoffDate")
    void deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);
}
