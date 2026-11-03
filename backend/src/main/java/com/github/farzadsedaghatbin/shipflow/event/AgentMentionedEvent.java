package com.github.farzadsedaghatbin.shipflow.event;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;

/**
 * Published after a comment that @mentions the ShipFlow Agent is committed, so the (slow) LLM reply
 * is produced in the background instead of blocking the comment POST.
 *
 * @param commentId the committed comment that mentioned the agent
 * @param entityType the kind of entity the thread belongs to
 * @param entityId the entity the thread belongs to
 * @param requesterUserId the human who wrote the comment — the agent answers with THIS user's access
 */
public record AgentMentionedEvent(
    Long commentId, CommentEntityType entityType, Long entityId, Long requesterUserId) {}
