package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.RetroItemVote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetroItemVoteRepository extends JpaRepository<RetroItemVote, Long> {

  Optional<RetroItemVote> findByRetroItemIdAndUserId(Long retroItemId, Long userId);

  List<RetroItemVote> findByRetroItemId(Long retroItemId);

  boolean existsByRetroItemIdAndUserId(Long retroItemId, Long userId);

  void deleteByRetroItemIdAndUserId(Long retroItemId, Long userId);
}
