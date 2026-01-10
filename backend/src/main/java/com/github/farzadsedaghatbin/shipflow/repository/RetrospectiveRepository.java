package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {
    List<Retrospective> findByCycleId(Long cycleId);
    List<Retrospective> findByProjectId(Long projectId);
}
