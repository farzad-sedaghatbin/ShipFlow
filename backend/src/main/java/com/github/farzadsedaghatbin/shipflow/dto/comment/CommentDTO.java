package com.github.farzadsedaghatbin.shipflow.dto.comment;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a comment with all its details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long id;
    private String content;
    private CommentEntityType entityType;
    private Long entityId;
    
    // Author information
    private Long authorId;
    private String authorName;
    private String authorUsername;
    
    // Metadata
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Reactions summary
    private ReactionSummaryDTO reactions;
    
    // Permission flags for current user
    private Boolean canEdit;
    private Boolean canDelete;
}
