package com.github.farzadsedaghatbin.shipflow.service.mcp;

import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Figma MCP client implementation.
 * Connects to a Figma MCP server to read design files and extract UI context.
 * 
 * MCP Server expected endpoints:
 * - POST /tools/list_pages: List pages/frames in a Figma file
 * - POST /tools/get_node: Get a specific node's details
 * - POST /tools/search_nodes: Search for nodes by name/type
 * - POST /tools/get_design_context: Get comprehensive design metadata
 */
@Service
@Slf4j
public class FigmaMcpProvider implements McpClientService {

    private final McpConfig mcpConfig;
    private final RestTemplate mcpRestTemplate;
    private final OrganizationSettingsService settingsService;

    // Figma URL patterns
    private static final Pattern FIGMA_FILE_PATTERN = Pattern.compile(
        "https?://(?:www\\.)?figma\\.com/(?:file|design)/([a-zA-Z0-9]+)(?:/[^?]*)?(?:\\?.*)?" );
    private static final Pattern FIGMA_PROTOTYPE_PATTERN = Pattern.compile(
        "https?://(?:www\\.)?figma\\.com/proto/([a-zA-Z0-9]+)(?:/[^?]*)?(?:\\?.*)?" );

    public FigmaMcpProvider(
            McpConfig mcpConfig,
            @Qualifier("mcpRestTemplate") RestTemplate mcpRestTemplate,
            OrganizationSettingsService settingsService) {
        this.mcpConfig = mcpConfig;
        this.mcpRestTemplate = mcpRestTemplate;
        this.settingsService = settingsService;
    }

    @Override
    public boolean isAvailable() {
        McpConfig.FigmaMcpConfig config = mcpConfig.getFigma();
        boolean available = config.isEnabled() && 
            config.getServerUrl() != null && 
            !config.getServerUrl().isBlank();
        
        if (!available) {
            log.debug("Figma MCP is not available - enabled: {}, serverUrl: {}", 
                config.isEnabled(), config.getServerUrl());
        }
        return available;
    }

    @Override
    public String getProviderType() {
        return "figma";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> listFiles(Map<String, String> context) {
        if (!isAvailable()) {
            log.warn("Figma MCP not available, cannot list files");
            return List.of();
        }

        String fileKey = context.get("fileKey");
        String accessToken = context.get("accessToken");
        log.info("Listing pages/frames in Figma file {} via MCP", fileKey);

        try {
            String url = mcpConfig.getFigma().getServerUrl() + "/tools/list_pages";
            
            Map<String, String> request = new HashMap<>();
            request.put("file_key", fileKey);
            if (accessToken != null) {
                request.put("access_token", accessToken);
            }

            HttpEntity<Map<String, String>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("pages")) {
                Object pages = response.getBody().get("pages");
                if (pages instanceof List) {
                    log.debug("Retrieved {} pages from Figma MCP", ((List<?>) pages).size());
                    return (List<String>) pages;
                }
            }

            log.debug("No pages returned from Figma MCP for file: {}", fileKey);
            return List.of();

        } catch (RestClientException e) {
            log.error("Failed to list pages via Figma MCP: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<String> readFile(Map<String, String> context, String nodePath) {
        if (!isAvailable()) {
            log.warn("Figma MCP not available, cannot read node: {}", nodePath);
            return Optional.empty();
        }

        String fileKey = context.get("fileKey");
        String accessToken = context.get("accessToken");
        log.info("Reading Figma node {} in file {} via MCP", nodePath, fileKey);

        try {
            String url = mcpConfig.getFigma().getServerUrl() + "/tools/get_node";
            
            Map<String, String> request = new HashMap<>();
            request.put("file_key", fileKey);
            request.put("node_id", nodePath);
            if (accessToken != null) {
                request.put("access_token", accessToken);
            }

            HttpEntity<Map<String, String>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("node")) {
                String nodeJson = String.valueOf(response.getBody().get("node"));
                log.debug("Successfully read Figma node {}", nodePath);
                return Optional.of(nodeJson);
            }

            log.debug("No node data returned from Figma MCP for: {}", nodePath);
            return Optional.empty();

        } catch (RestClientException e) {
            log.error("Failed to read node {} via Figma MCP: {}", nodePath, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> searchFiles(Map<String, String> context, String pattern) {
        if (!isAvailable()) {
            log.warn("Figma MCP not available, cannot search with pattern: {}", pattern);
            return List.of();
        }

        String fileKey = context.get("fileKey");
        String accessToken = context.get("accessToken");
        log.info("Searching nodes in Figma file {} with pattern '{}' via MCP", fileKey, pattern);

        try {
            String url = mcpConfig.getFigma().getServerUrl() + "/tools/search_nodes";
            
            Map<String, String> request = new HashMap<>();
            request.put("file_key", fileKey);
            request.put("pattern", pattern);
            if (accessToken != null) {
                request.put("access_token", accessToken);
            }

            HttpEntity<Map<String, String>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null && response.getBody().containsKey("matches")) {
                Object matches = response.getBody().get("matches");
                if (matches instanceof List) {
                    log.debug("Found {} nodes matching pattern '{}' via Figma MCP", 
                        ((List<?>) matches).size(), pattern);
                    return (List<String>) matches;
                }
            }

            log.debug("No matches returned from Figma MCP for pattern: {}", pattern);
            return List.of();

        } catch (RestClientException e) {
            log.error("Failed to search nodes via Figma MCP: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public Map<String, Object> getResourceContext(Map<String, String> context) {
        if (!isAvailable()) {
            log.warn("Figma MCP not available, cannot get design context");
            return Map.of();
        }

        String fileKey = context.get("fileKey");
        String accessToken = context.get("accessToken");

        log.info("Getting design context for Figma file {} via MCP", fileKey);

        try {
            String url = mcpConfig.getFigma().getServerUrl() + "/tools/get_design_context";
            
            Map<String, String> request = new HashMap<>();
            request.put("file_key", fileKey);
            if (accessToken != null) {
                request.put("access_token", accessToken);
            }

            HttpEntity<Map<String, String>> entity = createJsonEntity(request);
            ResponseEntity<Map<String, Object>> response = mcpRestTemplate.exchange(
                url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response.getBody() != null) {
                log.debug("Retrieved design context for Figma file {}", fileKey);
                return response.getBody();
            }

            log.debug("No context returned from Figma MCP for file: {}", fileKey);
            return Map.of();

        } catch (RestClientException e) {
            log.error("Failed to get design context via Figma MCP: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Create an HTTP entity with JSON content type and Authorization headers.
     * Supports both header-based auth (preferred) and body-based auth (legacy).
     */
    private <T> HttpEntity<T> createJsonEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json, text/event-stream");
        
        // Add Figma token for authentication
        String figmaToken = settingsService.getFigmaAccessToken();
        if (figmaToken != null && !figmaToken.isBlank()) {
            headers.set("Authorization", "Bearer " + figmaToken);
        } else {
            log.warn("Figma access token not configured, MCP requests may fail");
        }
        
        return new HttpEntity<>(body, headers);
    }

    /**
     * Extract Figma file keys from wireframe links text.
     * Handles multiple URLs separated by newlines, commas, or spaces.
     *
     * @param wireframeLinks the wireframe links text from a pitch
     * @return list of extracted Figma file keys
     */
    public List<String> extractFigmaFileKeys(String wireframeLinks) {
        if (wireframeLinks == null || wireframeLinks.isBlank()) {
            return List.of();
        }

        Set<String> fileKeys = new LinkedHashSet<>();

        // Try file pattern
        Matcher fileMatcher = FIGMA_FILE_PATTERN.matcher(wireframeLinks);
        while (fileMatcher.find()) {
            fileKeys.add(fileMatcher.group(1));
        }

        // Try prototype pattern
        Matcher protoMatcher = FIGMA_PROTOTYPE_PATTERN.matcher(wireframeLinks);
        while (protoMatcher.find()) {
            fileKeys.add(protoMatcher.group(1));
        }

        log.debug("Extracted {} Figma file keys from wireframe links", fileKeys.size());
        return new ArrayList<>(fileKeys);
    }

    /**
     * Extract Figma URLs from wireframe links text.
     *
     * @param wireframeLinks the wireframe links text from a pitch
     * @return list of Figma URLs found
     */
    public List<String> extractFigmaUrls(String wireframeLinks) {
        if (wireframeLinks == null || wireframeLinks.isBlank()) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();

        // Match all Figma URLs
        Pattern urlPattern = Pattern.compile(
            "https?://(?:www\\.)?figma\\.com/(?:file|design|proto)/[a-zA-Z0-9]+(?:/[^\\s,]*)?");
        Matcher matcher = urlPattern.matcher(wireframeLinks);
        
        while (matcher.find()) {
            urls.add(matcher.group());
        }

        log.debug("Extracted {} Figma URLs from wireframe links", urls.size());
        return urls;
    }

    /**
     * Get design context for a Figma file.
     * Returns a compact, token-efficient summary of the design.
     *
     * @param fileKey Figma file key
     * @param accessToken organization's Figma access token
     * @return design context summary, or null if unavailable
     */
    public FigmaDesignContext getDesignContext(String fileKey, String accessToken) {
        if (!isAvailable()) {
            log.debug("Figma MCP not available for file: {}", fileKey);
            return null;
        }

        if (accessToken == null || accessToken.isBlank()) {
            log.debug("No Figma access token provided for file: {}", fileKey);
            return null;
        }

        Map<String, String> context = Map.of(
            "fileKey", fileKey,
            "accessToken", accessToken
        );

        Map<String, Object> resourceContext = getResourceContext(context);
        
        if (resourceContext.isEmpty()) {
            return null;
        }

        // Parse the response into a compact context object
        return FigmaDesignContext.builder()
            .fileKey(fileKey)
            .fileName((String) resourceContext.getOrDefault("name", "Unknown"))
            .components(extractComponentNames(resourceContext))
            .colors(extractColors(resourceContext))
            .typography(extractTypography(resourceContext))
            .layoutPatterns(extractLayoutPatterns(resourceContext))
            .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractComponentNames(Map<String, Object> context) {
        Object components = context.get("components");
        if (components instanceof List) {
            return ((List<Map<String, Object>>) components).stream()
                .map(c -> (String) c.get("name"))
                .filter(Objects::nonNull)
                .limit(20) // Token efficiency: limit to top 20 components
                .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractColors(Map<String, Object> context) {
        Object colors = context.get("colors");
        if (colors instanceof List) {
            return ((List<String>) colors).stream()
                .limit(10) // Token efficiency: limit to top 10 colors
                .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTypography(Map<String, Object> context) {
        Object typography = context.get("typography");
        if (typography instanceof List) {
            return ((List<String>) typography).stream()
                .limit(5) // Token efficiency: limit to top 5 font styles
                .toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractLayoutPatterns(Map<String, Object> context) {
        Object layouts = context.get("layouts");
        if (layouts instanceof List) {
            return ((List<String>) layouts).stream()
                .limit(5) // Token efficiency: limit to top 5 layout patterns
                .toList();
        }
        return List.of();
    }

    /**
     * Compact design context for token-efficient LLM prompts.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FigmaDesignContext {
        private String fileKey;
        private String fileName;
        private List<String> components;
        private List<String> colors;
        private List<String> typography;
        private List<String> layoutPatterns;

        /**
         * Convert to a compact string for LLM prompts.
         * Optimized for token efficiency (~100-200 tokens).
         */
        public String toPromptString() {
            StringBuilder sb = new StringBuilder();
            
            if (fileName != null && !fileName.equals("Unknown")) {
                sb.append("Design: ").append(fileName).append("\n");
            }
            
            if (components != null && !components.isEmpty()) {
                sb.append("Components: ").append(String.join(", ", components)).append("\n");
            }
            
            if (colors != null && !colors.isEmpty()) {
                sb.append("Colors: ").append(String.join(", ", colors)).append("\n");
            }
            
            if (typography != null && !typography.isEmpty()) {
                sb.append("Typography: ").append(String.join(", ", typography)).append("\n");
            }
            
            if (layoutPatterns != null && !layoutPatterns.isEmpty()) {
                sb.append("Layouts: ").append(String.join(", ", layoutPatterns)).append("\n");
            }
            
            return sb.toString().trim();
        }

        /**
         * Check if any meaningful design context is available.
         */
        public boolean hasContent() {
            return (components != null && !components.isEmpty()) ||
                   (colors != null && !colors.isEmpty()) ||
                   (typography != null && !typography.isEmpty()) ||
                   (layoutPatterns != null && !layoutPatterns.isEmpty());
        }
    }
}
