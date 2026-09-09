package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ProjectAgreement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ProjectAgreement entity operations.
 * Supports the per-project list of discrete, dated agreement entries.
 */
@Repository
public interface ProjectAgreementRepository extends JpaRepository<ProjectAgreement, Long> {

  @Query("SELECT a FROM ProjectAgreement a WHERE a.id = :id AND a.deletedAt IS NULL")
  Optional<ProjectAgreement> findByIdNotDeleted(@Param("id") Long id);

  @Query("SELECT a FROM ProjectAgreement a WHERE a.project.id = :projectId AND a.deletedAt IS NULL "
      + "ORDER BY a.agreedDate DESC")
  List<ProjectAgreement> findByProjectIdNotDeleted(@Param("projectId") Long projectId);
}
