package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event;

/**
 * Published when a {@link com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource}'s metadata
 * or status changes (e.g. INGESTING, READY, FAILED, STALE). Consumed by the SSE event bridge
 * (Task 13) to push updates to the frontend.
 */
public record KnowledgeSourceUpdatedEvent(Long sourceId) {}
