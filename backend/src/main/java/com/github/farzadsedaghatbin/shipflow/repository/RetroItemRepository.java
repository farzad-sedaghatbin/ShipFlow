package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RetroItemRepository extends JpaRepository<RetroItem, Long> {

  List<RetroItem> findByRetrospectiveIdOrderByCreatedAtAsc(Long retrospectiveId);

  List<RetroItem> findByRetrospectiveIdAndColumnTypeOrderByCreatedAtAsc(Long retrospectiveId,
      RetroColumnType columnType);

  @Query("SELECT COUNT(i) FROM RetroItem i WHERE i.retrospective.id = :retrospectiveId")
  long countByRetrospectiveId(@Param("retrospectiveId") Long retrospectiveId);

  @Query("SELECT i FROM RetroItem i LEFT JOIN FETCH i.author WHERE i.id = :id")
  Optional<RetroItem> findByIdWithAuthor(@Param("id") Long id);

  // Find items that are not merged (for display)
  @Query("SELECT i FROM RetroItem i WHERE i.retrospective.id = :retrospectiveId AND i.mergedInto IS NULL ORDER BY i.voteCount DESC, i.createdAt ASC")
  List<RetroItem> findActiveItemsByRetrospectiveId(@Param("retrospectiveId") Long retrospectiveId);

  // Find items merged into a specific item
  List<RetroItem> findByMergedIntoId(Long mergedIntoId);

  // v0.5 - Find action items by column type for follow-through tracking
  List<RetroItem> findByRetrospectiveIdAndColumnType(Long retrospectiveId, RetroColumnType columnType);

  // v0.5 - Find unacted action items by retrospective
  @Query("SELECT i FROM RetroItem i WHERE i.retrospective.id = :retroId AND i.columnType = 'ACTIONS' AND (i.actedOn IS NULL OR i.actedOn = false)")
  List<RetroItem> findUnactedActionItems(@Param("retroId") Long retrospectiveId);

  // v0.5 - Find unacted action items across all retros in a project
  @Query("SELECT i FROM RetroItem i WHERE i.retrospective.cycle.project.id = :projectId AND i.columnType = 'ACTIONS' AND (i.actedOn IS NULL OR i.actedOn = false)")
  List<RetroItem> findUnactedActionItemsByProjectId(@Param("projectId") Long projectId);

  // v0.5 - Count action items acted upon by retrospective
  @Query("SELECT COUNT(i) FROM RetroItem i WHERE i.retrospective.id = :retroId AND i.columnType = 'ACTIONS' AND i.actedOn = true")
  long countActedOnByRetrospectiveId(@Param("retroId") Long retrospectiveId);

  void deleteAllByRetrospectiveId(Long retrospectiveId);

  // ==================== BATCH QUERY METHODS ====================

  /**
   * Batch count items by retrospective IDs.
   * Fixes N+1 pattern in toDTO mapping.
   */
  @Query("SELECT i.retrospective.id as retroId, COUNT(i) as itemCount FROM RetroItem i WHERE i.retrospective.id IN :retroIds GROUP BY i.retrospective.id")
  List<Object[]> countByRetrospectiveIdsRaw(@Param("retroIds") List<Long> retroIds);

  default Map<Long, Long> countByRetrospectiveIds(List<Long> retroIds) {
    return countByRetrospectiveIdsRaw(retroIds).stream()
        .collect(Collectors.toMap(
            row -> (Long) row[0],
            row -> (Long) row[1]
        ));
  }

  /**
   * Batch find merged item IDs grouped by target item ID.
   * Fixes N+1 pattern in toItemDTO mapping.
   */
  @Query("SELECT i.mergedInto.id as targetId, i.id as sourceId FROM RetroItem i WHERE i.mergedInto.id IN :targetIds")
  List<Object[]> findMergedItemIdsByTargetIdsRaw(@Param("targetIds") List<Long> targetIds);

  default Map<Long, List<Long>> findMergedItemIdsByTargetIds(List<Long> targetIds) {
    return findMergedItemIdsByTargetIdsRaw(targetIds).stream()
        .collect(Collectors.groupingBy(
            row -> (Long) row[0],
            Collectors.mapping(row -> (Long) row[1], Collectors.toList())
        ));
  }
}
