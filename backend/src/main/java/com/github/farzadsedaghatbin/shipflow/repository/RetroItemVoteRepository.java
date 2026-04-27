package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.RetroItemVote;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RetroItemVoteRepository extends JpaRepository<RetroItemVote, Long> {

  Optional<RetroItemVote> findByRetroItemIdAndUserId(Long retroItemId, Long userId);

  List<RetroItemVote> findByRetroItemId(Long retroItemId);

  boolean existsByRetroItemIdAndUserId(Long retroItemId, Long userId);

  void deleteByRetroItemIdAndUserId(Long retroItemId, Long userId);

  /**
   * Batch check which items have been voted on by a user.
   * Fixes N+1 pattern in toItemDTO mapping.
   */
  @Query("SELECT v.retroItem.id FROM RetroItemVote v WHERE v.retroItem.id IN :itemIds AND v.user.id = :userId")
  Set<Long> findVotedItemIds(@Param("itemIds") List<Long> itemIds, @Param("userId") Long userId);
}
