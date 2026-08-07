package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.TaskCycleHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskCycleHistoryRepository extends JpaRepository<TaskCycleHistory, Long> {

  List<TaskCycleHistory> findByTaskIdOrderByRecordedAtAsc(Long taskId);
}
