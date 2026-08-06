# ViperAI Testing Suite Status

## Scope

This file summarizes the local testing-lab and proof path copied into the desktop mirror.

## Current test lanes

- `smoke`
  Purpose: fast “is the system alive and routed correctly?” proof.
- `behavioral`
  Purpose: verify behavior pack injection, lightweight memory shaping, and retrieval context.
- `epoch`
  Purpose: verify proposal/epoch generation and bounded upgrade proof logic.
- `system`
  Purpose: wider integration checks across the local command surfaces.
- `benchmark`
  Purpose: latency, response timing, and route-performance evidence.

## Current proof artifacts in this mirror

```text
testing_lab_reports/
  |- 20260513T110513Z_smoke_testing_lab_report.json
  |- 20260513T110542Z_behavioral_testing_lab_report.json
  |- 20260513T110542Z_epoch_testing_lab_report.json
  |- 20260514T015218Z_system_testing_lab_report.json
  `- 20260514T015218Z_benchmark_testing_lab_report.json
```

## Why this matters

The ViperAI path is being built around proof-first promotion. That means:

- routes are measured, not assumed
- behavioral retrieval is verified, not just described
- latency lanes are compared before promotion
- Karoo proposals stay bounded until evidence supports change

## Testing philosophy

The working principle is:

```text
small proof
  ->
persist report
  ->
compare outcomes
  ->
promote only when the lane is measurably better
```

That fits the current topological coding matrix direction, because the matrix needs test-backed pattern selection rather than unbounded generation noise.
