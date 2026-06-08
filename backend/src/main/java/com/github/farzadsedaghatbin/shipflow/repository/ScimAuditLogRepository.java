package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ScimAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for SCIM provisioning audit log entries. */
@Repository
public interface ScimAuditLogRepository extends JpaRepository<ScimAuditLog, Long> {

  /** Returns the 50 most recent audit events, newest first. */
  List<ScimAuditLog> findTop50ByOrderByOccurredAtDesc();
}
