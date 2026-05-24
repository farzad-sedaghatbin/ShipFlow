package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkLogTimer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkLogTimerRepository extends JpaRepository<WorkLogTimer, Long> {

  Optional<WorkLogTimer> findByPersonId(Long personId);

  Optional<WorkLogTimer> findByPersonIdAndStatus(Long personId, String status);

  boolean existsByPersonId(Long personId);

  void deleteByPersonId(Long personId);
}
