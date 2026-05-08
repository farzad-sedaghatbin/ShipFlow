package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.PitchDependency;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PitchDependencyRepository extends JpaRepository<PitchDependency, Long> {

  /**
   * Find all dependencies where the given pitch is the source (it blocks or
   * depends on others).
   */
  List<PitchDependency> findBySourcePitchId(Long sourcePitchId);

  /**
   * Find all dependencies where the given pitch is the target (it is blocked by or
   * depended upon by others).
   */
  List<PitchDependency> findByTargetPitchId(Long targetPitchId);

  /** Find a specific dependency between two pitches. */
  Optional<PitchDependency> findBySourcePitchIdAndTargetPitchId(Long sourcePitchId, Long targetPitchId);

  /**
   * Find all dependencies where the given pitch is either the source or the target.
   * Used for dependency constraint validation during reorder operations.
   */
  List<PitchDependency> findBySourcePitchIdOrTargetPitchId(Long sourcePitchId, Long targetPitchId);

  /**
   * Batch fetch all dependencies where any of the given pitch IDs appears as source
   * or target. Used to avoid N+1 queries when converting a list of pitches to DTOs.
   */
  List<PitchDependency> findBySourcePitchIdInOrTargetPitchIdIn(
      java.util.Collection<Long> sourceIds, java.util.Collection<Long> targetIds);

  /** Delete a specific dependency between two pitches. */
  void deleteBySourcePitchIdAndTargetPitchId(Long sourcePitchId, Long targetPitchId);
}
