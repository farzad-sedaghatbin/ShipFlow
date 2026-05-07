package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Evidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
  List<Evidence> findByPitchId(Long pitchId);

  List<Evidence> findByPersonId(Long personId);
}
