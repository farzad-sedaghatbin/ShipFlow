package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.enums.EpicStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EpicRepository extends JpaRepository<Epic, Long> {

  // Standard queries (include deleted)
  List<Epic> findByProjectId(Long projectId);

  List<Epic> findByInitiativeId(Long initiativeId);

  List<Epic> findByProjectIdAndStatus(Long projectId, EpicStatus status);

  // Soft delete-aware queries
  @Query("SELECT e FROM Epic e WHERE e.deletedAt IS NULL ORDER BY e.sortOrder, e.createdAt DESC")
  List<Epic> findAllNotDeleted();

  @Query("SELECT e FROM Epic e WHERE e.id = :id AND e.deletedAt IS NULL")
  Optional<Epic> findByIdNotDeleted(@Param("id") Long id);

  @Query("SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.deletedAt IS NULL ORDER BY e.sortOrder, e.targetStartDate")
  List<Epic> findByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.deletedAt IS NULL ORDER BY e.sortOrder, e.targetStartDate")
  Page<Epic> findByProjectIdNotDeleted(@Param("projectId") Long projectId, Pageable pageable);

  @Query("SELECT e FROM Epic e WHERE e.initiative.id = :initiativeId AND e.deletedAt IS NULL ORDER BY e.sortOrder")
  List<Epic> findByInitiativeIdNotDeleted(@Param("initiativeId") Long initiativeId);

  @Query("SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.initiative IS NULL AND e.deletedAt IS NULL ORDER BY e.sortOrder")
  List<Epic> findOrphanEpicsByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.status = :status AND e.deletedAt IS NULL ORDER BY e.sortOrder")
  List<Epic> findByProjectIdAndStatusNotDeleted(@Param("projectId") Long projectId, @Param("status") EpicStatus status);

  @Query("SELECT e FROM Epic e WHERE e.project.id = :projectId AND e.status IN :statuses AND e.deletedAt IS NULL ORDER BY e.sortOrder")
  List<Epic> findByProjectIdAndStatusInNotDeleted(@Param("projectId") Long projectId, @Param("statuses") List<EpicStatus> statuses);

  @Query("SELECT e FROM Epic e WHERE e.owner.id = :ownerId AND e.deletedAt IS NULL ORDER BY e.sortOrder")
  List<Epic> findByOwnerIdNotDeleted(@Param("ownerId") Long ownerId);

  // Date range queries for roadmap timeline
  @Query("SELECT e FROM Epic e WHERE e.project.id = :projectId " +
         "AND ((e.targetStartDate <= :endDate AND e.targetEndDate >= :startDate) " +
         "OR (e.targetStartDate IS NULL OR e.targetEndDate IS NULL)) " +
         "AND e.deletedAt IS NULL ORDER BY e.sortOrder, e.targetStartDate")
  List<Epic> findByProjectIdAndDateRangeNotDeleted(
      @Param("projectId") Long projectId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  // Count queries
  @Query("SELECT COUNT(e) FROM Epic e WHERE e.project.id = :projectId AND e.deletedAt IS NULL")
  long countByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT COUNT(e) FROM Epic e WHERE e.initiative.id = :initiativeId AND e.deletedAt IS NULL")
  long countByInitiativeIdNotDeleted(@Param("initiativeId") Long initiativeId);

  @Query("SELECT COUNT(e) FROM Epic e WHERE e.initiative.id = :initiativeId AND e.status = :status AND e.deletedAt IS NULL")
  long countByInitiativeIdAndStatusNotDeleted(@Param("initiativeId") Long initiativeId, @Param("status") EpicStatus status);

  // Fetch with pitches
  @Query("SELECT DISTINCT e FROM Epic e LEFT JOIN FETCH e.pitches p WHERE e.id = :id AND e.deletedAt IS NULL AND (p IS NULL OR p.deletedAt IS NULL)")
  Optional<Epic> findByIdWithPitchesNotDeleted(@Param("id") Long id);

  @Query("SELECT DISTINCT e FROM Epic e LEFT JOIN FETCH e.pitches p WHERE e.project.id = :projectId AND e.deletedAt IS NULL AND (p IS NULL OR p.deletedAt IS NULL) ORDER BY e.sortOrder")
  List<Epic> findByProjectIdWithPitchesNotDeleted(@Param("projectId") Long projectId);
}
