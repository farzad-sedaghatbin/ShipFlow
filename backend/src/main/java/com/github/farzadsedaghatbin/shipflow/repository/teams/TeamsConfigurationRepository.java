package com.github.farzadsedaghatbin.shipflow.repository.teams;

import com.github.farzadsedaghatbin.shipflow.entity.teams.TeamsConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamsConfigurationRepository extends JpaRepository<TeamsConfiguration, Long> {
    
    Optional<TeamsConfiguration> findByTenantName(String tenantName);
    
    Optional<TeamsConfiguration> findFirstByIsEnabledTrue();
}
