package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ProjectSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for {@link ProjectSnapshot} entities. */
@Repository
public interface ProjectSnapshotRepository extends JpaRepository<ProjectSnapshot, Long> {

  /** Returns the most recently computed snapshot for the given project, if any. */
  Optional<ProjectSnapshot> findTopByProjectIdOrderByComputedAtDesc(Long projectId);

  /** Removes all snapshots for the given project (called before storing a fresh one). */
  void deleteByProjectId(Long projectId);
}
