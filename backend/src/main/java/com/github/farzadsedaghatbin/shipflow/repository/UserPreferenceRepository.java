package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.UserPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for user preference entities. */
@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

  Optional<UserPreference> findByUserId(Long userId);

  boolean existsByUserId(Long userId);
}
