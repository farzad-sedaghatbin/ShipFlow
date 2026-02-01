package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for OrganizationSettings entity. */
@Repository
public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, Long> {

  /** Find the first (and should be only) organization settings record. */
  Optional<OrganizationSettings> findFirstByOrderByIdAsc();
}
