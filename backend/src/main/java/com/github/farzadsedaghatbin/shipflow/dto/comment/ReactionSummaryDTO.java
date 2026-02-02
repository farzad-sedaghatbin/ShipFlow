package com.github.farzadsedaghatbin.shipflow.dto.comment;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentReaction;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing aggregated reaction counts and user's own reactions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryDTO {

    /** Map of reaction type to count */
    private Map<CommentReaction, Integer> reactionCounts;

    /** Map of reaction type to whether current user has reacted */
    private Map<CommentReaction, Boolean> userReactions;

    /** Total number of reactions */
    private Integer totalReactions;
}
