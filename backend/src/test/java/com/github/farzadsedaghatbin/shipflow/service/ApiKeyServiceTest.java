package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.publicapi.ApiKeyDTO;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.CreateApiKeyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import com.github.farzadsedaghatbin.shipflow.repository.ApiKeyRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

  @Mock
  private ApiKeyRepository apiKeyRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ApiKeyService apiKeyService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = User.builder().id(1L).username("testuser").email("test@example.com").build();
  }

  @Test
  void createApiKey_shouldReturnDtoWithRawKey() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
      ApiKey key = inv.getArgument(0);
      key.setId(42L);
      return key;
    });

    CreateApiKeyRequest request = CreateApiKeyRequest.builder()
        .name("CI Key")
        .scopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE))
        .build();

    ApiKeyDTO dto = apiKeyService.createApiKey(1L, request);

    assertThat(dto.getName()).isEqualTo("CI Key");
    assertThat(dto.getRawKey()).isNotNull().startsWith("sf_");
    assertThat(dto.getKeyPrefix()).hasSize(8);
    assertThat(dto.getScopes()).containsExactlyInAnyOrder(ApiKeyScope.READ, ApiKeyScope.WRITE);
    assertThat(dto.getIsActive()).isTrue();

    // Verify the saved entity has a hashed key
    ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
    verify(apiKeyRepository).save(captor.capture());
    assertThat(captor.getValue().getKeyHash()).isNotBlank();
    assertThat(captor.getValue().getKeyHash()).isNotEqualTo(dto.getRawKey());
  }

  @Test
  void validateKey_shouldReturnApiKeyWhenValid() {
    String rawKey = "sf_test_key_123456789";
    String hash = ApiKeyService.sha256(rawKey);

    ApiKey apiKey = ApiKey.builder()
        .id(1L).name("test").keyPrefix("sf_test_").keyHash(hash)
        .user(testUser).isActive(true).build();

    when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(apiKey));

    Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(1L);
  }

  @Test
  void validateKey_shouldReturnEmptyWhenInvalid() {
    when(apiKeyRepository.findByKeyHash(anyString())).thenReturn(Optional.empty());

    Optional<ApiKey> result = apiKeyService.validateKey("bad_key");

    assertThat(result).isEmpty();
  }

  @Test
  void validateKey_shouldReturnEmptyWhenRevoked() {
    String rawKey = "sf_test_key_revoked";
    String hash = ApiKeyService.sha256(rawKey);

    ApiKey revokedKey = ApiKey.builder()
        .id(2L).name("revoked").keyPrefix("sf_test_").keyHash(hash)
        .user(testUser).isActive(false)
        .revokedAt(java.time.LocalDateTime.now())
        .build();

    when(apiKeyRepository.findByKeyHash(hash)).thenReturn(Optional.of(revokedKey));

    Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

    assertThat(result).isEmpty();
  }

  @Test
  void revokeKey_shouldDeactivateKey() {
    ApiKey key = ApiKey.builder().id(1L).name("test").user(testUser).isActive(true).build();
    when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));
    when(apiKeyRepository.save(any())).thenReturn(key);

    apiKeyService.revokeKey(1L, 1L);

    assertThat(key.getIsActive()).isFalse();
    assertThat(key.getRevokedAt()).isNotNull();
    verify(apiKeyRepository).save(key);
  }

  @Test
  void revokeKey_shouldRejectDifferentUser() {
    ApiKey key = ApiKey.builder().id(1L).name("test").user(testUser).isActive(true).build();
    when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(key));

    assertThatThrownBy(() -> apiKeyService.revokeKey(1L, 999L))
        .isInstanceOf(SecurityException.class);
  }

  @Test
  void listKeys_shouldReturnDtosWithoutRawKey() {
    ApiKey key = ApiKey.builder()
        .id(1L).name("test").keyPrefix("sf_test_").keyHash("hash")
        .user(testUser).isActive(true).scopes(Set.of(ApiKeyScope.READ))
        .build();
    when(apiKeyRepository.findByUserId(1L)).thenReturn(List.of(key));

    List<ApiKeyDTO> keys = apiKeyService.listKeys(1L);

    assertThat(keys).hasSize(1);
    assertThat(keys.get(0).getRawKey()).isNull();
    assertThat(keys.get(0).getName()).isEqualTo("test");
  }

  @Test
  void sha256_shouldBeConsistent() {
    String hash1 = ApiKeyService.sha256("test");
    String hash2 = ApiKeyService.sha256("test");
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).hasSize(64); // 256 bits = 64 hex chars
  }
}
