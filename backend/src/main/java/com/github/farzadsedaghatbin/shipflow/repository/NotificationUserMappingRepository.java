package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.NotificationUserMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationUserMappingRepository extends JpaRepository<NotificationUserMapping, Long> {

  Optional<NotificationUserMapping> findByPersonIdAndProviderName(Long personId, String providerName);

  List<NotificationUserMapping> findByPersonId(Long personId);

  void deleteByPersonIdAndProviderName(Long personId, String providerName);
}
