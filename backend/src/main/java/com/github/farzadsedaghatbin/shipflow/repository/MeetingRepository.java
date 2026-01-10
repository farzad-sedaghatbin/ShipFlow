package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.enums.MeetingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long>, JpaSpecificationExecutor<Meeting> {
    List<Meeting> findByPitchId(Long pitchId);
    List<Meeting> findByType(MeetingType type);
    
    @Query("SELECT m FROM Meeting m WHERE m.pitch.cycle.id = :cycleId ORDER BY m.dateHeld DESC")
    Page<Meeting> findByCycleId(Long cycleId, Pageable pageable);
    
    @Query("SELECT m FROM Meeting m WHERE m.pitch.cycle.project.id = :projectId ORDER BY m.dateHeld DESC")
    Page<Meeting> findByProjectId(Long projectId, Pageable pageable);
}
