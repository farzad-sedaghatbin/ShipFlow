package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import org.springframework.stereotype.Component;

/**
 * Permission gate for {@link KnowledgeSource} operations.
 *
 * <p>This is a placeholder that allows all callers; Task 11 swaps in real role/membership checks
 * against {@code PERMISSION_MATRIX.md}. Keeping the surface defined here lets the service layer be
 * wired correctly today without a forward dependency on the permission module.
 */
@Component
public class KnowledgeSourceAccessChecker {

  public void assertCanCreate(KnowledgeSourceScope scope, Long teamId, Long projectId, Long userId) {
    // Task 11: enforce role + scope ownership.
  }

  public void assertCanListOrg(Long userId) {
    // Task 11: any authenticated user may list ORG-scope sources.
  }

  public void assertCanListTeam(Long teamId, Long userId) {
    // Task 11: verify membership in the team.
  }

  public void assertCanListProject(Long projectId, Long userId) {
    // Task 11: verify membership in the project.
  }

  public void assertCanModify(KnowledgeSource src, Long userId) {
    // Task 11: only creator / scope owner / admin may modify.
  }
}
