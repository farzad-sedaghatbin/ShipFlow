package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CycleNarrative;
import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for CycleNarrative entity operations.
 * Supports storing and retrieving AI or template-generated cycle summaries.
 */
@Repository
public interface CycleNarrativeRepository extends JpaRepository<CycleNarrative, Long> {

  List<CycleNarrative> findByCycleId(Long cycleId);

  List<CycleNarrative> findByCycleIdOrderByGeneratedAtDesc(Long cycleId);

  Optional<CycleNarrative> findByCycleIdAndNarrativeType(Long cycleId, NarrativeType narrativeType);

  @Query("SELECT cn FROM CycleNarrative cn WHERE cn.cycle.id = :cycleId AND cn.narrativeType = :type ORDER BY cn.generatedAt DESC")
  List<CycleNarrative> findAllByCycleIdAndNarrativeType(@Param("cycleId") Long cycleId, @Param("type") NarrativeType type);

  @Query("SELECT cn FROM CycleNarrative cn WHERE cn.cycle.id = :cycleId ORDER BY cn.generatedAt DESC")
  List<CycleNarrative> findLatestByCycleId(@Param("cycleId") Long cycleId);

  @Query("SELECT DISTINCT cn.narrativeType FROM CycleNarrative cn WHERE cn.cycle.id = :cycleId")
  List<NarrativeType> findNarrativeTypesByCycleId(@Param("cycleId") Long cycleId);

  boolean existsByCycleIdAndNarrativeType(Long cycleId, NarrativeType narrativeType);

  void deleteByCycleIdAndNarrativeType(Long cycleId, NarrativeType narrativeType);

  @Query("SELECT cn FROM CycleNarrative cn WHERE cn.cycle.project.id = :projectId ORDER BY cn.cycle.startDate DESC, cn.generatedAt DESC")
  List<CycleNarrative> findByProjectIdOrderByCycleStartDateDesc(@Param("projectId") Long projectId);

  /**
   * Pageable overload — used by {@code ProjectSnapshotService} to limit the number of retro
   * narratives loaded when building the "knownPatterns" snapshot section.
   */
  @Query("SELECT cn FROM CycleNarrative cn WHERE cn.cycle.project.id = :projectId ORDER BY cn.cycle.startDate DESC, cn.generatedAt DESC")
  List<CycleNarrative> findByProjectIdOrderByCycleStartDateDesc(
      @Param("projectId") Long projectId, Pageable pageable);
}
