package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.EntityWikiLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityWikiLinkRepository extends JpaRepository<EntityWikiLink, Long> {

  @Query("SELECT l FROM EntityWikiLink l WHERE l.entityType = :entityType AND l.entityId = :entityId "
      + "AND l.wikiPage.deletedAt IS NULL ORDER BY l.linkedAt DESC")
  List<EntityWikiLink> findByEntityTypeAndEntityId(
      @Param("entityType") String entityType, @Param("entityId") Long entityId);

  boolean existsByEntityTypeAndEntityIdAndWikiPageId(
      String entityType, Long entityId, Long wikiPageId);

  Optional<EntityWikiLink> findByEntityTypeAndEntityIdAndWikiPageId(
      String entityType, Long entityId, Long wikiPageId);

  void deleteByEntityTypeAndEntityIdAndWikiPageId(
      String entityType, Long entityId, Long wikiPageId);
}
