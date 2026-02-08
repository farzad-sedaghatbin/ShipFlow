package com.github.farzadsedaghatbin.shipflow.service.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.github.*;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubAppInstallation;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubAppInstallationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubRepositoryRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Service for handling GitHub App OAuth flow and installation management.
 *
 * <p>
 * This service enables organization-wide repository access through GitHub App
 * installation, eliminating the need to add repositories one by one. When an
 * admin authorizes the GitHub App for their organization, all repositories (or
 * selected ones) become accessible automatically.
 *
 * <p>
 * Flow: 1. Admin clicks "Connect GitHub" in ShipFlow 2. ShipFlow redirects to
 * GitHub App installation URL 3. Admin authorizes the app for their
 * organization 4. GitHub redirects back with installation_id 5. ShipFlow
 * fetches all repositories and syncs them
 *
 * <p>
 * Benefits: - Single authorization grants access to all organization
 * repositories - New repositories automatically become accessible (if "all" is
 * selected) - Webhooks are automatically configured for all repositories - No
 * need to add repositories one by one
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GitHubAppOAuthService {

  private final GitHubAppInstallationRepository installationRepository;
  private final GitHubRepositoryRepository repositoryRepository;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  @Value("${app.github.app.id:}")
  private String appId;

  @Value("${app.github.app.name:shipflow}")
  private String appName;

  @Value("${app.github.app.private-key:}")
  private String privateKey;

  @Value("${app.github.app.client-id:}")
  private String clientId;

  @Value("${app.github.app.client-secret:}")
  private String clientSecret;

  @Value("${app.github.app.webhook-secret:}")
  private String webhookSecret;

  @Value("${app.base-url:}")
  private String configuredBaseUrl;

  private static final String GITHUB_API_URL = "https://api.github.com";
  private static final String GITHUB_APP_URL = "https://github.com/apps";

  // Store OAuth states temporarily (in production, use Redis or database)
  private final Map<String, OAuthState> oauthStates = new ConcurrentHashMap<>();

  /**
   * Get the base URL - uses configured value or provided request base URL The
   * base URL is needed for OAuth callbacks where GitHub redirects back to
   * ShipFlow
   */
  public String getBaseUrl(String requestBaseUrl) {
    // Priority: 1) Configured value, 2) Request-provided value, 3) Default
    if (configuredBaseUrl != null && !configuredBaseUrl.isEmpty()) {
      return configuredBaseUrl;
    }
    if (requestBaseUrl != null && !requestBaseUrl.isEmpty()) {
      return requestBaseUrl;
    }
    return "http://localhost:8080";
  }

  /**
   * Generate the GitHub App installation URL for organization-wide authorization
   *
   * @param request
   *            OAuth initialization request
   * @return Response with authorization URL
   */
  public GitHubOAuthUrlResponse generateAuthorizationUrl(GitHubOAuthInitRequest request) {
    String baseUrl = getBaseUrl(request.getBaseUrl());
    // Generate state for CSRF protection
    String state = request.getState() != null ? request.getState() : UUID.randomUUID().toString();

    // Store state with redirect URL for verification
    OAuthState oauthState = new OAuthState(state,
        request.getRedirectUrl() != null ? request.getRedirectUrl() : baseUrl + "/settings/integrations",
        LocalDateTime.now().plusMinutes(10) // 10-minute expiry
    );
    oauthStates.put(state, oauthState);

    // Build the GitHub App installation URL
    // This is different from OAuth apps - users install the GitHub App directly
    StringBuilder authUrl = new StringBuilder(GITHUB_APP_URL);
    authUrl.append("/").append(appName);
    authUrl.append("/installations/new");

    // Add state parameter for CSRF protection
    authUrl.append("?state=").append(state);

    // Suggest specific organization if provided
    if (request.getSuggestedOrganization() != null && !request.getSuggestedOrganization().isEmpty()) {
      authUrl.append("&suggested_target_id=").append(request.getSuggestedOrganization());
    }

    String callbackUrl = baseUrl + "/api/github/app/callback";

    return GitHubOAuthUrlResponse.builder().authorizationUrl(authUrl.toString()).state(state)
        .callbackUrl(callbackUrl)
        .instructions("Click the authorization URL to install the ShipFlow GitHub App for your organization. "
            + "You can choose to grant access to all repositories or select specific ones. "
            + "This is a one-time setup that will automatically sync all authorized repositories.")
        .build();
  }

  /**
   * Process the OAuth callback from GitHub after app installation
   *
   * @param installationId
   *            GitHub App installation ID
   * @param state
   *            State parameter for CSRF verification
   * @param setupAction
   *            Action taken (install, update, etc.)
   * @return Result of the callback processing
   */
  public GitHubOAuthCallbackResult processCallback(Long installationId, String state, String setupAction) {
    log.info("Processing GitHub App callback: installationId={}, setupAction={}", installationId, setupAction);

    // Verify state to prevent CSRF attacks
    OAuthState oauthState = oauthStates.remove(state);
    if (oauthState == null || oauthState.expiresAt.isBefore(LocalDateTime.now())) {
      return GitHubOAuthCallbackResult.builder().success(false).error("invalid_state")
          .errorDescription("The authorization state is invalid or expired. Please try again.").build();
    }

    try {
      // Generate JWT for authenticating as the GitHub App
      String jwt = generateJwt();

      // Fetch installation details from GitHub
      GitHubAppInstallation installation = fetchAndSaveInstallation(installationId, jwt);

      // Fetch and sync all accessible repositories
      List<GitHubRepository> repos = syncRepositoriesForInstallation(installation);

      List<String> repoNames = repos.stream().map(GitHubRepository::getFullName).toList();

      return GitHubOAuthCallbackResult.builder().success(true).installation(mapToDTO(installation))
          .repositoriesSynced(repos.size()).repositoryNames(repoNames).redirectUrl(oauthState.redirectUrl)
          .message(String.format("Successfully connected %s with access to %d repositories!",
              installation.getAccountLogin(), repos.size()))
          .build();

    } catch (Exception e) {
      log.error("Failed to process GitHub App callback", e);
      return GitHubOAuthCallbackResult.builder().success(false).error("processing_failed")
          .errorDescription("Failed to complete GitHub App installation: " + e.getMessage())
          .redirectUrl(oauthState.redirectUrl).build();
    }
  }

  /** Fetch installation details from GitHub and save to database */
  private GitHubAppInstallation fetchAndSaveInstallation(Long installationId, String jwt) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt);
    headers.set("Accept", "application/vnd.github+json");
    headers.set("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(GITHUB_API_URL + "/app/installations/" + installationId,
        HttpMethod.GET, entity, String.class);

    try {
      JsonNode installationNode = objectMapper.readTree(response.getBody());

      // Check if installation already exists
      GitHubAppInstallation installation = installationRepository.findByInstallationId(installationId)
          .orElse(new GitHubAppInstallation());

      // Update installation details
      installation.setInstallationId(installationId);
      installation.setAccountType(installationNode.path("account").path("type").asText());
      installation.setAccountLogin(installationNode.path("account").path("login").asText());
      installation.setAccountId(installationNode.path("account").path("id").asLong());
      installation.setRepositorySelection(installationNode.path("repository_selection").asText());
      installation.setTargetId(installationNode.path("target_id").asLong());
      installation.setTargetType(installationNode.path("target_type").asText());

      // Parse permissions
      JsonNode permsNode = installationNode.path("permissions");
      installation.setPermissions(objectMapper.writeValueAsString(permsNode));

      // Parse events
      JsonNode eventsNode = installationNode.path("events");
      if (eventsNode.isArray()) {
        List<String> events = new ArrayList<>();
        eventsNode.forEach(e -> events.add(e.asText()));
        installation.setSubscribedEvents(String.join(",", events));
      }

      installation.setIsActive(true);
      installation.setWebhooksConfigured(true); // GitHub App handles webhooks automatically
      installation.setLastSyncAt(LocalDateTime.now());

      // Generate and store installation access token
      refreshInstallationToken(installation, jwt);

      return installationRepository.save(installation);

    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse GitHub installation response", e);
    }
  }

  /** Refresh the installation access token */
  private void refreshInstallationToken(GitHubAppInstallation installation, String jwt) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwt);
    headers.set("Accept", "application/vnd.github+json");
    headers.set("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response = restTemplate.exchange(
        GITHUB_API_URL + "/app/installations/" + installation.getInstallationId() + "/access_tokens",
        HttpMethod.POST, entity, String.class);

    try {
      JsonNode tokenNode = objectMapper.readTree(response.getBody());
      installation.setAccessToken(tokenNode.path("token").asText());

      // Parse GitHub API ISO 8601 UTC timestamp (e.g., "2026-02-01T16:45:00Z")
      String expiresAt = tokenNode.path("expires_at").asText();
      Instant expiresAtInstant = Instant.parse(expiresAt);
      installation.setTokenExpiresAt(LocalDateTime.ofInstant(expiresAtInstant, ZoneOffset.UTC));

    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse GitHub token response", e);
    }
  }

  /** Sync all repositories accessible through an installation */
  public List<GitHubRepository> syncRepositoriesForInstallation(GitHubAppInstallation installation) {
    log.info("Syncing repositories for installation: {}", installation.getAccountLogin());

    // Ensure token is valid
    if (installation.isTokenExpired()) {
      String jwt = generateJwt();
      refreshInstallationToken(installation, jwt);
      installationRepository.save(installation);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(installation.getAccessToken());
    headers.set("Accept", "application/vnd.github+json");
    headers.set("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    List<GitHubRepository> syncedRepos = new ArrayList<>();
    String url = GITHUB_API_URL + "/installation/repositories?per_page=100";

    // Handle pagination with SSRF protection - validate all URLs
    while (url != null) {
      // Sanitize and validate URL to prevent SSRF attacks
      String validatedUrl = sanitizeGitHubApiUrl(url);

      ResponseEntity<String> response = restTemplate.exchange(validatedUrl, HttpMethod.GET, entity, String.class);

      try {
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode reposNode = rootNode.path("repositories");

        for (JsonNode repoNode : reposNode) {
          GitHubRepository repo = syncRepository(repoNode, installation);
          syncedRepos.add(repo);
        }

        // Check for next page (returns null or validated URL)
        url = getNextPageUrl(response.getHeaders());

      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to parse GitHub repositories response", e);
      }
    }

    // Update repository count
    installation.setRepositoryCount(syncedRepos.size());
    installation.setLastSyncAt(LocalDateTime.now());
    installationRepository.save(installation);

    log.info("Synced {} repositories for installation: {}", syncedRepos.size(), installation.getAccountLogin());
    return syncedRepos;
  }

  /** Sync a single repository from GitHub API response */
  private GitHubRepository syncRepository(JsonNode repoNode, GitHubAppInstallation installation) {
    String owner = repoNode.path("owner").path("login").asText();
    String name = repoNode.path("name").asText();

    Optional<GitHubRepository> existingRepo = repositoryRepository.findByOwnerAndName(owner, name);

    GitHubRepository repo;
    if (existingRepo.isPresent()) {
      repo = existingRepo.get();
    } else {
      repo = new GitHubRepository();
      repo.setOwner(owner);
      repo.setName(name);
    }

    repo.setFullName(repoNode.path("full_name").asText());
    repo.setUrl(repoNode.path("html_url").asText());
    repo.setDefaultBranch(repoNode.path("default_branch").asText("main"));
    repo.setIsActive(true);

    return repositoryRepository.save(repo);
  }

  /**
   * Get next page URL from GitHub API pagination headers. Validates the URL to
   * prevent SSRF attacks by ensuring it points to the legitimate GitHub API.
   */
  private String getNextPageUrl(HttpHeaders headers) {
    List<String> linkHeaders = headers.get("Link");
    if (linkHeaders == null || linkHeaders.isEmpty()) {
      return null;
    }

    String linkHeader = linkHeaders.get(0);
    String[] links = linkHeader.split(",");

    for (String link : links) {
      if (link.contains("rel=\"next\"")) {
        int start = link.indexOf('<') + 1;
        int end = link.indexOf('>');
        if (start > 0 && end > start) {
          String urlString = link.substring(start, end);

          // Validate URL to prevent SSRF attacks
          if (isValidGitHubApiUrl(urlString)) {
            return urlString;
          } else {
            log.warn("Pagination URL validation failed, stopping pagination: {}", urlString);
            return null;
          }
        }
      }
    }

    return null;
  }

  /**
   * Validate that a URL points to the legitimate GitHub API to prevent SSRF
   * attacks. Only allows HTTPS URLs pointing to api.github.com with paths
   * starting with expected prefixes.
   */
  private boolean isValidGitHubApiUrl(String urlString) {
    try {
      URI uri = new URI(urlString);

      // Must use HTTPS
      if (!"https".equalsIgnoreCase(uri.getScheme())) {
        log.warn("URL validation failed: scheme is not HTTPS: {}", urlString);
        return false;
      }

      // Disallow embedded credentials (user info) in the URL
      if (uri.getUserInfo() != null) {
        log.warn("URL validation failed: user info is not allowed in GitHub API URLs: {}", urlString);
        return false;
      }

      // Must point to api.github.com (no alternate ports other than default HTTPS)
      String host = uri.getHost();
      if (host == null || !"api.github.com".equalsIgnoreCase(host)) {
        log.warn("URL validation failed: host is not api.github.com: {}", urlString);
        return false;
      }
      int port = uri.getPort();
      if (port != -1 && port != 443) {
        log.warn("URL validation failed: unexpected port for GitHub API URL: {}", urlString);
        return false;
      }

      // Disallow URL fragments
      if (uri.getFragment() != null) {
        log.warn("URL validation failed: fragments are not allowed in GitHub API URLs: {}", urlString);
        return false;
      }

      // Path must start with expected GitHub API prefixes
      String path = uri.getPath();
      if (path == null || (!path.startsWith("/installation/") && !path.startsWith("/app/")
          && !path.startsWith("/repos/"))) {
        log.warn("URL validation failed: path does not start with expected prefix: {}", urlString);
        return false;
      }

      return true;

    } catch (Exception e) {
      log.error("URL validation failed: invalid URI: {}", urlString, e);
      return false;
    }
  }

  /**
   * Sanitize and validate GitHub API URL to prevent SSRF attacks. This method
   * validates the URL and returns it if valid, or throws SecurityException if
   * invalid. The explicit return value helps static analysis tools track the
   * sanitized data flow.
   *
   * @param url
   *            URL to validate
   * @return The same URL if valid
   * @throws SecurityException
   *             if URL is invalid or points to non-GitHub domain
   */
  private String sanitizeGitHubApiUrl(String url) {
    if (!isValidGitHubApiUrl(url)) {
      log.error("SSRF attempt blocked - invalid GitHub API URL: {}", url);
      throw new SecurityException("Invalid GitHub API URL detected: " + url);
    }
    return url;
  }

  /** Generate JWT for authenticating as the GitHub App */
  private String generateJwt() {
    if (privateKey == null || privateKey.isEmpty()) {
      throw new IllegalStateException("GitHub App private key is not configured");
    }

    try {
      PrivateKey privateKeyObj;

      // Check if it's PKCS#1 format (BEGIN RSA PRIVATE KEY) or PKCS#8 format (BEGIN
      // PRIVATE KEY)
      if (privateKey.contains("BEGIN RSA PRIVATE KEY")) {
        // PKCS#1 format - GitHub generates keys in this format
        privateKeyObj = parsePkcs1PrivateKey(privateKey);
      } else {
        // PKCS#8 format
        String pkcs8Key = privateKey.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(pkcs8Key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKeyObj = keyFactory.generatePrivate(keySpec);
      }

      // Generate JWT
      long nowSeconds = System.currentTimeMillis() / 1000;

      return Jwts.builder().setIssuer(appId).setIssuedAt(new Date((nowSeconds - 60) * 1000))
          .setExpiration(new Date((nowSeconds + 600) * 1000)) // 10 minutes max
          .signWith(privateKeyObj, SignatureAlgorithm.RS256).compact();

    } catch (Exception e) {
      throw new RuntimeException("Failed to generate GitHub App JWT", e);
    }
  }

  /**
   * Parse a PKCS#1 formatted RSA private key. GitHub App private keys are in
   * PKCS#1 format (BEGIN RSA PRIVATE KEY).
   */
  private PrivateKey parsePkcs1PrivateKey(String pem) throws Exception {
    String base64Key = pem.replace("-----BEGIN RSA PRIVATE KEY-----", "")
        .replace("-----END RSA PRIVATE KEY-----", "").replaceAll("\\s", "");

    byte[] keyBytes = Base64.getDecoder().decode(base64Key);

    // Parse the ASN.1 DER encoded PKCS#1 RSAPrivateKey structure
    // RSAPrivateKey ::= SEQUENCE {
    // version Version,
    // modulus INTEGER, -- n
    // publicExponent INTEGER, -- e
    // privateExponent INTEGER, -- d
    // prime1 INTEGER, -- p
    // prime2 INTEGER, -- q
    // exponent1 INTEGER, -- d mod (p-1)
    // exponent2 INTEGER, -- d mod (q-1)
    // coefficient INTEGER, -- (inverse of q) mod p
    // }

    int[] pos = {0};

    // Read SEQUENCE
    readTag(keyBytes, pos, 0x30);
    readLength(keyBytes, pos);

    // Read version (should be 0)
    readTag(keyBytes, pos, 0x02);
    int versionLength = readLength(keyBytes, pos);
    pos[0] += versionLength;

    // Read modulus (n)
    BigInteger modulus = readBigInteger(keyBytes, pos);

    // Read public exponent (e)
    BigInteger publicExponent = readBigInteger(keyBytes, pos);

    // Read private exponent (d)
    BigInteger privateExponent = readBigInteger(keyBytes, pos);

    // Read prime1 (p)
    BigInteger primeP = readBigInteger(keyBytes, pos);

    // Read prime2 (q)
    BigInteger primeQ = readBigInteger(keyBytes, pos);

    // Read exponent1 (dP = d mod p-1)
    BigInteger primeExponentP = readBigInteger(keyBytes, pos);

    // Read exponent2 (dQ = d mod q-1)
    BigInteger primeExponentQ = readBigInteger(keyBytes, pos);

    // Read coefficient (qInv = q^-1 mod p)
    BigInteger crtCoefficient = readBigInteger(keyBytes, pos);

    RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent, primeP,
        primeQ, primeExponentP, primeExponentQ, crtCoefficient);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePrivate(keySpec);
  }

  private void readTag(byte[] data, int[] pos, int expectedTag) {
    if (data[pos[0]++] != expectedTag) {
      throw new IllegalArgumentException("Invalid ASN.1 tag");
    }
  }

  private int readLength(byte[] data, int[] pos) {
    int length = data[pos[0]++] & 0xFF;
    if ((length & 0x80) != 0) {
      int numBytes = length & 0x7F;
      length = 0;
      for (int i = 0; i < numBytes; i++) {
        length = (length << 8) | (data[pos[0]++] & 0xFF);
      }
    }
    return length;
  }

  private BigInteger readBigInteger(byte[] data, int[] pos) {
    readTag(data, pos, 0x02); // INTEGER tag
    int length = readLength(data, pos);
    byte[] bytes = new byte[length];
    System.arraycopy(data, pos[0], bytes, 0, length);
    pos[0] += length;
    return new BigInteger(bytes);
  }

  /** Get all active installations */
  @Transactional(readOnly = true)
  public List<GitHubAppInstallationDTO> getAllInstallations() {
    return installationRepository.findByIsActiveTrue().stream().map(this::mapToDTO).toList();
  }

  /** Get installation by ID */
  @Transactional(readOnly = true)
  public Optional<GitHubAppInstallationDTO> getInstallation(Long id) {
    return installationRepository.findById(id).map(this::mapToDTO);
  }

  /** Remove an installation */
  public void removeInstallation(Long id) {
    installationRepository.findById(id).ifPresent(installation -> {
      installation.setIsActive(false);
      installationRepository.save(installation);
      log.info("Deactivated GitHub App installation: {}", installation.getAccountLogin());
    });
  }

  /** Sync all repositories from all active installations */
  public GitHubBulkSyncResultDTO syncAllRepositories() {
    List<GitHubAppInstallation> installations = installationRepository.findByIsActiveTrue();

    int totalDiscovered = 0;
    int totalAdded = 0;
    int totalUpdated = 0;
    int totalFailed = 0;
    List<String> errors = new ArrayList<>();

    for (GitHubAppInstallation installation : installations) {
      try {
        int beforeCount = repositoryRepository.findAll().size();
        List<GitHubRepository> repos = syncRepositoriesForInstallation(installation);
        int afterCount = repositoryRepository.findAll().size();

        totalDiscovered += repos.size();
        totalAdded += (afterCount - beforeCount);
        totalUpdated += (repos.size() - (afterCount - beforeCount));

      } catch (Exception e) {
        log.error("Failed to sync repositories for installation: {}", installation.getAccountLogin(), e);
        errors.add("Failed to sync " + installation.getAccountLogin() + ": " + e.getMessage());
        totalFailed++;
      }
    }

    return GitHubBulkSyncResultDTO.builder().installationsProcessed(installations.size())
        .repositoriesDiscovered(totalDiscovered).repositoriesAdded(totalAdded).repositoriesUpdated(totalUpdated)
        .repositoriesFailed(totalFailed).errors(errors).success(errors.isEmpty()).message(String
            .format("Synced %d repositories from %d installations", totalDiscovered, installations.size()))
        .build();
  }

  /**
   * Discover and sync all existing GitHub App installations. This is useful when
   * the app was installed directly on GitHub (not through ShipFlow OAuth flow),
   * allowing ShipFlow to detect and register existing installations.
   *
   * @return Result containing discovered installations and synced repositories
   */
  public GitHubBulkSyncResultDTO discoverInstallations() {
    if (!isAppConfigured()) {
      return GitHubBulkSyncResultDTO.builder().success(false).message("GitHub App is not configured")
          .errors(List.of("Missing GitHub App configuration (app ID, private key, or app name)")).build();
    }

    log.info("Discovering existing GitHub App installations...");

    List<String> errors = new ArrayList<>();
    List<String> syncedInstallations = new ArrayList<>();
    int totalReposSynced = 0;

    try {
      // Generate JWT for authenticating as the GitHub App
      String jwt = generateJwt();

      // Fetch all installations from GitHub API
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(jwt);
      headers.set("Accept", "application/vnd.github+json");
      headers.set("X-GitHub-Api-Version", "2022-11-28");

      HttpEntity<Void> entity = new HttpEntity<>(headers);

      String url = GITHUB_API_URL + "/app/installations?per_page=100";

      while (url != null) {
        String validatedUrl = sanitizeGitHubApiUrl(url);

        ResponseEntity<String> response = restTemplate.exchange(validatedUrl, HttpMethod.GET, entity,
            String.class);

        JsonNode installationsNode = objectMapper.readTree(response.getBody());

        for (JsonNode installationNode : installationsNode) {
          try {
            Long installationId = installationNode.path("id").asLong();
            String accountLogin = installationNode.path("account").path("login").asText();

            log.info("Discovered installation: {} (ID: {})", accountLogin, installationId);

            // Fetch full installation details and save
            GitHubAppInstallation installation = fetchAndSaveInstallation(installationId, jwt);

            // Sync repositories for this installation
            List<GitHubRepository> repos = syncRepositoriesForInstallation(installation);
            totalReposSynced += repos.size();
            syncedInstallations.add(accountLogin + " (" + repos.size() + " repos)");

          } catch (Exception e) {
            String accountLogin = installationNode.path("account").path("login").asText("unknown");
            log.error("Failed to process installation for {}", accountLogin, e);
            errors.add("Failed to process " + accountLogin + ": " + e.getMessage());
          }
        }

        // Check for next page
        url = getNextPageUrl(response.getHeaders());
      }

      String message = String.format("Discovered %d installations with %d total repositories",
          syncedInstallations.size(), totalReposSynced);

      log.info(message);

      return GitHubBulkSyncResultDTO.builder().success(errors.isEmpty())
          .installationsProcessed(syncedInstallations.size()).repositoriesDiscovered(totalReposSynced)
          .repositoriesAdded(totalReposSynced).syncedInstallations(syncedInstallations).errors(errors)
          .message(message).build();

    } catch (Exception e) {
      log.error("Failed to discover GitHub App installations", e);
      return GitHubBulkSyncResultDTO.builder().success(false)
          .message("Failed to discover installations: " + e.getMessage()).errors(List.of(e.getMessage()))
          .build();
    }
  }

  /** Check if GitHub App is properly configured */
  public boolean isAppConfigured() {
    return appId != null && !appId.isEmpty() && privateKey != null && !privateKey.isEmpty() && appName != null
        && !appName.isEmpty();
  }

  /** Get GitHub App configuration status */
  public Map<String, Object> getConfigurationStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("configured", isAppConfigured());
    status.put("appId", appId != null && !appId.isEmpty());
    status.put("appName", appName);
    status.put("privateKeyConfigured", privateKey != null && !privateKey.isEmpty());
    status.put("clientIdConfigured", clientId != null && !clientId.isEmpty());
    status.put("webhookSecretConfigured", webhookSecret != null && !webhookSecret.isEmpty());
    status.put("installationUrl", GITHUB_APP_URL + "/" + appName + "/installations/new");

    Long totalRepos = installationRepository.countTotalRepositories();
    status.put("totalInstallations", installationRepository.findByIsActiveTrue().size());
    status.put("totalRepositories", totalRepos);

    return status;
  }

  /** Map entity to DTO */
  private GitHubAppInstallationDTO mapToDTO(GitHubAppInstallation installation) {
    GitHubPermissionsDTO permissions = null;
    if (installation.getPermissions() != null) {
      try {
        JsonNode permsNode = objectMapper.readTree(installation.getPermissions());
        permissions = GitHubPermissionsDTO.builder().contents(permsNode.path("contents").asText(null))
            .issues(permsNode.path("issues").asText(null))
            .pullRequests(permsNode.path("pull_requests").asText(null))
            .metadata(permsNode.path("metadata").asText(null))
            .webhooks(permsNode.path("webhooks").asText(null)).checks(permsNode.path("checks").asText(null))
            .statuses(permsNode.path("statuses").asText(null)).build();
      } catch (JsonProcessingException e) {
        log.warn("Failed to parse permissions JSON", e);
      }
    }

    return GitHubAppInstallationDTO.builder().id(installation.getId())
        .installationId(installation.getInstallationId()).accountType(installation.getAccountType())
        .accountLogin(installation.getAccountLogin()).repositorySelection(installation.getRepositorySelection())
        .isActive(installation.getIsActive()).webhooksConfigured(installation.getWebhooksConfigured())
        .lastSyncAt(installation.getLastSyncAt()).repositoryCount(installation.getRepositoryCount())
        .createdAt(installation.getCreatedAt()).installedByLogin(installation.getInstalledByLogin())
        .permissions(permissions).build();
  }

  /** Internal class for storing OAuth state */
  private record OAuthState(String state, String redirectUrl, LocalDateTime expiresAt) {
  }
}
