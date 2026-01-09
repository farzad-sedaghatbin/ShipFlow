package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filters retrieved documents based on user permissions and security rules.
 */
@Component
@Slf4j
public class SecurityDocumentFilter {
    
    /**
     * Filter documents based on user access permissions.
     */
    public List<EmbeddingMatch<TextSegment>> filterByUserAccess(
            List<EmbeddingMatch<TextSegment>> matches,
            Long userId,
            Set<Long> userTeamIds,
            Set<Long> userProjectIds) {
        
        if (matches == null || matches.isEmpty()) {
            return matches;
        }
        
        List<EmbeddingMatch<TextSegment>> filtered = new ArrayList<>();
        int blockedCount = 0;
        
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (canUserAccess(match, userId, userTeamIds, userProjectIds)) {
                filtered.add(match);
            } else {
                blockedCount++;
            }
        }
        
        if (blockedCount > 0) {
            log.debug("Filtered out {} documents due to access control for user {}", 
                    blockedCount, userId);
        }
        
        return filtered;
    }
    
    /**
     * Check if user can access a document.
     */
    private boolean canUserAccess(
            EmbeddingMatch<TextSegment> match,
            Long userId,
            Set<Long> userTeamIds,
            Set<Long> userProjectIds) {
        
        if (match.embedded() == null || match.embedded().metadata() == null) {
            // No metadata = public or allow by default
            return true;
        }
        
        var metadata = match.embedded().metadata();
        
        // Check team access
        String teamIdStr = metadata.getString("teamId");
        if (teamIdStr != null) {
            try {
                Long teamId = Long.parseLong(teamIdStr);
                if (userTeamIds != null && !userTeamIds.contains(teamId)) {
                    log.trace("User {} blocked from team {} document", userId, teamId);
                    return false;
                }
            } catch (NumberFormatException e) {
                // Invalid team ID, allow by default
            }
        }
        
        // Check project access
        String projectIdStr = metadata.getString("projectId");
        if (projectIdStr != null) {
            try {
                Long projectId = Long.parseLong(projectIdStr);
                if (userProjectIds != null && !userProjectIds.contains(projectId)) {
                    log.trace("User {} blocked from project {} document", userId, projectId);
                    return false;
                }
            } catch (NumberFormatException e) {
                // Invalid project ID, allow by default
            }
        }
        
        // Check document visibility
        String visibility = metadata.getString("visibility");
        if ("private".equalsIgnoreCase(visibility)) {
            // Check if user is the owner
            String ownerIdStr = metadata.getString("ownerId");
            if (ownerIdStr != null) {
                try {
                    Long ownerId = Long.parseLong(ownerIdStr);
                    if (!userId.equals(ownerId)) {
                        log.trace("User {} blocked from private document owned by {}", 
                                userId, ownerId);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    // Invalid owner ID, block private docs by default
                    return false;
                }
            }
        }
        
        // Allow if no restrictions found
        return true;
    }
    
    /**
     * Build security filter for embedding search.
     * Returns metadata filter string for vector store.
     */
    public String buildSecurityFilter(Set<Long> userTeamIds, Set<Long> userProjectIds) {
        List<String> conditions = new ArrayList<>();
        
        if (userTeamIds != null && !userTeamIds.isEmpty()) {
            String teamIds = userTeamIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            conditions.add("teamId IN (" + teamIds + ")");
        }
        
        if (userProjectIds != null && !userProjectIds.isEmpty()) {
            String projectIds = userProjectIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            conditions.add("projectId IN (" + projectIds + ")");
        }
        
        // Always exclude private docs unless owned by user (handled in post-filter)
        conditions.add("visibility != 'private'");
        
        if (conditions.isEmpty()) {
            return null;
        }
        
        return String.join(" AND ", conditions);
    }
}
