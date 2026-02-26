package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.GlobalSearchResultDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for global search across tasks, subtasks, bug reports, pitches, and epics.
 * Uses PostgreSQL trigram similarity (pg_trgm) for fuzzy matching and exact key matching.
 */
@Service
@Slf4j
public class GlobalSearchService {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Search across all entity types within a project.
   *
   * @param query     the search text (minimum 2 characters)
   * @param projectId the project to scope results to
   * @param limit     maximum number of results to return
   * @return ranked list of search results
   */
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<GlobalSearchResultDTO> search(String query, Long projectId, int limit) {
    if (query == null || query.trim().length() < 2) {
      return List.of();
    }

    String trimmedQuery = query.trim();
    String sql = buildSearchQuery();

    Query nativeQuery = entityManager.createNativeQuery(sql);
    nativeQuery.setParameter("query", trimmedQuery);
    nativeQuery.setParameter("queryPattern", "%" + trimmedQuery + "%");
    nativeQuery.setParameter("projectId", projectId);
    nativeQuery.setParameter("resultLimit", limit);

    List<Object[]> rows = nativeQuery.getResultList();
    List<GlobalSearchResultDTO> results = new ArrayList<>();

    for (Object[] row : rows) {
      results.add(GlobalSearchResultDTO.builder()
          .entityType((String) row[0])
          .entityId(((Number) row[1]).longValue())
          .title((String) row[2])
          .subtitle((String) row[3])
          .route((String) row[4])
          .score(((Number) row[5]).doubleValue())
          .matchedBy((String) row[6])
          .build());
    }

    return results;
  }

  private String buildSearchQuery() {
    return """
        WITH search_results AS (
          -- Tasks (parent_task_id IS NULL = root task)
          SELECT
            CASE WHEN t.parent_task_id IS NULL THEN 'TASK' ELSE 'SUBTASK' END AS entity_type,
            t.id AS entity_id,
            t.title AS title,
            t.status AS subtitle,
            CASE WHEN t.parent_task_id IS NULL
              THEN CONCAT('/backlog/', t.id)
              ELSE CONCAT('/backlog/', t.id)
            END AS route,
            GREATEST(
              similarity(t.title, :query),
              CASE WHEN LOWER(t.title) LIKE LOWER(:queryPattern) THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(t.title) LIKE LOWER(:queryPattern) THEN 'TRIGRAM'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM tasks t
            JOIN cycles c ON t.cycle_id = c.id
          WHERE c.project_id = :projectId
            AND t.deleted_at IS NULL
            AND (
              similarity(t.title, :query) > 0.1
              OR LOWER(t.title) LIKE LOWER(:queryPattern)
            )

          UNION ALL

          -- Bug Reports
          SELECT
            'BUG_REPORT' AS entity_type,
            br.id AS entity_id,
            br.title AS title,
            CONCAT(br.bug_key, ' · ', br.status) AS subtitle,
            CONCAT('/qa/bug-reports/', br.id) AS route,
            GREATEST(
              similarity(br.title, :query),
              CASE WHEN LOWER(br.bug_key) = LOWER(:query) THEN 1.0 ELSE 0.0 END,
              CASE WHEN LOWER(br.bug_key) LIKE LOWER(:queryPattern) THEN 0.9 ELSE 0.0 END,
              CASE WHEN LOWER(br.title) LIKE LOWER(:queryPattern) THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(br.bug_key) = LOWER(:query) THEN 'EXACT_KEY'
              WHEN LOWER(br.bug_key) LIKE LOWER(:queryPattern) THEN 'EXACT_KEY'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM bug_reports br
          WHERE br.project_id = :projectId
            AND (
              similarity(br.title, :query) > 0.1
              OR LOWER(br.title) LIKE LOWER(:queryPattern)
              OR LOWER(br.bug_key) LIKE LOWER(:queryPattern)
            )

          UNION ALL

          -- Pitches (may be linked via cycle.project or epic.project)
          SELECT
            'PITCH' AS entity_type,
            p.id AS entity_id,
            p.title AS title,
            p.status AS subtitle,
            CONCAT('/pitches/', p.id) AS route,
            GREATEST(
              similarity(p.title, :query),
              CASE WHEN LOWER(p.title) LIKE LOWER(:queryPattern) THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(p.title) LIKE LOWER(:queryPattern) THEN 'TRIGRAM'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM pitches p
            LEFT JOIN cycles c ON p.cycle_id = c.id
            LEFT JOIN epics e ON p.epic_id = e.id
          WHERE p.deleted_at IS NULL
            AND (c.project_id = :projectId OR e.project_id = :projectId)
            AND (
              similarity(p.title, :query) > 0.1
              OR LOWER(p.title) LIKE LOWER(:queryPattern)
            )

          UNION ALL

          -- Epics
          SELECT
            'EPIC' AS entity_type,
            e.id AS entity_id,
            e.name AS title,
            e.status AS subtitle,
            CONCAT('/epics/', e.id) AS route,
            GREATEST(
              similarity(e.name, :query),
              CASE WHEN LOWER(e.name) LIKE LOWER(:queryPattern) THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(e.name) LIKE LOWER(:queryPattern) THEN 'TRIGRAM'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM epics e
          WHERE e.project_id = :projectId
            AND e.deleted_at IS NULL
            AND (
              similarity(e.name, :query) > 0.1
              OR LOWER(e.name) LIKE LOWER(:queryPattern)
            )
        )
        SELECT entity_type, entity_id, title, subtitle, route, score, matched_by
        FROM search_results
        ORDER BY score DESC, title ASC
        LIMIT :resultLimit
        """;
  }
}
