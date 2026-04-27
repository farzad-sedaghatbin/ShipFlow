package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WebhookSubscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

  List<WebhookSubscription> findByIsActiveTrue();

  List<WebhookSubscription> findByUserId(Long userId);

  List<WebhookSubscription> findByUserIdAndIsActiveTrue(Long userId);
}
