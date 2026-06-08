package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.IdentityProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentityProviderRepository extends JpaRepository<IdentityProvider, Long> {

  List<IdentityProvider> findAllByIsEnabledTrue();

  Optional<IdentityProvider> findByIdAndIsEnabledTrue(Long id);
}
