package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.enums.InitiativeStatus;
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
public interface InitiativeRepository extends JpaRepository<Initiative, Long> {

  // Standard queries (include deleted)
  List<Initiative> findByProjectId(Long projectId);

  List<Initiative> findByProjectIdAndStatus(Long projectId, InitiativeStatus status);

  // Soft delete-aware queries
  @Query("SELECT i FROM Initiative i WHERE i.deletedAt IS NULL ORDER BY i.sortOrder, i.createdAt DESC")
  List<Initiative> findAllNotDeleted();

  @Query("SELECT i FROM Initiative i WHERE i.id = :id AND i.deletedAt IS NULL")
  Optional<Initiative> findByIdNotDeleted(@Param("id") Long id);

  @Query("SELECT i FROM Initiative i WHERE i.project.id = :projectId AND i.deletedAt IS NULL ORDER BY i.sortOrder, i.targetStartDate")
  List<Initiative> findByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT i FROM Initiative i WHERE i.project.id = :projectId AND i.deletedAt IS NULL ORDER BY i.sortOrder, i.targetStartDate")
  Page<Initiative> findByProjectIdNotDeleted(@Param("projectId") Long projectId, Pageable pageable);

  @Query("SELECT i FROM Initiative i WHERE i.project.id = :projectId AND i.status = :status AND i.deletedAt IS NULL ORDER BY i.sortOrder")
  List<Initiative> findByProjectIdAndStatusNotDeleted(@Param("projectId") Long projectId, @Param("status") InitiativeStatus status);

  @Query("SELECT i FROM Initiative i WHERE i.project.id = :projectId AND i.status IN :statuses AND i.deletedAt IS NULL ORDER BY i.sortOrder")
  List<Initiative> findByProjectIdAndStatusInNotDeleted(@Param("projectId") Long projectId, @Param("statuses") List<InitiativeStatus> statuses);

  @Query("SELECT i FROM Initiative i WHERE i.owner.id = :ownerId AND i.deletedAt IS NULL ORDER BY i.sortOrder")
  List<Initiative> findByOwnerIdNotDeleted(@Param("ownerId") Long ownerId);

  // Date range queries for roadmap timeline
  @Query("SELECT i FROM Initiative i WHERE i.project.id = :projectId " +
         "AND ((i.targetStartDate <= :endDate AND i.targetEndDate >= :startDate) " +
         "OR (i.targetStartDate IS NULL OR i.targetEndDate IS NULL)) " +
         "AND i.deletedAt IS NULL ORDER BY i.sortOrder, i.targetStartDate")
  List<Initiative> findByProjectIdAndDateRangeNotDeleted(
      @Param("projectId") Long projectId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  // Count queries
  @Query("SELECT COUNT(i) FROM Initiative i WHERE i.project.id = :projectId AND i.deletedAt IS NULL")
  long countByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT COUNT(i) FROM Initiative i WHERE i.project.id = :projectId AND i.status = :status AND i.deletedAt IS NULL")
  long countByProjectIdAndStatusNotDeleted(@Param("projectId") Long projectId, @Param("status") InitiativeStatus status);

  // Fetch with epics
  @Query("SELECT DISTINCT i FROM Initiative i LEFT JOIN FETCH i.epics e WHERE i.id = :id AND i.deletedAt IS NULL AND (e IS NULL OR e.deletedAt IS NULL)")
  Optional<Initiative> findByIdWithEpicsNotDeleted(@Param("id") Long id);

  @Query("SELECT DISTINCT i FROM Initiative i LEFT JOIN FETCH i.epics e WHERE i.project.id = :projectId AND i.deletedAt IS NULL AND (e IS NULL OR e.deletedAt IS NULL) ORDER BY i.sortOrder")
  List<Initiative> findByProjectIdWithEpicsNotDeleted(@Param("projectId") Long projectId);
}
