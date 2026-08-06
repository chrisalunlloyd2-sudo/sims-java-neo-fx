# Darwin Lab Changelog

## 2026-05-14

### Added

- `GET /api/darwin-lab`
- `POST /api/darwin-lab`
- seeded local test-program database
- seeded local algorithm-registry database
- persisted generation lineage database
- persisted winner database
- SDK page panel for running and reviewing Darwin batches

### Verified

- live Java SDK listener on `18181`
- local Darwin databases created and populated
- first bounded run completed across `5` generations and `4` programs
- later bounded run completed across `6` generations and `8` programs

### Observed

- first generation winner was the brute-force seed
- later generations improved beyond that baseline
- the first final winner was a repair-balanced mutant with higher promotion success
- expanded seed data now includes `8` programs and `7` code-defined seed algorithms
- generation summaries now store baseline comparison and overtake reason
- verified winners now get promoted into the registry
- one remaining quirk is generation-0 display purity: prior promoted lineage can still appear early in the live winner view even though brute baseline comparison remains explicit
