package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.EpicDependency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EpicDependencyRepository extends JpaRepository<EpicDependency, Long> {

  /**
   * Find all dependencies where the given epic is the source (it blocks or
   * depends on others).
   */
  List<EpicDependency> findBySourceEpicId(Long sourceEpicId);

  /**
   * Find all dependencies where the given epic is the target (it is blocked by or
   * depended upon by others).
   */
  List<EpicDependency> findByTargetEpicId(Long targetEpicId);

  /** Find a specific dependency between two epics. */
  Optional<EpicDependency> findBySourceEpicIdAndTargetEpicId(Long sourceEpicId, Long targetEpicId);

  /**
   * Find all dependencies where the given epic is either the source or the target.
   */
  List<EpicDependency> findBySourceEpicIdOrTargetEpicId(Long sourceEpicId, Long targetEpicId);

  /** Delete a specific dependency between two epics. */
  void deleteBySourceEpicIdAndTargetEpicId(Long sourceEpicId, Long targetEpicId);
}
