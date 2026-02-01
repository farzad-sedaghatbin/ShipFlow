package com.github.farzadsedaghatbin.shipflow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for initiating GitHub App OAuth authorization flow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubOAuthInitRequest {
    
    /**
     * Optional: Suggest installation for a specific organization
     */
    private String suggestedOrganization;
    
    /**
     * Optional: Frontend URL to redirect after OAuth completion
     */
    private String redirectUrl;
    
    /**
     * Optional: State parameter for CSRF protection (auto-generated if not provided)
     */
    private String state;
    
    /**
     * Optional: Base URL of the ShipFlow instance (auto-detected from request if not set in config)
     * This is used for OAuth callback URLs. Example: https://shipflow.mycompany.com
     * If APP_BASE_URL is configured in environment, this is ignored.
     */
    private String baseUrl;
}
