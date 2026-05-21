package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ImportJob;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportJobStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

  List<ImportJob> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

  List<ImportJob> findByStatusOrderByCreatedAtDesc(ImportJobStatus status);
}
