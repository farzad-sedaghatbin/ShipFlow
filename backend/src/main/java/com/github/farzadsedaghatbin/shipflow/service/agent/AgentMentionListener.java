package com.github.farzadsedaghatbin.shipflow.service.agent;

import com.github.farzadsedaghatbin.shipflow.event.AgentMentionedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Hands an {@link AgentMentionedEvent} to {@link AgentReplyService} once the mentioning comment is
 * committed. AFTER_COMMIT so the comment row is visible; {@code @Async} so the LLM call never
 * blocks the comment POST; {@code REQUIRES_NEW} because there is no transaction left to join.
 * Same shape as {@code WikiKnowledgeListener}.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AgentMentionListener {

  private final AgentReplyService agentReplyService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onAgentMentioned(AgentMentionedEvent event) {
    try {
      agentReplyService.reply(event);
    } catch (Exception e) {
      // Never let an agent failure surface anywhere; the comment itself already succeeded.
      log.error("ShipFlow Agent reply failed for comment {}: {}", event.commentId(), e.getMessage(), e);
    }
  }
}
