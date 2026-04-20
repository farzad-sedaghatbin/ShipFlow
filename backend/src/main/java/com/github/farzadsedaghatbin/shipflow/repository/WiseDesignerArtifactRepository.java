package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WiseDesignerArtifact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WiseDesignerArtifactRepository extends JpaRepository<WiseDesignerArtifact, Long> {

  /** All artifacts for a pitch, newest first. */
  List<WiseDesignerArtifact> findByPitchIdOrderByCreatedAtDesc(Long pitchId);

  /** Most recent artifact for a pitch (the "current" design). */
  @Query("SELECT a FROM WiseDesignerArtifact a WHERE a.pitchId = :pitchId ORDER BY a.createdAt DESC LIMIT 1")
  Optional<WiseDesignerArtifact> findLatestByPitchId(@Param("pitchId") Long pitchId);

  /** Find by conversation session ID for iteration. */
  Optional<WiseDesignerArtifact> findBySessionId(String sessionId);
}
