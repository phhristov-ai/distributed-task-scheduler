# Distributed Task Scheduler

A Spring Boot-based distributed task scheduler that executes scheduled jobs asynchronously using a pool of workers.

The project focuses on reliable job execution in a multi-instance environment, including:

- Atomic job claiming
- Concurrent workers
- Retry with exponential backoff
- Maximum retry attempts
- Lease-based failure recovery
- At-least-once execution semantics
- PostgreSQL persistence
- Strategy-based job handlers
- Unit, integration, and concurrency testing

---

## Architecture

```text
                         ┌──────────────────────┐
                         │      REST API        │
                         │                      │
                         │ Create / Get Jobs    │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │     PostgreSQL       │
                         │                      │
                         │       jobs           │
                         └──────────┬───────────┘
                                    │
                                    │
                         ┌──────────▼───────────┐
                         │     Job Scheduler     │
                         │                       │
                         │ Find due jobs         │
                         └──────────┬────────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │    TaskExecutor      │
                         │    Thread Pool       │
                         └──────────┬───────────┘
                                    │
                           ┌────────┴────────┐
                           ▼                 ▼
                    ┌──────────────┐  ┌──────────────┐
                    │ Job Worker   │  │ Job Worker   │
                    └──────┬───────┘  └──────┬───────┘
                           │                 │
                           └────────┬────────┘
                                    ▼
                         ┌──────────────────────┐
                         │    Job Claiming      │
                         │                      │
                         │ Atomic DB UPDATE     │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │   Job Handler        │
                         │                      │
                         │ SEND_EMAIL           │
                         │ REPORT               │
                         │ CLEANUP              │
                         └──────────────────────┘