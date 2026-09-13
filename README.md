# MediaForge

A self-hostable, distributed media-processing pipeline. Upload images, video, and audio; process them asynchronously through a horizontally-scalable worker fleet; watch job status update live; and operate the whole system through a production-grade DevOps toolchain.

Built as a portfolio project to learn **DevOps by building and operating a real distributed system** — every operational concept (orchestration, autoscaling, observability, infrastructure-as-code) is introduced because the architecture genuinely demands it, not bolted on.

**Stack:** Angular · Spring Boot / Java 21 · PostgreSQL · RabbitMQ · Redis · MinIO
**DevOps:** Docker · Docker Compose · GitHub Actions · Kubernetes (k3s) · KEDA · Kustomize · Terraform · Prometheus + Grafana

---

## What it does

A user uploads a media file through the Angular web app. The Spring Boot API stores the original in object storage and enqueues one or more processing jobs. A fleet of worker services consumes those jobs asynchronously — generating image thumbnails, transcoding video, extracting audio, probing metadata, rendering waveforms — and writes the results back to storage. The UI reflects each job's status **in real time** over WebSockets, and finished assets can be previewed in-app or downloaded.

On the surface, an ordinary web app. Underneath, a genuinely distributed system: an API tier, a message queue, a horizontally-scalable worker tier, object storage, a relational database, and a cache — taken through the complete modern operational lifecycle.

### Processing by media type

| Media | Jobs | Tooling |
|---|---|---|
| **Image** | Thumbnails | Thumbnailator (pure Java) |
| **Video** | Poster frame, H.264 transcode, preview clip | FFmpeg |
| **Audio** | Metadata probe, waveform image, MP3 transcode | FFmpeg / ffprobe |

---

## Architecture

```
 Angular SPA ──HTTP + JWT──▶  API  ──publish {jobId}──▶  RabbitMQ  ──▶  Worker fleet
      ▲  ▲                     │                                          │  (7 processors
      │  └── WebSocket ◀─STOMP /topic/uploads/{id}◀── API subscriber ◀────┤   + FFmpeg)
      │                        ├── PostgreSQL (users, uploads, jobs, assets)◀┤
      │                        └── MinIO (originals + derived outputs) ◀──────┘
                                    Redis pub/sub carries live job + upload events
        Prometheus scrapes api + workers + RabbitMQ · Grafana dashboards
```

The request path (accepting an upload) is deliberately decoupled from the work path (processing it) via the message queue — which is what lets the worker tier scale independently of the API.

**Key design points**
- **Idempotent, retry-safe workers** — jobs are safe to redeliver; outputs use deterministic storage keys, and completion is recorded atomically.
- **Layered retry** — transient failures retry with backoff (immediate → delayed via per-type wait queues); permanent failures dead-letter.
- **Per-type worker split** — image / video / audio workers are separate deployments, each autoscaling independently on its own queue depth.
- **Real-time end to end** — worker → Redis pub/sub → API → STOMP/WebSocket → browser; jobs, upload status, and new assets all appear live without refresh.

---

## Tech stack

**Backend** — Java 21, Spring Boot 4, Spring Web / Security (JWT) / Data JPA / AMQP / WebSocket, Flyway, Actuator + Micrometer. Maven multi-module monorepo (`common` / `api` / `worker`).

**Frontend** — Angular (standalone components, signals), plain CSS design-token system, STOMP-over-WebSocket for live updates, light/dark theming.

**Infrastructure** — PostgreSQL 18, RabbitMQ 4, Redis 7, MinIO (S3-compatible), FFmpeg.

**DevOps** — Docker (multi-stage builds), Docker Compose, GitHub Actions (build, Trivy security scan, GHCR), Kubernetes with KEDA autoscaling, Kustomize (base + overlays), Terraform (AWS), Prometheus + Grafana.

---

## The DevOps lifecycle

Each phase adds a layer of the production toolchain, in the order real systems mature — from "runs on my machine" to "live, observable, and reproducible from code."

| Phase | What | Highlights |
|---|---|---|
| **1 — Containerization** | Multi-stage Dockerfiles | Slim JRE runtime images; FFmpeg bundled into the worker |
| **2 — Local orchestration** | Docker Compose | Whole stack up with one command; service-name networking; health checks |
| **3 — CI/CD** | GitHub Actions | Build + test, Trivy CVE gate (0 tolerated), image push to GHCR, layer caching |
| **4 — Kubernetes** | Manifests + autoscaling | Per-type workers, **KEDA** scaling on RabbitMQ queue depth |
| **5 — Observability** | Prometheus + Grafana | Custom pipeline metrics (jobs by type/outcome, p95 latency, queue depth); dashboards as code |
| **6 — Infrastructure as Code** | Terraform + k3s | One `terraform apply` provisions AWS; **Kustomize** overlays deploy per-environment; live on the internet |

**Deployment is reproducible from code end to end** — Terraform provisions the infrastructure, Kustomize deploys the app:

```bash
# local (Docker Desktop)
kubectl apply -k k8s/overlays/local

# cloud (AWS, via Terraform-provisioned k3s)
terraform -chdir=terraform apply
# ... install k3s on the instance, set the instance IP in the prod overlay ...
kubectl apply -k k8s/overlays/prod
```

---

## Running it locally

**Prerequisites:** Docker, Java 21, Node.js, Maven.

```bash
# 1. bring up the whole stack (infra + api + worker + frontend)
docker compose up -d

# 2. open the app
#    frontend:  http://localhost:8090
#    api docs:  http://localhost:8080/swagger-ui.html
```

For frontend development with hot-reload, run `ng serve` in `frontend/` (proxies `/api` and `/ws` to the backend), and run the backend via Compose or your IDE.

**On Kubernetes (Docker Desktop):**
```bash
kubectl apply -k k8s/overlays/local
kubectl port-forward -n mediaforge service/frontend 8090:80
```

---

## Repository layout

```
common/      shared library — entities, repos, migrations, messaging, storage, JWT
api/         Spring Boot web app — auth, uploads, WebSocket, read endpoints
worker/      Spring Boot app — RabbitMQ consumers + 7 media processors + FFmpeg
frontend/    Angular SPA (+ Dockerfile / Nginx config for production)
k8s/         Kubernetes manifests — Kustomize base + local/prod overlays, KEDA, monitoring
terraform/   AWS infrastructure as code (VPC, EC2, security groups)
.github/     CI/CD pipeline
```

---

## Notable engineering

A few things worth calling out beyond the feature list:

- **Distributed-systems correctness** — idempotent workers, a layered retry system with dead-lettering, and upload-status roll-up that derives an upload's state from its jobs concurrency-safely.
- **Real-time via WebSocket + Redis fan-out** — the Redis pub/sub layer lets any API instance deliver an event to any connected client, so the real-time layer scales horizontally behind a load balancer.
- **Autoscaling on the right signal** — KEDA scales workers on *queue depth* (work waiting), not just CPU, which is the correct signal for a job pipeline.
- **Environment-portable config** — `${VAR:default}` app config + Kustomize overlays mean the same images and manifests run unchanged across local, Compose, and cloud, with only environment-specific values patched in.
- **Ongoing security** — the CI Trivy gate has caught newly-disclosed CVEs on routine pushes (e.g. Tomcat), remediated by dependency overrides — the pipeline catching real drift over time, not just at setup.

---

## Status

Feature-complete and deployed live to AWS via Terraform + k3s (then torn down — the apply-demo-destroy pattern keeps cloud costs negligible). Documented end to end.

**Planned:** HTTPS (domain + Let's Encrypt via Traefik), production monitoring hardening (persistent Prometheus, secured Grafana, alerting), and an automated test suite.