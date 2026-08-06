# ViperNotes SDK Testing And Persistence

## Scope

This file summarizes the desktop-facing role of the Java SDK and its testing/persistence records.

## Key SDK responsibilities

- expose local state cleanly
- record benchmark snapshots
- store epoch upgrade proofs
- keep recursive training epochs append-only
- hold Darwin/algebraic lab data
- preserve persistence events for later replay or review

## Important data files

```text
data/
  |- benchmark_snapshots.jsonl
  |- epoch_upgrade_proofs.jsonl
  |- epoch_implementation_queue.jsonl
  |- training_runs.jsonl
  |- recursive_training_epochs.jsonl
  |- algebraic_pattern_flows.jsonl
  |- darwin_algorithm_generations.jsonl
  |- darwin_algorithm_winners.jsonl
  |- persistence_events.jsonl
  `- web_source_manifest.jsonl
```

## Why this matters to ViperAI

The Java SDK is not separate from the main system in purpose. It is the readable control-and-proof layer for:

- what was tested
- what was proposed
- what was accepted
- what evolved
- what should be promoted later

That makes it one of the cleanest places to understand the current project state without digging through the whole Python/bridge side.
