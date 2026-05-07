package com.github.farzadsedaghatbin.shipflow.entity.audit;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Listener that captures the current user's username when a revision is
 * created. This is called by Hibernate Envers whenever an audited entity is
 * modified.
 */
public class AuditRevisionListener implements RevisionListener {

  @Override
  public void newRevision(Object revisionEntity) {
    AuditRevisionEntity auditRevisionEntity = (AuditRevisionEntity) revisionEntity;

    String username = getCurrentUsername();
    auditRevisionEntity.setModifiedBy(username);
  }

  /**
   * Retrieves the current username from the Spring Security context. Returns
   * "system" if no authentication is present (e.g., for scheduled tasks).
   */
  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      return "system";
    }

    String principal = authentication.getName();
    if (principal == null || principal.equals("anonymousUser")) {
      return "system";
    }

    return principal;
  }
}
