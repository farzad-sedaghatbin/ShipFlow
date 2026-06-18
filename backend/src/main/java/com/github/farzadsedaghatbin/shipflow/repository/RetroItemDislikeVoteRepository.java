package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.RetroItemDislikeVote;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RetroItemDislikeVoteRepository extends JpaRepository<RetroItemDislikeVote, Long> {

  boolean existsByRetroItemIdAndUserId(Long retroItemId, Long userId);

  void deleteByRetroItemIdAndUserId(Long retroItemId, Long userId);

  List<RetroItemDislikeVote> findByRetroItemId(Long retroItemId);

  @Query(
      "SELECT v.retroItem.id FROM RetroItemDislikeVote v"
          + " WHERE v.retroItem.id IN :itemIds AND v.user.id = :userId")
  Set<Long> findDislikedItemIds(
      @Param("itemIds") List<Long> itemIds, @Param("userId") Long userId);
}
