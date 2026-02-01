package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {

  /** Find knowledge items by entity type and entity ID. */
  List<KnowledgeItem> findByEntityTypeAndEntityId(KnowledgeEntityType entityType, Long entityId);

  /** Find a knowledge item by its embedding ID. */
  Optional<KnowledgeItem> findByEmbeddingId(String embeddingId);

  /** Find knowledge items that need to be embedded. */
  List<KnowledgeItem> findByIsEmbeddedFalse();

  /** Find knowledge items by cycle ID. */
  List<KnowledgeItem> findByCycleId(Long cycleId);

  /** Find knowledge items by team ID. */
  List<KnowledgeItem> findByTeamId(Long teamId);

  /** Find knowledge items by pitch ID. */
  List<KnowledgeItem> findByPitchId(Long pitchId);

  /** Count embedded knowledge items. */
  @Query("SELECT COUNT(k) FROM KnowledgeItem k WHERE k.isEmbedded = true")
  long countEmbedded();

  /** Find knowledge items by entity type. */
  List<KnowledgeItem> findByEntityType(KnowledgeEntityType entityType);

  /** Find knowledge item by entity type, entity ID, and chunk index. */
  Optional<KnowledgeItem> findByEntityTypeAndEntityIdAndChunkIndex(
      KnowledgeEntityType entityType, Long entityId, Integer chunkIndex);

  /** Delete knowledge items by entity type and entity ID. */
  void deleteByEntityTypeAndEntityId(KnowledgeEntityType entityType, Long entityId);

  /** Find knowledge items by content hash. */
  List<KnowledgeItem> findByContentHash(String contentHash);

  /** Find all embedding IDs for a given entity. */
  @Query(
      "SELECT k.embeddingId FROM KnowledgeItem k WHERE k.entityType = :type AND k.entityId = :entityId AND k.embeddingId IS NOT NULL")
  List<String> findEmbeddingIdsByEntity(
      @Param("type") KnowledgeEntityType type, @Param("entityId") Long entityId);
}
