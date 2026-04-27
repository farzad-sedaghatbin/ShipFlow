package com.github.farzadsedaghatbin.shipflow.repository.inbound;

import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InboundWebhookConfigRepository
    extends JpaRepository<InboundWebhookConfig, Long> {

    Optional<InboundWebhookConfig> findByProviderName(String providerName);

    List<InboundWebhookConfig> findByIsEnabledTrue();

    boolean existsByProviderName(String providerName);
}
