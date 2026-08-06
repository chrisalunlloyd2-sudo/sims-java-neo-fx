# Vulkan Scoring Blueprint

## Purpose

Define the bounded Vulkan compute lane for:

1. candidate ranking
2. similarity scoring
3. bounded brute-force variable sweeps
4. CPU parity checks before promotion

This is a scoring lane only. It is not a model-hosting lane.

## Scope

### In scope

- dense score vectors
- adjacency and dependency scoring
- top-k candidate ranking
- bounded permutation scoring
- small-variable brute-force windows such as `top 10`

### Out of scope

- model training
- full inference hosting
- DB write ownership
- compile orchestration
- autonomous page promotion

## Runtime Position

```text
page candidates
  ->
feature pack
  ->
cpu baseline score
  ->
vulkan score kernel
  ->
cpu parity check
  ->
ranked candidates
  ->
matrix gate / Karoo
```

## Required Inputs

- `candidate_id`
- `entry_point`
- `exit_point`
- `entry_scale`
- `exit_scale`
- `topology_anchor_score`
- `performative_score`
- `proof_history_score`
- `timeout_risk_score`
- `dependency_affinity_score`
- `route_fit_score`
- `bytecode_score`

## Feature Vector Contract

Each candidate should compress into a stable float vector:

```text
[
  topology_anchor,
  entry_exit_affinity,
  performative_confidence,
  proof_history,
  timeout_risk,
  dependency_affinity,
  route_fit,
  bytecode_confidence,
  block_fit,
  repair_risk
]
```

## Initial Kernel Set

### Kernel 01: weighted score

Use for:

- first-pass ranking
- parity against current CPU chooser
- genetic-performance weighting

Formula:

```text
score =
  w1 * topology_anchor +
  w2 * entry_exit_affinity +
  w3 * performative_confidence +
  w4 * proof_history +
  w5 * dependency_affinity +
  w6 * route_fit +
  w7 * bytecode_confidence +
  w8 * block_fit -
  w9 * timeout_risk -
  w10 * repair_risk
```

Weight source:

- default weights may seed the run
- promoted weights should come from genetic-performance results over prior
  batches when available

### Kernel 02: bounded brute-force sweep

Use for:

- tiny variable windows
- score recalculation under small weight mutations

Rule:

- only run on small windows
- keep CPU fallback always available
- log the exact search bounds

### Kernel 03: top-k reducer

Use for:

- selecting the best `k`
- reducing transfer back to CPU

## CPU Parity Rule

Never trust Vulkan by itself.

For each scored batch:

1. compute CPU baseline
2. compute Vulkan score
3. compare order and score deltas
4. allow only small tolerance drift
5. if drift exceeds tolerance, keep CPU ordering

## Logging Contract

Add or emulate:

- `GPU_SCORE_JOBS`
- `GPU_SCORE_RESULTS`
- `GPU_CPU_PARITY_CHECKS`

Each record should keep:

- input hash
- weight vector
- genetic generation id
- parent weight lineage
- runtime mode
- elapsed ms
- top-k output
- CPU parity verdict

## Genetic Performance Rule

The scoring lane should learn from repeated measured batches:

1. mutate small weight deltas
2. compare matrix pass quality
3. compare latency/resource impact
4. compare repair burden
5. keep weight sets that improve real outcomes

Do not treat the weight vector as fixed. Treat it as a bounded genetic artifact
with proof attached.

## Promotion Guard

Vulkan output may influence ranking only when:

1. CPU parity passes
2. runtime stays bounded
3. no device/runtime instability is observed
4. the matrix harness still passes

## First Build Order

1. implement CPU reference scorer
2. freeze feature vector shape
3. add Vulkan weighted-score kernel
4. add top-k reducer
5. compare CPU vs Vulkan on the same candidate set
6. only then add bounded brute-force sweeps
