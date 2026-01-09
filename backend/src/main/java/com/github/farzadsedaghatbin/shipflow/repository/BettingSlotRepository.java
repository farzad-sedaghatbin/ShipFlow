package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.BettingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BettingSlotRepository extends JpaRepository<BettingSlot, Long> {
    
    List<BettingSlot> findByCycleId(Long cycleId);
    
    List<BettingSlot> findByCycleIdAndTeamId(Long cycleId, Long teamId);
    
    List<BettingSlot> findByTeamId(Long teamId);
    
    Optional<BettingSlot> findByPitchId(Long pitchId);
    
    @Query("SELECT bs FROM BettingSlot bs WHERE bs.cycle.id = :cycleId AND bs.team.id = :teamId AND bs.position = :position")
    Optional<BettingSlot> findByCycleIdAndTeamIdAndPosition(
            @Param("cycleId") Long cycleId, 
            @Param("teamId") Long teamId, 
            @Param("position") Integer position);
    
    @Query("SELECT bs FROM BettingSlot bs WHERE bs.pitch IS NOT NULL AND bs.cycle.id = :cycleId")
    List<BettingSlot> findAssignedSlotsByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT bs FROM BettingSlot bs WHERE bs.pitch IS NULL AND bs.cycle.id = :cycleId")
    List<BettingSlot> findEmptySlotsByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT COUNT(bs) FROM BettingSlot bs WHERE bs.pitch IS NOT NULL AND bs.cycle.id = :cycleId")
    long countAssignedByCycleId(@Param("cycleId") Long cycleId);
    
    @Query("SELECT COALESCE(MAX(bs.position), -1) FROM BettingSlot bs WHERE bs.cycle.id = :cycleId AND bs.team.id = :teamId")
    Integer findMaxPositionByCycleAndTeam(@Param("cycleId") Long cycleId, @Param("teamId") Long teamId);
    
    void deleteByCycleId(Long cycleId);
}
