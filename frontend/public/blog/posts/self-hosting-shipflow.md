---
title: "Self-Hosting ShipFlow: Full Control Over Your Project Data"
slug: self-hosting-shipflow
date: 2026-05-08
description: "ShipFlow is open source and designed for self-hosting. Learn how to deploy it with Docker and keep complete ownership of your project management data."
keywords: ["self-hosted", "open source project management", "docker deploy", "data ownership", "shipflow setup"]
author: farzad
---

# Self-Hosting ShipFlow: Full Control Over Your Project Data

Every SaaS tool comes with a trade-off: convenience vs control. Your project data — pitches, decisions, retrospectives, risk analyses — lives on someone else's servers, governed by someone else's terms of service.

ShipFlow is MIT-licensed and built for self-hosting from day one. Your data stays on your infrastructure.

## Why self-hosting matters

**Data sovereignty** — Regulated industries (healthcare, finance, government) often cannot use cloud-hosted project management tools. Self-hosting removes that blocker.

**No vendor lock-in** — If ShipFlow stops being maintained, you still have the code and your data. Fork it, extend it, or migrate at your own pace.

**Customization** — Self-hosting means you can modify the application. Add custom fields, integrate with internal tools, or change the UI to match your workflow.

## Getting started with Docker

ShipFlow ships with a Docker Compose setup that runs the full stack: Spring Boot backend, React frontend, and PostgreSQL database.

Pull the repository, configure your environment variables, and run a single command. The application handles database migrations automatically on startup.

## Connecting your AI provider

ShipFlow's AI features (risk analysis, knowledge base, Q&A) work with multiple providers through LangChain4j. Connect OpenAI, Anthropic Claude, or a locally-running model like Ollama.

For teams that cannot send data to external AI providers, running a local model keeps everything within your network boundary.

## Production considerations

For production deployments, we recommend running PostgreSQL as a managed service for reliability, placing the application behind a reverse proxy with TLS, and setting up regular database backups.

ShipFlow is stateless — you can run multiple instances behind a load balancer for high availability.

[Get started at github.com/farzad-sedaghatbin/ShipFlow](https://github.com/farzad-sedaghatbin/ShipFlow)
