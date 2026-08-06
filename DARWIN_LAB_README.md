# Darwin Lab

## Purpose

Provide a bounded local evolution lane that:

1. runs against seeded test programs
2. evolves bounded algorithm weight sets
3. stores generations and winners in local JSONL databases
4. keeps brute force plus the matrix as the primary baseline

## Live Endpoint

- `GET /api/darwin-lab`
- `POST /api/darwin-lab`

## Local Databases

- [darwin_test_programs.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_test_programs.jsonl)
- [darwin_algorithm_registry.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_algorithm_registry.jsonl)
- [darwin_algorithm_generations.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_algorithm_generations.jsonl)
- [darwin_algorithm_winners.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_algorithm_winners.jsonl)

## What It Does

1. loads seeded test programs
2. loads seeded algorithms
3. evaluates each algorithm against all programs
4. scores them using matrix-pass, repair burden, latency, topology fit, and promotion success
5. mutates the best algorithms across bounded generations
6. records the brute baseline, generation lineage, and final winner
7. promotes verified winners into the registry without making them the default generation-0 seed pack

## First Verified Run

- `5` generations
- `4` seeded test programs
- `5` seeded algorithms
- first final winner:
  `ALG_REPAIR_BALANCED_SEED_G3_3_G5_2`
- first winner fitness:
  `0.934`

## Current Expanded Run

- `8` seeded test programs
- `7` code-defined seed algorithms
- baseline comparison pinned to `ALG_BRUTE_MATRIX_SEED`
- current best promoted winner:
  `ALG_LATENCY_GUARD_SEED_G2_4_G3_3_G6_4`
- current winner fitness:
  `0.980`

## Current Quirk

The live lane now records the brute baseline explicitly, but fresh runs are
still surfacing one previously promoted lineage early in the winner view. The
DB and proof path are valid; the remaining cleanup is a stricter generation-0
reset so the displayed winner history is purely seed-origin when that mode is
desired.

## Rule

The Darwin lab is useful only if its winners beat or improve the brute-force
plus matrix baseline in measurable ways.
