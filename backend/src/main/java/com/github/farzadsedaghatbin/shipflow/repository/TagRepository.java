package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Tag;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Tag entity operations.
 * Supports tag-based correlation between retro items and pitches.
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

  List<Tag> findByProjectId(Long projectId);

  List<Tag> findByProjectIdOrderByNameAsc(Long projectId);

  Optional<Tag> findByNameAndProjectId(String name, Long projectId);

  @Query("SELECT t FROM Tag t WHERE t.project.id = :projectId AND LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))")
  List<Tag> searchByNameInProject(@Param("projectId") Long projectId, @Param("query") String query);

  @Query("SELECT t FROM Tag t WHERE t.project.id = :projectId AND LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY t.name ASC")
  List<Tag> searchByNameInProjectOrdered(@Param("projectId") Long projectId, @Param("query") String query);

  @Query("SELECT DISTINCT t FROM Tag t JOIN t.retroItems ri WHERE ri.retrospective.id = :retroId")
  List<Tag> findByRetrospectiveId(@Param("retroId") Long retroId);

  @Query("SELECT DISTINCT t FROM Tag t JOIN t.pitches p WHERE p.cycle.id = :cycleId AND p.deletedAt IS NULL")
  List<Tag> findByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT COUNT(ri) FROM RetroItem ri JOIN ri.tags t WHERE t.id = :tagId")
  Long countRetroItemsByTagId(@Param("tagId") Long tagId);

  @Query("SELECT COUNT(p) FROM Pitch p JOIN p.tags t WHERE t.id = :tagId AND p.deletedAt IS NULL")
  Long countPitchesByTagId(@Param("tagId") Long tagId);

  boolean existsByNameAndProjectId(String name, Long projectId);
}
