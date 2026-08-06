# Language Agent Matrix Blueprint

## Core Idea

Treat software generation like a compact NLP kernel:

- topology is the sentence structure
- performatives are the grammar/actions
- page cards are the tokens
- language agents are the local decoders
- proof logs are the loss/evaluation surface

The model should not invent the whole project every time. It should decode one
page at a time from a strict topology/performative representation.

Current priority:

1. brute force bounded variables
2. pass the full matrix
3. weigh candidate strategies by genetic performance
4. only then test whether page agents help enough to keep

## Axiomatic Sets

### Set 01: Topology

- Node
- Edge
- Parent
- Dependency
- Entry point
- Exit point

### Set 02: Language

- Syntax kernel
- Import rules
- Type rules
- Build rules
- Runtime rules

### Set 03: Proof

- Parse
- Compile
- Execute
- Compare
- Record

### Set 04: Repair

- Deficiency
- Hypothesis
- Single-variable fix
- Re-test
- Accept or reject

## Agent Contract

### Concurrency rule

Only these two shapes are allowed:

1. `solo_page_agent`
2. `author_agent + verifier_agent + repair_agent`

Never spin up more than `3` active page models for one program lane at a time.
The smaller the active set, the better.
If the page-agent lane does not beat the brute-force baseline, remove it from
the active path.

### Solo page agent

Input:

- one `page_card`
- one `language_kernel`
- one `performative_contract`
- one rolling-recursive expansion ledger

Output:

- full page candidate
- self-check notes
- proof claims
- requested expansion slices, if any

### Author agent

Input:

- one `page_card`
- one `language_kernel`
- one `performative_contract`
- narrow dependency context

Output:

- candidate page code
- import/dependency claims
- expected proof envelope

### Verifier agent

Input:

- candidate page code
- page card
- proof contract

Output:

- pass/fail
- exact deficiency list
- no code mutation

### Repair agent

Input:

- candidate page code
- deficiency list
- same page card

Output:

- revised code
- revised proof claims
- one-pass bounded repair only

## Per-Page Prompt Shape

```text
ROLE: language author agent
LANGUAGE: <language>
PAGE_ID: <stable page id>
FILE_PATH: <single file only>
TOPOLOGY_CONTEXT: <page parent, siblings, dependencies only>
PERFORMATIVE_CONTRACT: <tell/request/propose/achieve + route owner + proof target>
SYNTAX_KERNEL: <imports, style, rules, compile mode>
ACCEPTANCE_TEST: <single page acceptance contract>
DO NOT EDIT: any other page
OUTPUT: code only for this page
```

## Lightweight Model Policy

Prefer super-light local models for this lane:

- Danube-class small models
- Qwen-class small models
- similar lightweight reasoning/coding runtimes

The point is not raw model size. The point is that logic and topology extend
context only when needed until a whole page is writable.

## Rolling Recursive Page Writer

```text
page_card
  ->
minimal_prompt
  ->
write_partial_page
  ->
proof_check
  ->
if missing_context:
    request next dependency/topology slice
    extend prompt
    continue
  ->
full_page_candidate
```

Rules:

1. start with minimal context
2. extend only when proof requires more
3. keep the expansion ledger
4. stop once the page is complete and verified enough for the matrix

## Data Flow

```text
source_tree_card / topology_ascii
  ->
performative_tree
  ->
page_card_registry
  ->
language_kernel_lookup
  ->
solo_page_agent(page) or author_agent(page)
  ->
verifier_agent(page)
  ->
repair_agent(page if needed)
  ->
matrix_test_harness
  ->
page_score_ledger
  ->
Karoo(weak pages only)
```

## ASCII Runtime Map

```text
              +----------------------+
              | source_tree_card     |
              | topology_ascii       |
              +----------+-----------+
                         |
                         v
              +----------------------+
              | performative_tree    |
              | page_card_registry   |
              +----------+-----------+
                         |
             +-----------+-----------+
             |                       |
             v                       v
   +-------------------+   +-------------------+
   | language_kernel   |   | dependency_slice  |
   | syntax/type/build |   | local page scope  |
   +---------+---------+   +---------+---------+
             |                       |
             +-----------+-----------+
                         |
                         v
               +--------------------+
               | author_agent       |
               +---------+----------+
                         |
                         v
               +--------------------+
               | verifier_agent     |
               +----+-----------+---+
                    |           |
                pass|           |fail
                    |           v
                    |   +--------------------+
                    |   | repair_agent       |
                    |   +---------+----------+
                    |             |
                    +-------------+
                         |
                         v
               +--------------------+
               | 100-test matrix    |
               | proof + latency    |
               +---------+----------+
                         |
                         v
               +--------------------+
               | Karoo weak-page    |
               | comparison only    |
               +--------------------+
```

## GPU Use Cases

### Good fits for the Quadro K4000

- Vulkan compute shader scoring
- score matrix multiplication
- similarity ranking
- bounded candidate sort/re-rank
- tiny search windows for brute-force variables
- adjacency or dependency scoring

### Bad fits for the Quadro K4000

- training a new language model
- large live inference
- huge context embedding sweeps
- full project compile orchestration
- anything that assumes CUDA is the required path

## 100-Test Matrix Shape

Use ten dimensions with ten checks each:

1. syntax
2. imports/dependencies
3. topology fit
4. performative fit
5. compile/parse
6. route fit
7. deficiency repair
8. output determinism
9. latency/resource
10. proof-log completeness

## Proposed Tables

Add or emulate these top tables when the implementation phase starts:

- `LANGUAGE_KERNELS`
- `TOPOLOGY_PAGE_CARDS`
- `PAGE_CONTEXT_EXPANSIONS`
- `PERFORMATIVE_PAGE_CONTRACTS`
- `LANGUAGE_AGENT_CAPABILITIES`
- `PAGE_CANDIDATE_OUTPUTS`
- `PAGE_DEFICIENCY_REPORTS`
- `MATRIX_TEST_RUNS`
- `MATRIX_TEST_RESULTS`
- `GPU_SCORE_JOBS`
- `PAGE_PROMOTION_GATES`

## Promotion Rule

Promote a page only when:

1. page matrix is complete
2. no unresolved deficiency remains
3. route and topology fit are intact
4. GPU-accelerated scores, if used, match CPU sanity checks
5. Karoo improvement is positive or unnecessary

Promotion weighting should prefer algorithms and weight sets with stronger
genetic performance over time:

- higher matrix pass rate
- lower repair burden
- lower timeout/race risk
- better page completion efficiency

## Final Decision

Start CPU-first, page-first, and proof-first.

The GPU should accelerate ranking and bounded brute force later through Vulkan
compute if the local runtime stays stable. It should not own the reasoning loop.

The page-agent lane is a late experiment, not a permanent requirement.
