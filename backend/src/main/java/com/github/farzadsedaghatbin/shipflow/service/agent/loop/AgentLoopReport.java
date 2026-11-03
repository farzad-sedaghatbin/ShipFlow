package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

/**
 * What an agent loop found, before anything is sent.
 *
 * @param title one-line heading, e.g. "Standup digest — Payments"
 * @param facts the report body. Built from the database by code, never by the model, so every
 *     number and name in it is real; the model may only add a short summary on top
 * @param hasFindings false when there is nothing worth sending (a quiet day, a healthy cycle)
 * @param entityType what the message is about, for the Slack/Teams history (e.g. PROJECT, CYCLE)
 * @param entityId the id of that entity
 */
public record AgentLoopReport(String title, String facts, boolean hasFindings, String entityType, Long entityId) {}
