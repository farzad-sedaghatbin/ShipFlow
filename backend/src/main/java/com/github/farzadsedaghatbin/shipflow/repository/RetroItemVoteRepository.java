package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.RetroItemVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetroItemVoteRepository extends JpaRepository<RetroItemVote, Long> {
    
    Optional<RetroItemVote> findByRetroItemIdAndUserId(Long retroItemId, Long userId);
    
    List<RetroItemVote> findByRetroItemId(Long retroItemId);
    
    boolean existsByRetroItemIdAndUserId(Long retroItemId, Long userId);
    
    void deleteByRetroItemIdAndUserId(Long retroItemId, Long userId);
}
