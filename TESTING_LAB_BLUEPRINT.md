# Testing Lab Blueprint

## Mission

Build a testing lab that can verify behavior, routing, persistence, Karoo
comparison, and cloud-twin continuity without mutating production paths
blindly.

## Subsections

| Subsection | Goal | Output |
| --- | --- | --- |
| Smoke | Fast liveness checks | Pass/fail plus timestamp |
| System | End-to-end execution tests | Route-level evidence |
| Benchmark | Latency and throughput measurement | Benchmark events |
| Behavioral | Behavior-pack and feedback fit validation | Approved behavior cards |
| Epoch | One-variable upgrade proof | Promotion or reject report |
| Chaos | Timeout, DB lock, and tunnel recovery drills | Recovery evidence |

## Evidence Rules

1. One changed variable per epoch.
2. Every test writes an evidence summary and status.
3. Every benchmark includes route, duration, and context.
4. Behavioral tests compare liked and disliked intent groups.
5. Promotion requires repeatable success, not a one-off pass.

## Core Test Flows

1. Bridge route smoke:
   `/api/datapoints`, `/api/system/tests`, `/api/rolling`, `/api/loibi/predict`.
2. House route smoke:
   `/health`, generate path, timeout guard path.
3. Shipper smoke:
   `/health`, `/api/uplink`, `/api/resource/status`, cloud tunnel status.
4. Karoo smoke:
   chunk refresh, candidate compare, report generation, checkpoint capture.
5. Behavioral smoke:
   top-5 context pack exists, nominal context remains compact, no prompt crowding.

## Required Reports

1. Daily system summary
2. 25-step checkpoint report
3. Epoch promotion report
4. Tunnel health recovery report
5. DB lock incident report

## Naming Conventions

### Checkpoint Reports

Format:
`PHASE##_CHECKPOINT_##_<focus>_<YYYYMMDDTHHMMSSZ>.md`

Examples:
`PHASE01_CHECKPOINT_02_db_lock_audit_20260514T015218Z.md`
`PHASE03_CHECKPOINT_05_testing_lab_system_benchmark_20260514T015218Z.md`

Rules:

1. Prefix with the roadmap phase number.
2. Include the checkpoint number inside that phase.
3. Use a short focus slug describing the proof slice.
4. End with a UTC timestamp matching the evidence window.

### Epoch Evidence

Format:
`EPOCH_<proposal_id>_<status>_<YYYYMMDDTHHMMSSZ>.json`

Examples:
`EPOCH_SOVEREIGN_AGENT_CONTRACT_proposed_20260514T015218Z.json`
`EPOCH_BRIDGE_HEADROOM_REPAIR_accepted_20260514T005758Z.json`

Rules:

1. Reuse the epoch proposal id exactly.
2. Keep status one of `proposed`, `accepted`, `tested`, `promoted`, `rejected`.
3. Link every epoch file back to one proof SHA-256 when possible.

### Route Evidence

Format:
`ROUTE_<route>_<probe>_<YYYYMMDDTHHMMSSZ>.json`

Examples:
`ROUTE_planning_latency_20260514T015218Z.json`
`ROUTE_build_fast_lane_20260514T015218Z.json`

Rules:

1. Route is `chat`, `planning`, `build`, or a bounded internal alias.
2. Probe names stay short and test-shaped: `latency`, `guardrail`, `proof`, `pack`.
3. Use the same timestamp family as the related lab report when grouped.
