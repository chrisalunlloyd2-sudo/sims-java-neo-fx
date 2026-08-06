# Language Agent Matrix Build Plan

## Objective

Build a reproducible lane where a topological file tree is compiled into
performative page cards, then solved by small language-specific agents under a
100-test matrix before Karoo evolves anything.

This lane is now experimental. The primary path is still bounded brute force
plus the 100-test matrix. Language agents are only worth keeping if they
produce measurable gains.

## Phase Map

| Phase | Focus | Exit |
| --- | --- | --- |
| Phase A | Hardware and runtime audit | GPU and CPU lanes are explicitly bounded |
| Phase B | Performative tree compiler | Every page has a compact page card |
| Phase C | Language-agent triplets | Each page can be authored, verified, and repaired |
| Phase D | 100-test matrix | The page matrix produces repeatable pass/fail evidence |
| Phase E | Optional GPU scoring | Candidate ranking and brute force are accelerated safely |
| Phase F | Karoo evolution and return to upgrades | Only weak pages go to Karoo after the matrix stabilizes |

## Phase A: Hardware And Runtime Audit

1. Record the current GPU model, driver, VRAM, and warnings.
2. Record whether a Vulkan runtime and device path are available locally.
3. Declare the GPU lane optional and non-blocking.
4. Define the CPU fallback path for every GPU-accelerated step.
5. Limit GPU scope to ranking, matrix scoring, and bounded brute force.
6. Exclude GPU from live bridge persistence and DB writes.
7. Exclude GPU from large model hosting assumptions.
8. Publish the hardware limits in the changelog.

## Phase B: Performative Tree Compiler

9. Convert `source_tree_card` or `topology_ascii` into a performative tree.
10. Assign each node a stable page id.
11. Mark each node as `page`, `module`, `dependency`, or `proof_target`.
12. Add language ownership to each page node.
13. Add import/dependency edges to each page node.
14. Add acceptance-test fields to each page node.
15. Add a `single_page_scope` card for each node.
16. Add a `syntax_kernel_ref` field for each language.
17. Add a `performative_contract` field per page.
18. Freeze the page card schema.

## Phase C: Language-Agent Triplets

19. Define the `author_agent` prompt shell.
20. Define the `verifier_agent` prompt shell.
21. Define the `repair_agent` prompt shell.
22. Define the `solo_page_agent` prompt shell for single-model page completion.
23. Ensure only `1` or `3` lightweight models may be active per program lane.
24. Ensure each agent sees only one page card at a time.
25. Ensure each agent sees the current tree context, not the full project dump.
26. Ensure the verifier cannot mutate code.
27. Ensure the repair agent runs only on bounded deficiencies.
28. Route successful pages forward without Karoo intervention.
29. Route failed pages into deficiency reports.
30. Publish the agent prompt contract.

## Phase C2: Rolling Recursive Page Completion

31. Start every page from the minimal page card only.
32. Expand context one bounded slice at a time.
33. Pull sibling or dependency detail only when the current proof step needs it.
34. Rebuild the active page prompt after each verified expansion.
35. Stop expansion once the full page is writable.
36. Record which expansion slices were actually needed.
37. Prefer the solo agent when one lightweight model can finish the page.
38. Escalate to the three-agent triplet only when proof says the solo path is insufficient.
39. Keep the recursive expansion ledger attached to the page.
40. Freeze the recursive page-completion contract.

Gate for Phase C and C2:

- run only after the brute-force plus matrix baseline exists
- keep only if it beats the baseline on useful metrics
- remove or ignore it if it adds complexity without value

## Phase D: 100-Test Matrix

41. Define ten test dimensions.
42. Define ten test levels per dimension.
43. Make the matrix `10 x 10 = 100` total checks.
44. Include syntax validity.
45. Include dependency resolution.
46. Include topology-performative fit.
47. Include route fit.
48. Include compile or parse success.
49. Include deterministic output structure.
50. Include repair success after one deficiency pass.
51. Include proof-log completeness.
52. Include race-risk flags.
53. Include resource/latency thresholds.
54. Require the matrix to finish before Karoo compares pages.

## Phase E: Optional GPU Scoring

55. Convert candidate comparison into small dense scoring matrices.
56. Use Vulkan compute only for score/rank kernels when available.
57. Keep candidate data transfer compact.
58. Run small-variable brute force only on bounded variables.
59. Cap brute-force windows at practical sizes like `top 10`.
60. Compare GPU ranking against CPU ranking for parity.
61. Disable GPU on instability or toolchain absence.
62. Log GPU and CPU timings separately.
63. Never make GPU success a proof gate by itself.

## Phase F: Karoo Evolution And Return To Upgrades

64. Send only weak or tied pages to Karoo.
65. Restrict Karoo to one-variable deltas.
66. Compare the pre-Karoo and post-Karoo page scores.
67. Keep original matrix evidence attached.
68. Promote only if the post-Karoo page beats the baseline.
69. Merge promoted winners back into the page ledger.
70. Close the page batch.
71. Resume broader system-upgrade work after the page batch stabilizes.
72. Publish the final batch summary.

## First Execution Order

1. Build the page card schema.
2. Create the 100-test matrix.
3. Run CPU-only brute force first.
4. Add Vulkan scoring only after CPU proof is stable.
5. Add language-agent experiments only if the simpler lane is insufficient.
6. Run Karoo last.
