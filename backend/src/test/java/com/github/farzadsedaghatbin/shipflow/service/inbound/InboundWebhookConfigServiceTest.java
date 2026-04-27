package com.github.farzadsedaghatbin.shipflow.service.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.inbound.CreateInboundWebhookConfigRequest;
import com.github.farzadsedaghatbin.shipflow.dto.inbound.InboundWebhookConfigDTO;
import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link InboundWebhookConfigService}.
 *
 * @since 0.6.0
 */
class InboundWebhookConfigServiceTest {

    @Mock
    private InboundWebhookConfigRepository repository;

    @InjectMocks
    private InboundWebhookConfigService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(service, "appBaseUrl", "https://shipflow.example.com");
    }

    // ── helpers ──────────────────────────────────────────────────────

    private InboundWebhookConfig stubConfig(String providerName) {
        InboundWebhookConfig c = new InboundWebhookConfig();
        c.setId(1L);
        c.setProviderName(providerName);
        c.setDisplayName("Test Provider");
        c.setWebhookSecret("supersecretkey1234");
        c.setSignatureHeader("x-hub-signature-256");
        c.setHmacAlgorithm("HmacSHA256");
        c.setIsEnabled(true);
        return c;
    }

    private CreateInboundWebhookConfigRequest newRequest(String providerName) {
        CreateInboundWebhookConfigRequest r = new CreateInboundWebhookConfigRequest();
        r.setProviderName(providerName);
        r.setDisplayName("Display " + providerName);
        r.setWebhookSecret("s3cr3t12345abcd");
        r.setHmacAlgorithm("HmacSHA256");
        r.setSignatureHeader("x-hub-signature-256");
        return r;
    }

    // ── createOrUpdate ───────────────────────────────────────────────

    @Nested
    @DisplayName("createOrUpdate()")
    class CreateOrUpdateTests {

        @Test
        @DisplayName("creates new config when provider does not exist")
        void createsNewConfig() {
            InboundWebhookConfig saved = stubConfig("zendesk");
            when(repository.findByProviderName("zendesk")).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(saved);

            InboundWebhookConfigDTO dto = service.createOrUpdate(newRequest("zendesk"));

            assertThat(dto).isNotNull();
            assertThat(dto.getProviderName()).isEqualTo("zendesk");
            assertThat(dto.getWebhookUrl())
                .isEqualTo("https://shipflow.example.com/api/inbound/zendesk");
        }

        @Test
        @DisplayName("updates existing config when provider already exists")
        void updatesExistingConfig() {
            InboundWebhookConfig existing = stubConfig("zendesk");
            CreateInboundWebhookConfigRequest req = newRequest("zendesk");
            req.setDisplayName("Zendesk Updated");

            when(repository.findByProviderName("zendesk")).thenReturn(Optional.of(existing));
            when(repository.save(any())).thenReturn(existing);

            service.createOrUpdate(req);

            verify(repository, times(1)).save(existing);
            assertThat(existing.getDisplayName()).isEqualTo("Zendesk Updated");
        }

        @Test
        @DisplayName("defaults to HmacSHA256 when algorithm not specified")
        void defaultsAlgorithm() {
            CreateInboundWebhookConfigRequest req = newRequest("pagerduty");
            req.setHmacAlgorithm(null);

            InboundWebhookConfig saved = stubConfig("pagerduty");
            when(repository.findByProviderName("pagerduty")).thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(inv -> {
                InboundWebhookConfig c = inv.getArgument(0);
                assertThat(c.getHmacAlgorithm()).isEqualTo("HmacSHA256");
                return saved;
            });

            service.createOrUpdate(req);
        }
    }

    // ── getAll ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll()")
    class GetAllTests {

        @Test
        @DisplayName("returns all configs as DTOs")
        void returnsList() {
            when(repository.findAll()).thenReturn(List.of(stubConfig("slack")));
            List<InboundWebhookConfigDTO> result = service.getAll();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProviderName()).isEqualTo("slack");
        }

        @Test
        @DisplayName("returns empty list when no configs")
        void returnsEmpty() {
            when(repository.findAll()).thenReturn(Collections.emptyList());
            assertThat(service.getAll()).isEmpty();
        }
    }

    // ── toggleEnabled ────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleEnabled()")
    class ToggleEnabledTests {

        @Test
        @DisplayName("flips enabled=true to enabled=false")
        void disablesWhenEnabled() {
            InboundWebhookConfig config = stubConfig("test");
            config.setIsEnabled(true);
            when(repository.findById(1L)).thenReturn(Optional.of(config));
            when(repository.save(config)).thenReturn(config);

            service.toggleEnabled(1L);

            assertThat(config.getIsEnabled()).isFalse();
        }

        @Test
        @DisplayName("flips enabled=false to enabled=true")
        void enablesWhenDisabled() {
            InboundWebhookConfig config = stubConfig("test");
            config.setIsEnabled(false);
            when(repository.findById(1L)).thenReturn(Optional.of(config));
            when(repository.save(config)).thenReturn(config);

            service.toggleEnabled(1L);

            assertThat(config.getIsEnabled()).isTrue();
        }

        @Test
        @DisplayName("throws RuntimeException when ID not found")
        void throwsWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.toggleEnabled(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
        }
    }

    // ── secret masking ───────────────────────────────────────────────

    @Nested
    @DisplayName("Secret masking")
    class SecretMaskingTests {

        @Test
        @DisplayName("masks secret as first4 + **** + last4")
        void masksLongSecret() {
            InboundWebhookConfig c = stubConfig("test");
            c.setWebhookSecret("abcd1234efgh5678");
            when(repository.findAll()).thenReturn(List.of(c));

            String masked = service.getAll().get(0).getWebhookSecret();

            assertThat(masked).startsWith("abcd").endsWith("5678").contains("****");
            assertThat(masked).doesNotContain("1234efgh");
        }

        @Test
        @DisplayName("returns short secret (<8 chars) as-is without masking")
        void shortSecretNotMasked() {
            InboundWebhookConfig c = stubConfig("test");
            c.setWebhookSecret("abc");
            when(repository.findAll()).thenReturn(List.of(c));

            assertThat(service.getAll().get(0).getWebhookSecret()).isEqualTo("abc");
        }

        @Test
        @DisplayName("null secret is returned as null")
        void nullSecretHandled() {
            InboundWebhookConfig c = stubConfig("test");
            c.setWebhookSecret(null);
            when(repository.findAll()).thenReturn(List.of(c));

            assertThat(service.getAll().get(0).getWebhookSecret()).isNull();
        }
    }

    // ── webhookUrl ───────────────────────────────────────────────────

    @Test
    @DisplayName("webhookUrl includes base URL and provider name")
    void webhookUrl_isBuiltCorrectly() {
        when(repository.findAll()).thenReturn(List.of(stubConfig("github")));
        assertThat(service.getAll().get(0).getWebhookUrl())
            .isEqualTo("https://shipflow.example.com/api/inbound/github");
    }

    // ── delete ───────────────────────────────────────────────────────

    @Test
    @DisplayName("delete() delegates to repository.deleteById()")
    void delete_callsRepository() {
        service.delete(42L);
        verify(repository).deleteById(42L);
    }
}
