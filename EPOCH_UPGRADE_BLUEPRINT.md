# Epoch Upgrade Blueprint

## Goal

Create a disciplined one-variable upgrade system that uses Karoo comparison,
testing lab proof, and explicit promotion gates instead of uncontrolled drift.

## Top Table

| Field | Meaning |
| --- | --- |
| Epoch ID | Unique run identifier |
| Subsystem | Bridge, retrieval, Karoo, shipper, testing lab, or twin |
| Changed Variable | Exactly one thing changed |
| Baseline | Before-state metrics and evidence |
| Candidate | Proposed adjustment |
| Comparator Set | At least three meaningful comparison points when applicable |
| Proof | Test and benchmark results |
| Promotion Decision | Promote, hold, or reject |

## Epoch Contract

1. Baseline first.
2. One variable only.
3. Compare against at least one local baseline and one current best known
   alternative whenever possible.
4. Record human-readable summary plus hashes.
5. Promote only after repeatable proof.

## Promotion Gate

1. Success rate target: `>= 99.99` for promoted operational paths.
2. Improvement target: at least one measurable gain in latency, resource use,
   stability, or answer quality.
3. No GUI regression.
4. No silent persistence regression.
5. No increase in uncontrolled prompt bulk.

## Required Epoch Subsections

1. Retrieval epoch
2. Behavioral epoch
3. Build-route epoch
4. Karoo comparator epoch
5. Cloud twin sync epoch
6. Recovery epoch

