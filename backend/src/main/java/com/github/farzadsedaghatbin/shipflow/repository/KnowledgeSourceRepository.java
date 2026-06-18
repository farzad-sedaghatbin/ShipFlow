package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence layer for {@link KnowledgeSource}.
 *
 * <p>Single-org deployment: queries do not filter by an organization FK. ORG-scope sources are
 * visible to all authenticated users; TEAM- and PROJECT-scope sources are filtered by their owning
 * id.
 */
@Repository
public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, Long> {

  @Query(
      "SELECT s FROM KnowledgeSource s WHERE s.deletedAt IS NULL "
          + "AND s.scope = com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope.ORG")
  List<KnowledgeSource> findActiveByOrgScope();

  @Query(
      "SELECT s FROM KnowledgeSource s WHERE s.deletedAt IS NULL "
          + "AND s.scope = com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope.TEAM "
          + "AND s.teamId = :teamId")
  List<KnowledgeSource> findActiveByTeamScope(@Param("teamId") Long teamId);

  @Query(
      "SELECT s FROM KnowledgeSource s WHERE s.deletedAt IS NULL "
          + "AND s.scope = com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope.PROJECT "
          + "AND s.projectId = :projectId")
  List<KnowledgeSource> findActiveByProjectScope(@Param("projectId") Long projectId);

  @Query("SELECT s FROM KnowledgeSource s WHERE s.deletedAt IS NULL AND s.id = :id")
  Optional<KnowledgeSource> findActiveById(@Param("id") Long id);

  @Query(
      "SELECT s FROM KnowledgeSource s WHERE s.deletedAt IS NULL AND s.status = :status "
          + "AND (s.lastIngestedAt IS NULL OR s.lastIngestedAt < :before)")
  List<KnowledgeSource> findRefreshCandidates(
      @Param("status") KnowledgeSourceStatus status, @Param("before") OffsetDateTime before);

  /**
   * Finds the active KnowledgeSource whose JSON config contains the given spaceId value.
   * Used to locate the auto-created source when a wiki space is deleted.
   */
  @Query(
      "SELECT s FROM KnowledgeSource s WHERE s.deletedAt IS NULL "
          + "AND s.config LIKE CONCAT('%\"spaceId\":', :spaceId, '%')")
  List<KnowledgeSource> findActiveByWikiSpaceId(@Param("spaceId") Long spaceId);
}
