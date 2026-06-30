package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event;

/**
 * Published when a new {@link com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource} is
 * created or a refresh is requested. The orchestrator (Task 7) listens to schedule ingestion.
 */
public record KnowledgeSourceCreatedEvent(Long sourceId) {}
