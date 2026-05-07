package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ManualNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ManualNoteRepository extends JpaRepository<ManualNote, Long> {

  /** Find notes by context. */
  List<ManualNote> findByContextTypeAndContextId(String contextType, Long contextId);

  /** Find notes by author. */
  List<ManualNote> findByAuthorId(Long authorId);

  /** Find notes by cycle ID. */
  List<ManualNote> findByCycleId(Long cycleId);

  /** Find notes by team ID. */
  List<ManualNote> findByTeamId(Long teamId);

  /** Find notes by pitch ID. */
  List<ManualNote> findByPitchId(Long pitchId);

  /** Find notes that should be included in knowledge base. */
  List<ManualNote> findByIncludeInKnowledgeTrue();
}
