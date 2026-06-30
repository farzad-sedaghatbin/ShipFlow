package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, Long> {

  List<CustomFieldValue> findByEntityTypeAndEntityId(
      CustomFieldEntityType entityType, Long entityId);

  Optional<CustomFieldValue> findByDefinitionIdAndEntityTypeAndEntityId(
      Long definitionId, CustomFieldEntityType entityType, Long entityId);

  /** Hard-delete all values for a definition (called before soft-deleting the definition). */
  @Modifying
  @Query("DELETE FROM CustomFieldValue v WHERE v.definition.id = :definitionId")
  void deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
