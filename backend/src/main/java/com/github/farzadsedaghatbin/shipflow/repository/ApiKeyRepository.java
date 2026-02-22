package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  Optional<ApiKey> findByKeyHash(String keyHash);

  List<ApiKey> findByUserIdAndIsActiveTrue(Long userId);

  List<ApiKey> findByUserId(Long userId);

  boolean existsByKeyHash(String keyHash);
}
