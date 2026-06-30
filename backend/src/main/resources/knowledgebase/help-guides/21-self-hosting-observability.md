# Self-Hosting & Observability

ShipFlow ships production-grade self-hosting: a Helm chart for Kubernetes and a built-in observability stack (Prometheus metrics, Grafana dashboards, and optional OpenTelemetry tracing).

# Deploying with Helm

A first-party Helm chart lives in `charts/shipflow`. It deploys the ShipFlow application and expects PostgreSQL, Redis, and (optionally) Qdrant to be provided as external services — point the chart at them in `values.yaml` under `externalServices`.

## Install

```bash
helm install shipflow ./charts/shipflow \
  --namespace shipflow --create-namespace \
  --set externalServices.postgres.host=my-postgres \
  --set externalServices.redis.host=my-redis \
  --set secrets.JWT_SECRET=$(openssl rand -base64 48)
```

## What the chart configures

- **Replicas & autoscaling** — `replicaCount` (default 2), or enable a HorizontalPodAutoscaler with `autoscaling.enabled=true`.
- **Ingress** — off by default; enable with `ingress.enabled=true` and set `ingress.hosts`.
- **Secrets** — DB/Redis/JWT/Qdrant credentials render into a Secret, or set `existingSecret` to use your own (recommended for production / external secret operators).
- **Persistent uploads** — a PVC mounts at `/app/uploads`. For more than one replica, either use a `ReadWriteMany` storage class **or** switch attachment storage to S3/MinIO in **Organization Settings → Storage** (recommended).
- **Health probes** — liveness/readiness use Spring Boot health groups at `/actuator/health/liveness` and `/actuator/health/readiness`.

# Metrics (Prometheus)

The app exposes a Prometheus scrape endpoint at **`/actuator/prometheus`**. Every metric carries the common tag `application="shipflow"` so you can distinguish instances. Only `health`, `info`, and `prometheus` actuator endpoints are exposed by default (override with the `MANAGEMENT_ENDPOINTS` env var).

- In Kubernetes, pod annotations advertise the scrape target for a vanilla Prometheus. If you run the Prometheus Operator, enable a ServiceMonitor with `observability.serviceMonitor.enabled=true`.
- Locally, run the bundled stack:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Prometheus runs at http://localhost:9090 and Grafana at http://localhost:3001 (admin / admin by default).

# Dashboards (Grafana)

Grafana is pre-provisioned with the Prometheus datasource and a **ShipFlow — Overview** dashboard (`monitoring/grafana/dashboards/shipflow-overview.json`) showing app up/down, JVM heap, HTTP request rate, p95 latency, and AI Q&A query/cache-hit rates. Import the JSON into any existing Grafana if you don't use the bundled one.

# Distributed Tracing (OpenTelemetry)

Tracing is **off by default**. Enable it to ship spans to any OTLP collector (Jaeger, Tempo, Grafana Cloud):

| Env var | Default | Purpose |
|---------|---------|---------|
| `TRACING_ENABLED` | `false` | Turn tracing on |
| `OTLP_ENDPOINT` | `http://localhost:4318/v1/traces` | OTLP HTTP traces endpoint |
| `TRACING_SAMPLE_RATE` | `0.1` | Fraction of requests sampled |

When the tracing bridge is active, `traceId`/`spanId` are also added to console logs so logs and traces correlate.

# Structured JSON Logging

Set `LOG_FORMAT=logstash` (or `ecs` / `gelf`) to emit JSON logs for aggregation (ELK, Loki, Datadog). Leave it unset for human-readable console output. JSON logs include the `traceId`/`spanId` when tracing is enabled.
