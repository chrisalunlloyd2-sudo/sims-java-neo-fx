# VIPER Agentic Twin Repro Runbook

## Purpose

Reproduce the current Java SDK testing lab, algebraic cube, physics refinement,
topology template gate, and library growth UI without relying on memory.

## Main Runtime

- Java SDK source:
  [ViperLabSuiteServer.java](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/src/com/viper/notes/ViperLabSuiteServer.java)
- Java SDK output classes:
  [java_notes_suite/out](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/out)
- Java SDK data:
  [java_notes_suite/data](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data)
- Live listener:
  `http://127.0.0.1:18181`

## Compile

```powershell
& 'C:\Users\viper\VIPER_JAVA_RISC\.runtime\jdk21\bin\javac.exe' `
  -d 'C:\Users\viper\VIPER_JAVA_RISC\java_notes_suite\out' `
  'C:\Users\viper\VIPER_JAVA_RISC\java_notes_suite\src\com\viper\notes\ViperLabSuiteServer.java' `
  'C:\Users\viper\VIPER_JAVA_RISC\java_notes_suite\src\com\viper\notes\ViperLabSuiteApp.java' `
  'C:\Users\viper\VIPER_JAVA_RISC\java_notes_suite\src\com\viper\notes\ViperNotesServer.java'
```

## Restart

```powershell
$existing = Get-CimInstance Win32_Process | Where-Object {
  $_.Name -eq 'java.exe' -and $_.CommandLine -like '*com.viper.notes.ViperLabSuiteServer*'
}
if ($existing) {
  $existing | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
  Start-Sleep -Seconds 2
}

Start-Process -FilePath 'C:\Users\viper\VIPER_JAVA_RISC\.runtime\jdk21\bin\java.exe' `
  -ArgumentList @(
    '-cp',
    'C:\Users\viper\VIPER_JAVA_RISC\java_notes_suite\out',
    'com.viper.notes.ViperLabSuiteServer'
  ) `
  -WorkingDirectory 'C:\Users\viper\VIPER_JAVA_RISC'
```

## Health Check

```powershell
Invoke-RestMethod 'http://127.0.0.1:18181/health'
```

Expected:

- `status=ok`
- `suite=viper_java_sdk`
- `port=18181`

## Main Endpoints

- `GET /api/state`
- `GET /api/library-growth`
- `POST /api/algebraic-flow`
- `GET /api/darwin-lab`
- `POST /api/darwin-lab`
- `POST /api/epoch-upgrade-proof`
- `POST /api/epoch-implement`
- `GET /api/log-tail`

## Key UI Panels

- `Algebraic Pattern Lab`
- `Physics Evolution Refinement`
- `Topology Template Gate`
- `Library Growth`

## Current Data Files

- [algebraic_pattern_flows.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/algebraic_pattern_flows.jsonl)
- [training_runs.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/training_runs.jsonl)
- [recursive_training_epochs.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/recursive_training_epochs.jsonl)
- [epoch_upgrade_proofs.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/epoch_upgrade_proofs.jsonl)
- [epoch_implementation_queue.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/epoch_implementation_queue.jsonl)
- [web_source_manifest.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/web_source_manifest.jsonl)
- [darwin_test_programs.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_test_programs.jsonl)
- [darwin_algorithm_registry.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_algorithm_registry.jsonl)
- [darwin_algorithm_generations.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_algorithm_generations.jsonl)
- [darwin_algorithm_winners.jsonl](/C:/Users/viper/VIPER_JAVA_RISC/java_notes_suite/data/darwin_algorithm_winners.jsonl)

## Reproduce Darwin Lab

```powershell
$body = @{
  generations = '5'
  programLimit = '4'
  mutationRate = '0.08'
  objective = 'darwinistically evolve bounded algorithms against local test programs with brute force and matrix first'
  strategy = 'bruteforce_matrix_primary'
} | ConvertTo-Json -Compress

Invoke-RestMethod -Method Post `
  -Uri 'http://127.0.0.1:18181/api/darwin-lab' `
  -ContentType 'application/json' `
  -Body $body
```

## Reproduce Physics Compare

```powershell
$body = @{
  startData = 'source_tree_card'
  endData = 'performative_route_card'
  maxPermutations = '8'
  includeModelProbes = 'false'
  objective = 'compare genetically against established designs for physics'
  customAsciiFlow = 'START_DATA -> compress.card -> axiomatic_set_select -> chooser -> END_DATA'
  customMathNotes = 'score = weighted gene closeness + proof - timeout risk'
  physicsComparisons = '50'
  physicsEvolutionRounds = '50'
  comparisonFamily = 'established_physics_grids'
} | ConvertTo-Json -Compress

Invoke-RestMethod -Method Post `
  -Uri 'http://127.0.0.1:18181/api/algebraic-flow' `
  -ContentType 'application/json' `
  -Body $body
```

## Promotion Interpretation

- `advanceFloor=0.70` means the topology can advance into a bounded
  code-template lane.
- `optimizeTarget=0.95` means the topology is target-grade and is close to the
  desired topological pipe standard.
- `source_tree_card` is the current best topology anchor.
- `Karoo` should evolve after initial page/block template emission, not before.

## Race-Condition Guardrails

- Serialize write-heavy promotion steps.
- Keep proof logging append-only and ordered.
- Record deterministic and stochastic picks before promotion.
- Treat stale PID and tunnel artifacts as separate audits, not promotion input.

## Planned Web Sources

- GitHub as the primary external git crawl
- Hugging Face
- PyPI
- npm
- Maven Central
- crates.io

These are currently manifest-only candidates. They are not active ingestion
pipelines yet, and the current growth-rate math is still driven by home-grown
local proof logs rather than external crawl volume.
