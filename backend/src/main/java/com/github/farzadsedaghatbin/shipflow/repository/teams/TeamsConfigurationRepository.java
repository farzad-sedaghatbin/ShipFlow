package com.github.farzadsedaghatbin.shipflow.repository.teams;

import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsConfiguration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamsConfigurationRepository extends JpaRepository<TeamsConfiguration, Long> {

  Optional<TeamsConfiguration> findByTenantName(String tenantName);

  Optional<TeamsConfiguration> findFirstByIsEnabledTrue();
}
