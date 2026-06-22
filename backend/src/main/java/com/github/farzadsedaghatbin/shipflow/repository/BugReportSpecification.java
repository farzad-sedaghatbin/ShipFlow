package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** JPA Criteria-based specifications for BugReport filtering. */
public final class BugReportSpecification {

  private BugReportSpecification() {}

  /**
   * Build an inclusion-mode specification: bugs whose status/severity/assignee are IN the provided
   * lists.
   */
  public static Specification<BugReport> withInclusionFilters(
      Long projectId,
      Long cycleId,
      Long pitchId,
      List<BugStatus> statuses,
      List<BugSeverity> severities,
      List<Long> assigneeIds,
      String search) {
    return (root, query, cb) -> {
      List<Predicate> predicates =
          buildCommonPredicates(root, query, cb, projectId, cycleId, pitchId, search);

      if (statuses != null && !statuses.isEmpty()) {
        predicates.add(root.get("status").in(statuses));
      }
      if (severities != null && !severities.isEmpty()) {
        predicates.add(root.get("severity").in(severities));
      }
      if (assigneeIds != null && !assigneeIds.isEmpty()) {
        predicates.add(root.get("assignee").get("id").in(assigneeIds));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * Build an exclusion-mode specification: bugs whose status/severity/assignee are NOT IN the
   * provided lists.
   */
  public static Specification<BugReport> withExclusionFilters(
      Long projectId,
      Long cycleId,
      Long pitchId,
      List<BugStatus> statuses,
      List<BugSeverity> severities,
      List<Long> assigneeIds,
      String search) {
    return (root, query, cb) -> {
      List<Predicate> predicates =
          buildCommonPredicates(root, query, cb, projectId, cycleId, pitchId, search);

      if (statuses != null && !statuses.isEmpty()) {
        predicates.add(root.get("status").in(statuses).not());
      }
      if (severities != null && !severities.isEmpty()) {
        predicates.add(root.get("severity").in(severities).not());
      }
      if (assigneeIds != null && !assigneeIds.isEmpty()) {
        predicates.add(root.get("assignee").get("id").in(assigneeIds).not());
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static List<Predicate> buildCommonPredicates(
      Root<BugReport> root,
      CriteriaQuery<?> query,
      CriteriaBuilder cb,
      Long projectId,
      Long cycleId,
      Long pitchId,
      String search) {
    List<Predicate> predicates = new ArrayList<>();

    // Deduplicate results when JOINs produce multiple rows for a single entity
    if (query.getResultType() != Long.class && query.getResultType() != long.class) {
      query.distinct(true);
    }

    Join<Object, Object> cycleJoin = root.join("cycle", JoinType.LEFT);
    Join<Object, Object> pitchJoin = root.join("pitch", JoinType.LEFT);

    // Project filter: direct project_id OR via cycle's project
    if (projectId != null) {
      Predicate directProject = cb.equal(root.get("project").get("id"), projectId);
      Predicate viaProject = cb.equal(cycleJoin.get("project").get("id"), projectId);
      predicates.add(cb.or(directProject, viaProject));
    }

    if (cycleId != null) {
      predicates.add(cb.equal(cycleJoin.get("id"), cycleId));
    }
    if (pitchId != null) {
      predicates.add(cb.equal(pitchJoin.get("id"), pitchId));
    }

    if (search != null && !search.isBlank()) {
      String pattern = "%" + search.trim().toLowerCase() + "%";
      predicates.add(
          cb.or(
              cb.like(cb.lower(root.get("title")), pattern),
              cb.like(cb.lower(root.get("description")), pattern),
              cb.like(cb.lower(root.get("bugKey")), pattern)));
    }

    return predicates;
  }
}
