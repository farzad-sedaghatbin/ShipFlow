package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.McpUsageLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface McpUsageLogRepository extends JpaRepository<McpUsageLog, Long> {

  long countByCalledAtAfter(LocalDateTime since);

  long countBySuccessTrueAndCalledAtAfter(LocalDateTime since);

  @Query("SELECT COUNT(DISTINCT l.user.id) FROM McpUsageLog l WHERE l.calledAt >= :since")
  long countDistinctUsersSince(@Param("since") LocalDateTime since);

  @Query("SELECT COUNT(DISTINCT l.toolName) FROM McpUsageLog l WHERE l.calledAt >= :since")
  long countDistinctToolsSince(@Param("since") LocalDateTime since);

  // Per-user aggregates: [username, email, totalCalls, successCalls, lastCalledAt]
  @Query(
      """
      SELECT l.user.username, l.user.email,
             COUNT(l.id), SUM(CASE WHEN l.success = TRUE THEN 1 ELSE 0 END),
             MAX(l.calledAt)
      FROM McpUsageLog l
      WHERE l.calledAt >= :since
      GROUP BY l.user.id, l.user.username, l.user.email
      ORDER BY COUNT(l.id) DESC
      """)
  List<Object[]> findUserUsageAggregatesSince(@Param("since") LocalDateTime since);

  // Per-tool aggregates: [toolName, totalCalls, successCalls, distinctUsers]
  @Query(
      """
      SELECT l.toolName,
             COUNT(l.id), SUM(CASE WHEN l.success = TRUE THEN 1 ELSE 0 END),
             COUNT(DISTINCT l.user.id)
      FROM McpUsageLog l
      WHERE l.calledAt >= :since
      GROUP BY l.toolName
      ORDER BY COUNT(l.id) DESC
      """)
  List<Object[]> findToolUsageAggregatesSince(@Param("since") LocalDateTime since);

  // Daily call counts: [date, totalCalls, successCalls]
  @Query(
      """
      SELECT FUNCTION('date_trunc', 'day', l.calledAt),
             COUNT(l.id), SUM(CASE WHEN l.success = TRUE THEN 1 ELSE 0 END)
      FROM McpUsageLog l
      WHERE l.calledAt >= :since
      GROUP BY FUNCTION('date_trunc', 'day', l.calledAt)
      ORDER BY FUNCTION('date_trunc', 'day', l.calledAt) ASC
      """)
  List<Object[]> findDailyCallsSince(@Param("since") LocalDateTime since);

  List<McpUsageLog> findAllByOrderByCalledAtDesc(Pageable pageable);
}
