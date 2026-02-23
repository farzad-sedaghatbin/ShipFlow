package com.github.farzadsedaghatbin.shipflow.service.inbound;

import com.github.farzadsedaghatbin.shipflow.dto.inbound.CreateInboundWebhookConfigRequest;
import com.github.farzadsedaghatbin.shipflow.dto.inbound.InboundWebhookConfigDTO;
import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD service for admin-managed inbound webhook configurations.
 *
 * @since 0.6.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InboundWebhookConfigService {

    private final InboundWebhookConfigRepository repository;

    @Value("${app.base-url:}")
    private String appBaseUrl;

    // ── Create / Update ──────────────────────────────────────────────

    public InboundWebhookConfigDTO createOrUpdate(CreateInboundWebhookConfigRequest request) {
        Optional<InboundWebhookConfig> existing =
            repository.findByProviderName(request.getProviderName());

        InboundWebhookConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            if (request.getDisplayName() != null) config.setDisplayName(request.getDisplayName());
            if (request.getWebhookSecret() != null) config.setWebhookSecret(request.getWebhookSecret());
            if (request.getSignatureHeader() != null) config.setSignatureHeader(request.getSignatureHeader());
            if (request.getHmacAlgorithm() != null) config.setHmacAlgorithm(request.getHmacAlgorithm());
            if (request.getDescription() != null) config.setDescription(request.getDescription());
            if (request.getIsEnabled() != null) config.setIsEnabled(request.getIsEnabled());
        } else {
            config = InboundWebhookConfig.builder()
                .providerName(request.getProviderName())
                .displayName(request.getDisplayName())
                .webhookSecret(request.getWebhookSecret())
                .signatureHeader(request.getSignatureHeader())
                .hmacAlgorithm(request.getHmacAlgorithm() != null
                    ? request.getHmacAlgorithm() : "HmacSHA256")
                .description(request.getDescription())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true)
                .build();
        }

        InboundWebhookConfig saved = repository.save(config);
        log.info("Created/updated inbound webhook config for provider: {}", saved.getProviderName());
        return toDTO(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InboundWebhookConfigDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<InboundWebhookConfigDTO> getById(Long id) {
        return repository.findById(id).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Optional<InboundWebhookConfigDTO> getByProviderName(String providerName) {
        return repository.findByProviderName(providerName).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<InboundWebhookConfigDTO> getEnabled() {
        return repository.findByIsEnabledTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Delete ───────────────────────────────────────────────────────

    public void delete(Long id) {
        repository.deleteById(id);
        log.info("Deleted inbound webhook config: {}", id);
    }

    // ── Toggle ───────────────────────────────────────────────────────

    public InboundWebhookConfigDTO toggleEnabled(Long id) {
        InboundWebhookConfig config = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inbound webhook config not found: " + id));
        config.setIsEnabled(!config.getIsEnabled());
        InboundWebhookConfig saved = repository.save(config);
        log.info("Toggled inbound webhook config {} to enabled={}", id, saved.getIsEnabled());
        return toDTO(saved);
    }

    // ── Mapping ──────────────────────────────────────────────────────

    private InboundWebhookConfigDTO toDTO(InboundWebhookConfig entity) {
        String webhookUrl = buildWebhookUrl(entity.getProviderName());
        return InboundWebhookConfigDTO.builder()
            .id(entity.getId())
            .providerName(entity.getProviderName())
            .displayName(entity.getDisplayName())
            .webhookSecret(maskSecret(entity.getWebhookSecret()))
            .signatureHeader(entity.getSignatureHeader())
            .hmacAlgorithm(entity.getHmacAlgorithm())
            .description(entity.getDescription())
            .isEnabled(entity.getIsEnabled())
            .webhookUrl(webhookUrl)
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private String buildWebhookUrl(String providerName) {
        String base = (appBaseUrl != null && !appBaseUrl.isBlank())
            ? appBaseUrl
            : "https://your-shipflow-instance.com";
        return base + "/api/inbound/" + providerName;
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.length() < 8) return secret;
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}
