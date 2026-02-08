package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long>, JpaSpecificationExecutor<Meeting> {
  List<Meeting> findByPitchId(Long pitchId);

  List<Meeting> findByType(String type);

  @Query("SELECT m FROM Meeting m WHERE m.pitch.cycle.id = :cycleId ORDER BY m.dateHeld DESC")
  Page<Meeting> findByCycleId(Long cycleId, Pageable pageable);

  @Query("SELECT m FROM Meeting m WHERE m.pitch.cycle.project.id = :projectId ORDER BY m.dateHeld DESC")
  Page<Meeting> findByProjectId(Long projectId, Pageable pageable);
}
