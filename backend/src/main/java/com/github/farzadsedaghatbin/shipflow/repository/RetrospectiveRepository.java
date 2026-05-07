package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {
  List<Retrospective> findByCycleId(Long cycleId);

  List<Retrospective> findByProjectId(Long projectId);
}
