package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RetroItemRepository extends JpaRepository<RetroItem, Long> {
    
    List<RetroItem> findByRetrospectiveIdOrderByCreatedAtAsc(Long retrospectiveId);
    
    List<RetroItem> findByRetrospectiveIdAndColumnTypeOrderByCreatedAtAsc(Long retrospectiveId, RetroColumnType columnType);
    
    @Query("SELECT COUNT(i) FROM RetroItem i WHERE i.retrospective.id = :retrospectiveId")
    long countByRetrospectiveId(@Param("retrospectiveId") Long retrospectiveId);
    
    @Query("SELECT i FROM RetroItem i LEFT JOIN FETCH i.author WHERE i.id = :id")
    Optional<RetroItem> findByIdWithAuthor(@Param("id") Long id);
    
    // Find items that are not merged (for display)
    @Query("SELECT i FROM RetroItem i WHERE i.retrospective.id = :retrospectiveId AND i.mergedInto IS NULL ORDER BY i.voteCount DESC, i.createdAt ASC")
    List<RetroItem> findActiveItemsByRetrospectiveId(@Param("retrospectiveId") Long retrospectiveId);
    
    // Find items merged into a specific item
    List<RetroItem> findByMergedIntoId(Long mergedIntoId);
    
    void deleteAllByRetrospectiveId(Long retrospectiveId);
}
