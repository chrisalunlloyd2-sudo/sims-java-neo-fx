# Language Agent Matrix

## Purpose

Define the bounded build lane for:

1. optional GPU-assisted scoring and small-variable brute force
2. language-specific page agents driven by topological performatives
3. a 100-test matrix before Karoo evolution and broader system upgrades

This is the practical bridge from the current programming cube into a
page-by-page code production loop.

## Current Feasibility

### GPU lane

- Local GPU detected: `Quadro K4000`
- Vulkan runtime detected through `vulkaninfo`
- Vulkan instance version: `1.3.301`
- Device API version: `1.2.175`
- VRAM: `3 GB`
- Risk: `nvidia-smi` reports a corrupted `infoROM`

Implication:

- Use the GPU only as an optional Vulkan-first accelerator for matrix scoring,
  similarity/ranking, and bounded brute-force kernels.
- Do not make CUDA a dependency for this lane.
- Do not treat this GPU as a reliable target for live model training or large
  inference.

### Language-agent lane

The concept is sound if each language agent receives only:

1. the language syntax/kernel contract
2. the exact performative contents for the active topological tree
3. the single page or file it is editing
4. the local dependency/proof contract for that page

## Recommended Agent Shape

Use only `1` or `3` lightweight models per program lane, not a large swarm:

1. `solo_page_agent`
   Runs when one compact local model is enough to author, self-check, and
   finish the page inside one bounded loop.
2. `author_agent`
   Creates or updates one page from the page card and language rules.
3. `verifier_agent`
   Checks syntax, imports, performative fit, and proof gates.
4. `repair_agent`
   Runs only if the verifier finds a bounded deficiency.

Model class:

- prefer very lightweight local models only
- examples: Danube-class, Qwen-class, or similarly small coding/reasoning
  runtimes
- the goal is page completion with low overhead, not large-context brute force

Karoo stays after this stage as the post-build comparator/evolver.

## Context Strategy

Do not send the whole program prompt to the model up front.

Use logic to extend from:

1. topology and performative seed
2. page card
3. local dependency slice
4. rolling-recursive expansion until the full page is written

That keeps each model narrow at first and only grows context as the page proves
it needs more.

## Main Flow

```text
intent
  ->
source_tree_card / topology_ascii
  ->
performative tree compiler
  ->
page cards
  ->
solo agent or language-agent triplet
  ->
rolling recursive page expansion
  ->
compile / test / matrix scoring
  ->
100-test gate
  ->
Karoo compare weak pages only
  ->
promote
```

## Files In This Pack

| File | Role |
| --- | --- |
| `LANGUAGE_AGENT_MATRIX_README.md` | This overview and current feasibility note |
| `LANGUAGE_AGENT_MATRIX_BUILD_PLAN.md` | Phased step-by-step execution plan |
| `LANGUAGE_AGENT_MATRIX_BLUEPRINT.md` | Architecture, tables, prompts, proofs, and ASCII flow |
| `LANGUAGE_AGENT_MATRIX_CHANGELOG.md` | Recorded decisions and observed constraints |

## Decision

Proceed with:

1. topology-aware predictive routing first
2. bounded brute force plus the 100-test matrix as the primary page-finishing lane
3. per-language page agents only as a last-step experiment if the simpler lane is not enough
4. bounded GPU scoring as optional acceleration only
5. Karoo evolution after the 100-test page matrix is stable

## Permanence Rule

If the language-agent lane does not add measurable value, do not keep it
forever.

The default assumption should now be:

1. brute force small variables
2. run the matrix
3. only add page agents if they clearly improve completion, speed, or proof quality
