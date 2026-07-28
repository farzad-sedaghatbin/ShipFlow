package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.PushSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository for Web Push subscription entities. */
@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

  Optional<PushSubscription> findByEndpoint(String endpoint);

  List<PushSubscription> findByUserId(Long userId);

  void deleteByEndpoint(String endpoint);
}
