package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.AgentActionProposal;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AgentActionStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AgentActionProposalRepository extends JpaRepository<AgentActionProposal, Long> {

  List<AgentActionProposal> findByCommentIdOrderByIdAsc(Long commentId);

  List<AgentActionProposal> findByCommentIdInOrderByIdAsc(Collection<Long> commentIds);

  /**
   * Atomically moves a proposal from one status to another. Returns 1 when this caller won the
   * transition and 0 when it was no longer in {@code from} — which is what stops a double click or
   * two tabs from running the same change twice.
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE AgentActionProposal p SET p.status = :to, p.resolvedAt = :now"
          + " WHERE p.id = :id AND p.status = :from")
  int transition(
      @Param("id") Long id,
      @Param("from") AgentActionStatus from,
      @Param("to") AgentActionStatus to,
      @Param("now") LocalDateTime now);
}
