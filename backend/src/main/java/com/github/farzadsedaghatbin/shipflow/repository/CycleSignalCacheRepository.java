package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CycleSignalCache;
import com.github.farzadsedaghatbin.shipflow.entity.enums.SignalType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for CycleSignalCache entity operations.
 * Manages caching of computed cycle signals to avoid expensive recalculation.
 */
@Repository
public interface CycleSignalCacheRepository extends JpaRepository<CycleSignalCache, Long> {

  Optional<CycleSignalCache> findByProjectIdAndSignalType(Long projectId, SignalType signalType);

  List<CycleSignalCache> findByProjectId(Long projectId);

  @Query("SELECT c FROM CycleSignalCache c WHERE c.project.id = :projectId AND c.signalType = :type AND (c.validUntil IS NULL OR c.validUntil > :now)")
  Optional<CycleSignalCache> findValidCacheByProjectAndType(
      @Param("projectId") Long projectId, 
      @Param("type") SignalType signalType,
      @Param("now") LocalDateTime now);

  @Query("SELECT c FROM CycleSignalCache c WHERE c.project.id = :projectId AND (c.validUntil IS NULL OR c.validUntil > :now)")
  List<CycleSignalCache> findAllValidByProjectId(@Param("projectId") Long projectId, @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM CycleSignalCache c WHERE c.project.id = :projectId AND c.signalType = :type")
  void deleteByProjectIdAndSignalType(@Param("projectId") Long projectId, @Param("type") SignalType signalType);

  @Modifying
  @Query("DELETE FROM CycleSignalCache c WHERE c.project.id = :projectId")
  void deleteByProjectId(@Param("projectId") Long projectId);

  @Modifying
  @Query("DELETE FROM CycleSignalCache c WHERE c.validUntil IS NOT NULL AND c.validUntil < :now")
  void deleteExpiredCaches(@Param("now") LocalDateTime now);

  boolean existsByProjectIdAndSignalType(Long projectId, SignalType signalType);
}
