package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldDefinition;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomFieldDefinitionRepository
    extends JpaRepository<CustomFieldDefinition, Long> {

  List<CustomFieldDefinition> findByEntityTypeAndDeletedAtIsNullOrderBySortOrderAsc(
      CustomFieldEntityType entityType);

  /**
   * Returns all non-deleted definitions applicable to a given entity type in a project, including
   * org-wide (project IS NULL) and project-scoped definitions for the given project.
   */
  @Query(
      """
      SELECT d FROM CustomFieldDefinition d
      WHERE d.entityType = :entityType
        AND d.deletedAt IS NULL
        AND (d.project IS NULL OR d.project.id = :projectId)
      ORDER BY d.sortOrder ASC, d.id ASC
      """)
  List<CustomFieldDefinition> findApplicable(
      @Param("entityType") CustomFieldEntityType entityType, @Param("projectId") Long projectId);

  Optional<CustomFieldDefinition> findByIdAndDeletedAtIsNull(Long id);
}
