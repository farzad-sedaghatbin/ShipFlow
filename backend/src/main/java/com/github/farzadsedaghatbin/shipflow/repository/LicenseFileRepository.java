package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.LicenseFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenseFileRepository extends JpaRepository<LicenseFile, Long> {

  /**
   * The table is meant to hold exactly one row, but this reads the most recently
   * inserted one defensively rather than assuming {@code findAll()} returns
   * exactly one element.
   */
  Optional<LicenseFile> findFirstByOrderByIdDesc();
}
