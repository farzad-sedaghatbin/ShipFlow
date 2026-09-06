package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.AuthAuditLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditOutcome;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, Long> {

  /**
   * Filtered, newest-first listing. Every filter is optional — a null argument
   * matches everything — so one query backs the whole admin view.
   */
  @Query("""
      SELECT a FROM AuthAuditLog a
      WHERE (:username IS NULL OR LOWER(a.username) = LOWER(:username))
        AND (:ipAddress IS NULL OR a.ipAddress = :ipAddress)
        AND (:outcome IS NULL OR a.outcome = :outcome)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
      ORDER BY a.createdAt DESC
      """)
  Page<AuthAuditLog> search(@Param("username") String username, @Param("ipAddress") String ipAddress,
      @Param("outcome") AuthAuditOutcome outcome, @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to, Pageable pageable);

  /** Failed attempts for an account since a cut-off — brute-force detection. */
  long countByUsernameIgnoreCaseAndOutcomeAndCreatedAtAfter(String username, AuthAuditOutcome outcome,
      OffsetDateTime after);
}
