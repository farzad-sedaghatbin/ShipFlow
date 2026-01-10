package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WorkLogTimer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkLogTimerRepository extends JpaRepository<WorkLogTimer, Long> {
    
    Optional<WorkLogTimer> findByPersonId(Long personId);
    
    boolean existsByPersonId(Long personId);
    
    void deleteByPersonId(Long personId);
}
