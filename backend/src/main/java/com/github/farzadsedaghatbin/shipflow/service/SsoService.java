package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.sso.CreateIdentityProviderRequest;
import com.github.farzadsedaghatbin.shipflow.dto.sso.IdentityProviderDTO;
import com.github.farzadsedaghatbin.shipflow.dto.sso.SsoInitiateResponse;
import com.github.farzadsedaghatbin.shipflow.entity.IdentityProvider;
import com.github.farzadsedaghatbin.shipflow.entity.ProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.ProvisionedVia;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.IdentityProviderRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Business logic for Single Sign-On (SSO) via SAML2 and OIDC.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD for {@link IdentityProvider} configuration (ADMIN only — enforced at controller layer)
 *   <li>Initiate OIDC authorization-code flow (PKCE-style state stored in Redis)
 *   <li>Handle OIDC callback: exchange code → extract {@code sub} → find-or-create User → JWT
 *   <li>Initiate SAML2 redirect (returns SSO URL)
 *   <li>Handle SAML2 callback: base64-decode → XML-parse NameID → find-or-create User → JWT
 * </ul>
 *
 * <p>Note: {@code StringRedisTemplate} is only injected when Redis is on the classpath and
 * {@code spring.cache.type=redis}. For in-memory dev/test profiles a fallback map is used.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SsoService {

  private static final String SSO_STATE_KEY_PREFIX = "sso:state:";
  private static final long STATE_TTL_MINUTES = 5;

  private final IdentityProviderRepository identityProviderRepository;
  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;

  /**
   * Optional — only present in Redis-enabled profiles. When null, a simple
   * in-memory fallback is used (acceptable for tests and local dev without Redis).
   */
  private final Optional<StringRedisTemplate> stringRedisTemplate;

  /** In-memory state fallback used when Redis is not available. */
  private final Map<String, String> stateStore = new java.util.concurrent.ConcurrentHashMap<>();

  @Value("${app.frontend.url:http://localhost:3000}")
  private String frontendUrl;

  // -------------------------------------------------------------------------
  // Provider CRUD
  // -------------------------------------------------------------------------

  /** Returns all enabled providers (safe for public login-page consumption). */
  @Transactional(readOnly = true)
  public List<IdentityProviderDTO> listProviders() {
    return identityProviderRepository.findAllByIsEnabledTrue().stream()
        .map(this::toPublicDto)
        .collect(Collectors.toList());
  }

  /** Returns all providers regardless of enabled state (ADMIN view). */
  @Transactional(readOnly = true)
  public List<IdentityProviderDTO> listAllProviders() {
    return identityProviderRepository.findAll().stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  /** Creates and persists a new IdentityProvider. */
  public IdentityProviderDTO createProvider(CreateIdentityProviderRequest req) {
    IdentityProvider entity = IdentityProvider.builder()
        .name(req.getName())
        .providerType(req.getProviderType())
        .organizationId(req.getOrganizationId())
        .clientId(req.getClientId())
        .clientSecret(req.getClientSecret())
        .metadataUrl(req.getMetadataUrl())
        .entityId(req.getEntityId())
        .ssoUrl(req.getSsoUrl())
        .certificate(req.getCertificate())
        .isEnabled(req.getIsEnabled() != null ? req.getIsEnabled() : false)
        .enforceSso(req.getEnforceSso() != null ? req.getEnforceSso() : false)
        .build();
    IdentityProvider saved = identityProviderRepository.save(entity);
    log.info("SSO: created identity provider id={} name={} type={}",
        saved.getId(), saved.getName(), saved.getProviderType());
    return toDto(saved);
  }

  /** Updates an existing IdentityProvider. Throws {@link ResourceNotFoundException} if not found. */
  public IdentityProviderDTO updateProvider(Long id, CreateIdentityProviderRequest req) {
    IdentityProvider entity = identityProviderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("IdentityProvider not found: " + id));
    entity.setName(req.getName());
    entity.setProviderType(req.getProviderType());
    if (req.getOrganizationId() != null) {
      entity.setOrganizationId(req.getOrganizationId());
    }
    if (req.getClientId() != null) {
      entity.setClientId(req.getClientId());
    }
    if (req.getClientSecret() != null) {
      entity.setClientSecret(req.getClientSecret());
    }
    if (req.getMetadataUrl() != null) {
      entity.setMetadataUrl(req.getMetadataUrl());
    }
    if (req.getEntityId() != null) {
      entity.setEntityId(req.getEntityId());
    }
    if (req.getSsoUrl() != null) {
      entity.setSsoUrl(req.getSsoUrl());
    }
    if (req.getCertificate() != null) {
      entity.setCertificate(req.getCertificate());
    }
    if (req.getIsEnabled() != null) {
      entity.setIsEnabled(req.getIsEnabled());
    }
    if (req.getEnforceSso() != null) {
      entity.setEnforceSso(req.getEnforceSso());
    }
    IdentityProvider saved = identityProviderRepository.save(entity);
    log.info("SSO: updated identity provider id={}", saved.getId());
    return toDto(saved);
  }

  /**
   * Soft-deletes a provider by disabling it (isEnabled=false).
   * User data is never hard-deleted.
   */
  public void deleteProvider(Long id) {
    IdentityProvider entity = identityProviderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("IdentityProvider not found: " + id));
    entity.setIsEnabled(false);
    identityProviderRepository.save(entity);
    log.info("SSO: disabled (soft-deleted) identity provider id={}", id);
  }

  // -------------------------------------------------------------------------
  // OIDC flow
  // -------------------------------------------------------------------------

  /**
   * Builds the OIDC authorization URL, stores state in Redis (or memory fallback),
   * and returns the redirect target + state token to the frontend.
   */
  public SsoInitiateResponse initiateOidc(Long idpId) {
    IdentityProvider idp = identityProviderRepository.findByIdAndIsEnabledTrue(idpId)
        .orElseThrow(() -> new ResourceNotFoundException("Identity provider not found or disabled: " + idpId));

    if (idp.getProviderType() != ProviderType.OIDC) {
      throw new IllegalArgumentException("Provider " + idpId + " is not an OIDC provider");
    }

    String state = UUID.randomUUID().toString();
    // Store idpId in state so the callback can resolve the provider
    String stateValue = idpId + ":" + state;
    storeState(state, stateValue);

    String redirectUrl = buildOidcAuthUrl(idp, state);
    log.debug("SSO OIDC: initiating flow for idp={} state={}", idpId, state);
    return SsoInitiateResponse.builder()
        .redirectUrl(redirectUrl)
        .state(state)
        .build();
  }

  /**
   * Handles the OIDC authorization-code callback.
   *
   * <ol>
   *   <li>Validate state from Redis / memory store
   *   <li>Exchange code for tokens at the IdP's token endpoint
   *   <li>Extract {@code sub} (and optionally {@code email}) from the id_token
   *   <li>Find or create the ShipFlow user
   *   <li>Return a ShipFlow JWT
   * </ol>
   */
  public String handleOidcCallback(String code, String state) {
    String stateValue = retrieveState(state);
    if (stateValue == null) {
      throw new IllegalArgumentException("SSO state is invalid or expired");
    }

    // stateValue format: "{idpId}:{originalState}"
    Long idpId = Long.parseLong(stateValue.split(":", 2)[0]);
    IdentityProvider idp = identityProviderRepository.findByIdAndIsEnabledTrue(idpId)
        .orElseThrow(() -> new ResourceNotFoundException("Identity provider not found: " + idpId));

    Map<String, String> tokenResponse = exchangeOidcCode(idp, code);
    String idToken = tokenResponse.get("id_token");
    if (idToken == null) {
      idToken = tokenResponse.get("access_token");
    }

    Map<String, Object> claims = decodeJwtPayload(idToken);
    String sub = (String) claims.get("sub");
    String email = (String) claims.get("email");

    if (sub == null) {
      throw new IllegalStateException("OIDC token missing 'sub' claim");
    }

    User user = findOrCreateSsoUser(sub, email, idp, ProvisionedVia.OIDC);
    log.info("SSO OIDC: login successful userId={} sub={}", user.getId(), sub);
    return jwtTokenProvider.generateTokenFromUsername(user.getUsername());
  }

  // -------------------------------------------------------------------------
  // SAML2 flow
  // -------------------------------------------------------------------------

  /**
   * Returns the SAML2 SSO URL for browser redirect.
   */
  public String initiateSaml(Long idpId) {
    IdentityProvider idp = identityProviderRepository.findByIdAndIsEnabledTrue(idpId)
        .orElseThrow(() -> new ResourceNotFoundException("Identity provider not found or disabled: " + idpId));

    if (idp.getProviderType() != ProviderType.SAML2) {
      throw new IllegalArgumentException("Provider " + idpId + " is not a SAML2 provider");
    }

    if (idp.getSsoUrl() == null || idp.getSsoUrl().isBlank()) {
      throw new IllegalStateException("SAML2 provider " + idpId + " has no SSO URL configured");
    }

    log.debug("SSO SAML2: redirecting to ssoUrl for idp={}", idpId);
    return idp.getSsoUrl();
  }

  /**
   * Handles a SAML2 Response POST-back.
   *
   * <ol>
   *   <li>Base64-decode the SAMLResponse
   *   <li>Parse XML and extract {@code &lt;saml:NameID&gt;}
   *   <li>Find or create the ShipFlow user
   *   <li>Return a ShipFlow JWT
   * </ol>
   *
   * @param samlResponse base64-encoded SAML response
   * @param relayState   IdP identifier — format: {@code idp:{idpId}} or plain idpId string
   */
  public String handleSamlCallback(String samlResponse, String relayState) {
    Long idpId = parseSamlRelayState(relayState);
    IdentityProvider idp = identityProviderRepository.findByIdAndIsEnabledTrue(idpId)
        .orElseThrow(() -> new ResourceNotFoundException("Identity provider not found: " + idpId));

    String nameId = extractSamlNameId(samlResponse);
    if (nameId == null || nameId.isBlank()) {
      throw new IllegalStateException("SAML response did not contain a NameID");
    }

    User user = findOrCreateSsoUser(nameId, nameId, idp, ProvisionedVia.SAML2);
    log.info("SSO SAML2: login successful userId={} nameId={}", user.getId(), nameId);
    return jwtTokenProvider.generateTokenFromUsername(user.getUsername());
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private String buildOidcAuthUrl(IdentityProvider idp, String state) {
    // Use metadataUrl as the base authorization URL (OIDC discovery endpoint is
    // stripped to its base in production; for simplicity we use it directly here).
    String authUrl = idp.getMetadataUrl();
    if (authUrl == null || authUrl.isBlank()) {
      throw new IllegalStateException("OIDC provider has no metadata URL configured");
    }
    // Strip /.well-known/openid-configuration suffix if present
    if (authUrl.endsWith("/.well-known/openid-configuration")) {
      authUrl = authUrl.substring(0, authUrl.lastIndexOf("/.well-known/openid-configuration"));
    }
    authUrl = authUrl + "/authorize";

    return authUrl
        + "?response_type=code"
        + "&client_id=" + encode(idp.getClientId())
        + "&redirect_uri=" + encode(frontendUrl + "/sso/callback/oidc")
        + "&scope=openid+email+profile"
        + "&state=" + encode(state);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> exchangeOidcCode(IdentityProvider idp, String code) {
    String tokenUrl = idp.getMetadataUrl();
    if (tokenUrl == null || tokenUrl.isBlank()) {
      throw new IllegalStateException("OIDC provider has no metadata URL configured");
    }
    if (tokenUrl.endsWith("/.well-known/openid-configuration")) {
      tokenUrl = tokenUrl.substring(0, tokenUrl.lastIndexOf("/.well-known/openid-configuration"));
    }
    tokenUrl = tokenUrl + "/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("code", code);
    params.add("client_id", idp.getClientId());
    params.add("client_secret", idp.getClientSecret());
    params.add("redirect_uri", frontendUrl + "/sso/callback/oidc");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
    try {
      ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
      JsonNode json = objectMapper.readTree(response.getBody());
      Map<String, String> result = new java.util.HashMap<>();
      json.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
      return result;
    } catch (Exception ex) {
      throw new IllegalStateException("OIDC token exchange failed: " + ex.getMessage(), ex);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> decodeJwtPayload(String jwt) {
    try {
      String[] parts = jwt.split("\\.");
      if (parts.length < 2) {
        throw new IllegalArgumentException("Not a valid JWT");
      }
      // Add padding if needed
      String payloadBase64 = parts[1];
      int padLen = (4 - payloadBase64.length() % 4) % 4;
      payloadBase64 = payloadBase64 + "=".repeat(padLen);
      byte[] decoded = Base64.getUrlDecoder().decode(payloadBase64);
      return objectMapper.readValue(decoded, Map.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decode JWT payload: " + ex.getMessage(), ex);
    }
  }

  private String extractSamlNameId(String samlResponseBase64) {
    try {
      // Strip whitespace/newlines that may be present in form-encoded value
      String cleaned = samlResponseBase64.replaceAll("\\s", "");
      byte[] decoded = Base64.getDecoder().decode(cleaned);

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Disable external entity processing (XXE prevention)
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(new ByteArrayInputStream(decoded));

      // Try both namespaced and non-namespaced NameID elements
      NodeList nameIdNodes = doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "NameID");
      if (nameIdNodes.getLength() == 0) {
        nameIdNodes = doc.getElementsByTagName("saml:NameID");
      }
      if (nameIdNodes.getLength() == 0) {
        nameIdNodes = doc.getElementsByTagName("NameID");
      }
      if (nameIdNodes.getLength() > 0) {
        return nameIdNodes.item(0).getTextContent().trim();
      }
      return null;
    } catch (Exception ex) {
      throw new IllegalStateException("SAML response parsing failed: " + ex.getMessage(), ex);
    }
  }

  private Long parseSamlRelayState(String relayState) {
    if (relayState == null || relayState.isBlank()) {
      throw new IllegalArgumentException("SAML RelayState is missing");
    }
    try {
      // Support "idp:42" or plain "42"
      String numPart = relayState.startsWith("idp:") ? relayState.substring(4) : relayState;
      return Long.parseLong(numPart.trim());
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Invalid SAML RelayState: " + relayState);
    }
  }

  private User findOrCreateSsoUser(
      String externalUserId, String email, IdentityProvider idp, ProvisionedVia via) {
    return userRepository.findByExternalUserIdAndIdentityProviderId(externalUserId, idp.getId())
        .orElseGet(() -> {
          log.info("SSO: provisioning new user externalId={} via={}", externalUserId, via);
          // Derive a username that is unique and safe
          String baseUsername = sanitizeUsername(externalUserId);
          String username = uniqueUsername(baseUsername);
          User newUser = User.builder()
              .username(username)
              .email(email)
              .password(passwordEncoder.encode(UUID.randomUUID().toString()))
              .role(UserRole.MEMBER)
              .isActive(true)
              .identityProvider(idp)
              .externalUserId(externalUserId)
              .provisionedVia(via)
              .build();
          return userRepository.save(newUser);
        });
  }

  private String sanitizeUsername(String raw) {
    // Keep only alphanumeric + _ + - characters, max 50 chars
    String sanitized = raw.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    return sanitized.length() > 50 ? sanitized.substring(0, 50) : sanitized;
  }

  private String uniqueUsername(String base) {
    String candidate = base;
    int attempt = 0;
    while (userRepository.existsByUsername(candidate)) {
      attempt++;
      candidate = base + "_" + attempt;
    }
    return candidate;
  }

  // -------------------------------------------------------------------------
  // State store (Redis with in-memory fallback)
  // -------------------------------------------------------------------------

  private void storeState(String key, String value) {
    stringRedisTemplate.ifPresentOrElse(
        redis -> redis.opsForValue().set(SSO_STATE_KEY_PREFIX + key, value, STATE_TTL_MINUTES, TimeUnit.MINUTES),
        () -> stateStore.put(key, value));
  }

  private String retrieveState(String key) {
    return stringRedisTemplate.map(redis -> {
      String val = redis.opsForValue().get(SSO_STATE_KEY_PREFIX + key);
      redis.delete(SSO_STATE_KEY_PREFIX + key);
      return val;
    }).orElseGet(() -> stateStore.remove(key));
  }

  // -------------------------------------------------------------------------
  // DTO mappers
  // -------------------------------------------------------------------------

  private IdentityProviderDTO toDto(IdentityProvider entity) {
    return IdentityProviderDTO.builder()
        .id(entity.getId())
        .organizationId(entity.getOrganizationId())
        .name(entity.getName())
        .providerType(entity.getProviderType())
        .clientId(entity.getClientId())
        .metadataUrl(entity.getMetadataUrl())
        .entityId(entity.getEntityId())
        .ssoUrl(entity.getSsoUrl())
        .certificate(entity.getCertificate())
        .isEnabled(entity.getIsEnabled())
        .enforceSso(entity.getEnforceSso())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  /** Public-safe DTO — omits certificate PEM. */
  private IdentityProviderDTO toPublicDto(IdentityProvider entity) {
    return IdentityProviderDTO.builder()
        .id(entity.getId())
        .name(entity.getName())
        .providerType(entity.getProviderType())
        .metadataUrl(entity.getMetadataUrl())
        .isEnabled(entity.getIsEnabled())
        .enforceSso(entity.getEnforceSso())
        .build();
  }

  private static String encode(String value) {
    if (value == null) return "";
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
