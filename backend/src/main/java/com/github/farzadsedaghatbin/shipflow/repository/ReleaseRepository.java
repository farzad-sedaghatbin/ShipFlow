package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Release;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
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
public interface ReleaseRepository extends JpaRepository<Release, Long> {

  // Standard queries (include deleted)
  List<Release> findByProjectId(Long projectId);

  List<Release> findByProjectIdAndStatus(Long projectId, ReleaseStatus status);

  // Soft delete-aware queries
  @Query("SELECT r FROM Release r WHERE r.deletedAt IS NULL ORDER BY r.targetDate DESC, r.sortOrder")
  List<Release> findAllNotDeleted();

  @Query("SELECT r FROM Release r WHERE r.id = :id AND r.deletedAt IS NULL")
  Optional<Release> findByIdNotDeleted(@Param("id") Long id);

  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.deletedAt IS NULL ORDER BY r.targetDate DESC, r.sortOrder")
  List<Release> findByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.deletedAt IS NULL ORDER BY r.targetDate DESC, r.sortOrder")
  Page<Release> findByProjectIdNotDeleted(@Param("projectId") Long projectId, Pageable pageable);

  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.status = :status AND r.deletedAt IS NULL ORDER BY r.targetDate DESC")
  List<Release> findByProjectIdAndStatusNotDeleted(@Param("projectId") Long projectId, @Param("status") ReleaseStatus status);

  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.status IN :statuses AND r.deletedAt IS NULL ORDER BY r.targetDate DESC")
  List<Release> findByProjectIdAndStatusInNotDeleted(@Param("projectId") Long projectId, @Param("statuses") List<ReleaseStatus> statuses);

  // Upcoming releases (for planning)
  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.status IN ('PLANNING', 'IN_PROGRESS', 'STAGING') " +
         "AND r.targetDate >= :today AND r.deletedAt IS NULL ORDER BY r.targetDate ASC")
  List<Release> findUpcomingByProjectIdNotDeleted(@Param("projectId") Long projectId, @Param("today") LocalDate today);

  // Past releases
  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.status = 'RELEASED' " +
         "AND r.deletedAt IS NULL ORDER BY r.releaseDate DESC")
  List<Release> findReleasedByProjectIdNotDeleted(@Param("projectId") Long projectId);

  // Date range queries for roadmap timeline
  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId " +
         "AND r.targetDate >= :startDate AND r.targetDate <= :endDate " +
         "AND r.deletedAt IS NULL ORDER BY r.targetDate ASC")
  List<Release> findByProjectIdAndDateRangeNotDeleted(
      @Param("projectId") Long projectId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  // Count queries
  @Query("SELECT COUNT(r) FROM Release r WHERE r.project.id = :projectId AND r.deletedAt IS NULL")
  long countByProjectIdNotDeleted(@Param("projectId") Long projectId);

  @Query("SELECT COUNT(r) FROM Release r WHERE r.project.id = :projectId AND r.status = :status AND r.deletedAt IS NULL")
  long countByProjectIdAndStatusNotDeleted(@Param("projectId") Long projectId, @Param("status") ReleaseStatus status);

  // Fetch with cycles
  @Query("SELECT DISTINCT r FROM Release r LEFT JOIN FETCH r.cycles WHERE r.id = :id AND r.deletedAt IS NULL")
  Optional<Release> findByIdWithCyclesNotDeleted(@Param("id") Long id);

  @Query("SELECT DISTINCT r FROM Release r LEFT JOIN FETCH r.cycles WHERE r.project.id = :projectId AND r.deletedAt IS NULL ORDER BY r.targetDate DESC")
  List<Release> findByProjectIdWithCyclesNotDeleted(@Param("projectId") Long projectId);

  // Find by version
  @Query("SELECT r FROM Release r WHERE r.project.id = :projectId AND r.version = :version AND r.deletedAt IS NULL")
  Optional<Release> findByProjectIdAndVersionNotDeleted(@Param("projectId") Long projectId, @Param("version") String version);

  // Find releases containing a cycle
  @Query("SELECT r FROM Release r JOIN r.cycles c WHERE c.id = :cycleId AND r.deletedAt IS NULL")
  List<Release> findByCycleIdNotDeleted(@Param("cycleId") Long cycleId);
}
