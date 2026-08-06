# Real AI 50-Step Build And Test Suite Plan

Goal: turn VIPER into a working local AI system with real recall, real routing, real tool use, real test proof, and honest limits. Each step must produce evidence before the next layer is trusted.

## Axiomatic Sets

- `ServiceSet`: every running process and endpoint.
- `MemorySet`: every durable recall table and file-backed memory store.
- `EndpointSet`: every API route the GUI, agents, and shipper depend on.
- `AgentSet`: every local or remote agent and its capabilities.
- `ReasoningSet`: every planner, chooser, verifier, and model lane.
- `ActionSet`: every operation that mutates code, DB, files, or external state.
- `ProofSet`: every test result, SHA, benchmark, and log proving behavior.
- `UnionSet`: the merged, queryable state used by the AI before answering or acting.

## 50 Steps

1. Define the minimum real AI contract: input, recall, plan, act, verify, explain.
2. List every live service in `ServiceSet`: GUI bridge, house model, shipper, Java SDK, topology sidecar, tunnel.
3. Add a service registry table with `service_id`, `endpoint`, `pid`, `health_url`, `status`, `last_seen`.
4. Add a service health test that probes every registered service and records pass/fail.
5. Fail closed when a required service is missing instead of pretending it works.
6. Define `MemorySet` tables: chat memory, system tests, uplink receipts, relay notices, benchmarks, agent capabilities, task proofs.
7. Put all SQLite writers on `busy_timeout`, WAL mode, and one shared connection helper per process.
8. Add a DB schema migration test that verifies every required table and index exists.
9. Add a recall write/read test for each table, using a unique SHA-tagged probe row.
10. Add a recall union query that returns the latest relevant memories across all memory tables.
11. Add memory source labels so the AI knows whether a fact came from chat, test, log, benchmark, or user instruction.
12. Add memory confidence fields: `observed`, `tested`, `inferred`, `claimed`, `placeholder`.
13. Block high-confidence wording for anything only marked `claimed` or `placeholder`.
14. Build a real `EndpointSet` manifest for every route used by GUI, shipper, Java SDK, and agents.
15. Add endpoint contract tests for method, status code, response schema, and side effects.
16. Make `/api/uplink` a required endpoint with receipt persistence and test proof.
17. Make `/api/agent/heartbeat` idempotent so repeated heartbeats update liveness without queue spam.
18. Add route discovery output so the system can say which routes exist.
19. Add a missing-route test that proves false endpoints return clear `404` JSON.
20. Add GUI smoke tests for `/`, `/api/datapoints`, `/api/system/tests`, and chat submit.
21. Define `AgentSet`: local AI, Karoo, verifier, Java SDK, shipper, remote agent, phone node.
22. Require every agent to register a capability card: can do, cannot do, endpoint, token budget, proof type.
23. Add an agent capability ping test.
24. Add an agent trust level: `untrusted`, `observed`, `proof_submitted`, `approved`.
25. Prevent untrusted agents from mutating code or DB except heartbeat/proof tables.
26. Define `ReasoningSet`: chooser, retriever, planner, executor, verifier, summarizer.
27. Add a route chooser test using fixed prompts for chat, planning, build, repair, and review.
28. Add a retrieval test that proves the answer includes real recalled evidence when available.
29. Add a no-memory test that proves the AI says it lacks evidence instead of inventing.
30. Add a planner output schema: goal, facts, assumptions, missing data, action plan, tests.
31. Add a verifier schema: expected behavior, observed behavior, diff, pass/fail, next action.
32. Add a "do not act" policy gate for code edits, process restarts, DB mutations, and external calls.
33. Add an `ActionSet` ledger table for every mutation: actor, command, files, DB tables, before/after SHA.
34. Add one-action-per-step enforcement for risky operations.
35. Add rollback notes for every code/process action, without auto-reverting user work.
36. Add code-change tests that run compile/lint/smoke checks after edits.
37. Add process restart tests that verify the new process is using the edited file.
38. Add log freshness tests for bridge, shipper, sidecar, Java SDK, and Cloudflare tunnel.
39. Add stale-process detection by comparing PID start time to file edit time.
40. Add `ProofSet` dashboard output: latest passing tests, failing tests, stale evidence, placeholders.
41. Add a red/green CLI command: `python tools/viper_ai_test_suite.py status`.
42. Add a "cool" HTML test dashboard showing services, endpoints, memory, agents, and proofs.
43. Add animated but accurate GUI indicators: green for tested, yellow for degraded, red for failed, gray for placeholder.
44. Add a failing-test drilldown that links to the exact log line or DB row.
45. Add a benchmark lane for latency, tokens/sec, DB write latency, endpoint latency, and memory recall time.
46. Add regression tests for the bugs already found: uplink 404, heartbeat spam, DB locked writes, missing route claims.
47. Add Karoo comparator tests requiring `comparison_count >= 3` before calling it an optimizer.
48. Add a nightly self-test that writes a signed summary into `SYSTEM_TEST_LOG`.
49. Add a "truth report" command that says what is working, degraded, placeholder, or fake.
50. Only call the system "real AI working" when all required ServiceSet, EndpointSet, MemorySet, AgentSet, ReasoningSet, ActionSet, and ProofSet tests pass together.

## First Test Suite Target

The first useful suite should prove:

- GUI loads and returns datapoints.
- SQLite recall writes and reads across all required tables.
- `/api/uplink` returns `202` and stores a receipt.
- repeated heartbeat creates one connection notice, then only updates liveness.
- route chooser returns stable routes for fixed prompts.
- retrieval uses real memory and marks unknowns honestly.
- Karoo is labeled `baseline-only` until comparator evidence exists.
- final status separates `working`, `degraded`, `placeholder`, and `not implemented`.

## Definition Of Done

The system is not done when a document says it is done. It is done when the test suite can be run repeatedly and produces a machine-readable proof that every required set member exists, responds, writes recall, and can be queried by the AI before action.
