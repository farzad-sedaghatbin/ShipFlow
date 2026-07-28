package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.PasskeyCredential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, Long> {

  List<PasskeyCredential> findByUserId(Long userId);

  Optional<PasskeyCredential> findByCredentialId(String credentialId);

  Optional<PasskeyCredential> findByIdAndUserId(Long id, Long userId);

  boolean existsByCredentialId(String credentialId);
}
