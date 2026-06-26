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
   * Search across all entity types.
   *
   * <p>Project-scoped entities (tasks, subtasks, bug reports, pitches, epics) are included only when
   * {@code projectId} is provided. Wiki spaces and pages are org-global by design, so they are always
   * searched: when a project is selected we include that project's spaces plus every org-global
   * (project-less) space; with no project selected we search org-global spaces only.
   *
   * @param query     the search text (minimum 2 characters)
   * @param projectId the project to scope results to, or {@code null} for an org-global wiki search
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
    boolean hasProject = projectId != null;
    String sql = buildSearchQuery(hasProject);

    Query nativeQuery = entityManager.createNativeQuery(sql);
    nativeQuery.setParameter("query", trimmedQuery);
    // Escape LIKE special chars using '!' as escape character (see ESCAPE '!' in SQL)
    String escapedForLike = trimmedQuery.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    nativeQuery.setParameter("queryPattern", "%" + escapedForLike + "%");
    // Only bind :projectId when the SQL references it (omitted for org-global wiki-only search).
    if (hasProject) {
      nativeQuery.setParameter("projectId", projectId);
    }
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

  /**
   * Build the search SQL. Project-scoped entity blocks are included only when {@code hasProject};
   * wiki space + page blocks are always included, scoped org-globally (and, when a project is
   * selected, additionally to that project's spaces).
   */
  private String buildSearchQuery(boolean hasProject) {
    // Org-global wiki is always searchable; a selected project additionally surfaces its own spaces.
    String wikiScope =
        hasProject
            ? "(ws.project_id = :projectId OR ws.project_id IS NULL)"
            : "ws.project_id IS NULL";

    StringBuilder sb = new StringBuilder("WITH search_results AS (\n");
    if (hasProject) {
      sb.append(PROJECT_SCOPED_BLOCK).append("\n          UNION ALL\n");
    }
    sb.append(wikiSpaceBlock(wikiScope))
        .append("\n          UNION ALL\n")
        .append(wikiPageBlock(wikiScope))
        .append("\n        )\n")
        .append("        SELECT entity_type, entity_id, title, subtitle, route, score, matched_by\n")
        .append("        FROM search_results\n")
        .append("        ORDER BY score DESC, title ASC\n")
        .append("        LIMIT :resultLimit\n");
    return sb.toString();
  }

  /** Wiki space results — matches space name and key; routes to the space by numeric id. */
  private String wikiSpaceBlock(String wikiScope) {
    return """
          -- Wiki Spaces
          SELECT
            'WIKI_SPACE' AS entity_type,
            ws.id AS entity_id,
            ws.name AS title,
            CONCAT('Wiki space · ', ws.space_key) AS subtitle,
            CONCAT('/wiki/', ws.id) AS route,
            GREATEST(
              similarity(ws.name, :query),
              CASE WHEN LOWER(ws.name) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.6 ELSE 0.0 END,
              CASE WHEN LOWER(ws.space_key) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.5 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(ws.name) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE_TITLE'
              WHEN LOWER(ws.space_key) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE_KEY'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM wiki_spaces ws
          WHERE %s
            AND ws.deleted_at IS NULL
            AND (
              similarity(ws.name, :query) > 0.1
              OR LOWER(ws.name) LIKE LOWER(:queryPattern) ESCAPE '!'
              OR LOWER(ws.space_key) LIKE LOWER(:queryPattern) ESCAPE '!'
            )"""
        .formatted(wikiScope);
  }

  /** Wiki page results — matches title and body; routes to the page by numeric space id + page id. */
  private String wikiPageBlock(String wikiScope) {
    return """
          -- Wiki Pages
          SELECT
            'WIKI_PAGE' AS entity_type,
            wp.id AS entity_id,
            wp.title AS title,
            ws.name AS subtitle,
            CONCAT('/wiki/', ws.id, '/', wp.id) AS route,
            GREATEST(
              similarity(wp.title, :query),
              CASE WHEN LOWER(wp.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.6 ELSE 0.0 END,
              CASE WHEN LOWER(wp.content_text) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.4 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(wp.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE_TITLE'
              WHEN LOWER(wp.content_text) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE_BODY'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM wiki_pages wp
            JOIN wiki_spaces ws ON wp.space_id = ws.id
          WHERE %s
            AND ws.deleted_at IS NULL
            AND wp.deleted_at IS NULL
            AND (
              similarity(wp.title, :query) > 0.1
              OR LOWER(wp.title) LIKE LOWER(:queryPattern) ESCAPE '!'
              OR LOWER(wp.content_text) LIKE LOWER(:queryPattern) ESCAPE '!'
            )"""
        .formatted(wikiScope);
  }

  /** Project-scoped entity SELECTs (tasks, subtasks, bug reports, pitches, epics), UNION-joined. */
  private static final String PROJECT_SCOPED_BLOCK =
      """
          -- Tasks (parent_task_id IS NULL = root task)
          SELECT
            CASE WHEN t.parent_task_id IS NULL THEN 'TASK' ELSE 'SUBTASK' END AS entity_type,
            t.id AS entity_id,
            t.title AS title,
            t.status AS subtitle,
            CONCAT('/backlog/', t.id) AS route,
            GREATEST(
              similarity(t.title, :query),
              CASE WHEN LOWER(t.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(t.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE_TITLE'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM tasks t
            JOIN cycles c ON t.cycle_id = c.id
          WHERE c.project_id = :projectId
            AND t.deleted_at IS NULL
            AND (
              similarity(t.title, :query) > 0.1
              OR LOWER(t.title) LIKE LOWER(:queryPattern) ESCAPE '!'
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
              CASE WHEN LOWER(br.bug_key) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.9 ELSE 0.0 END,
              CASE WHEN LOWER(br.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(br.bug_key) = LOWER(:query) THEN 'EXACT_KEY'
              WHEN LOWER(br.bug_key) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'PARTIAL_KEY'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM bug_reports br
          WHERE br.project_id = :projectId
            AND (
              similarity(br.title, :query) > 0.1
              OR LOWER(br.title) LIKE LOWER(:queryPattern) ESCAPE '!'
              OR LOWER(br.bug_key) LIKE LOWER(:queryPattern) ESCAPE '!'
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
              CASE WHEN LOWER(p.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(p.title) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE_TITLE'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM pitches p
            LEFT JOIN cycles c ON p.cycle_id = c.id
            LEFT JOIN epics e ON p.epic_id = e.id
          WHERE p.deleted_at IS NULL
            AND (
              (p.cycle_id IS NOT NULL OR p.epic_id IS NOT NULL)
              AND (
                (p.cycle_id IS NULL OR c.project_id = :projectId)
                AND (p.epic_id IS NULL OR e.project_id = :projectId)
              )
            )
            AND (
              similarity(p.title, :query) > 0.1
              OR LOWER(p.title) LIKE LOWER(:queryPattern) ESCAPE '!'
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
              CASE WHEN LOWER(e.name) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 0.6 ELSE 0.0 END
            ) AS score,
            CASE
              WHEN LOWER(e.name) LIKE LOWER(:queryPattern) ESCAPE '!' THEN 'LIKE'
              ELSE 'TRIGRAM'
            END AS matched_by
          FROM epics e
          WHERE e.project_id = :projectId
            AND e.deleted_at IS NULL
            AND (
              similarity(e.name, :query) > 0.1
              OR LOWER(e.name) LIKE LOWER(:queryPattern) ESCAPE '!'
            )""";
}
