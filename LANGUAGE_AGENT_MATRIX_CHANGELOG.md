# Language Agent Matrix Changelog

## 2026-05-14

### Added

- Initial Phase 04 blueprint pack for the language-agent matrix lane.
- Bounded plan for GPU-assisted matrix scoring and brute-force variables.
- Step-by-step six-phase build plan for page agents and 100-test matrix work.
- Architecture blueprint for `author_agent`, `verifier_agent`, and
  `repair_agent`.
- Vulkan scoring blueprint for bounded ranking and parity checks.
- Exact page-card schema for single-page language agents.
- Full 100-test matrix specification.
- Concurrency rule for one-model or three-model page lanes only.
- Rolling-recursive page expansion contract for whole-page completion.
- Demoted the language-agent lane to an optional last-step experiment behind the brute-force and matrix baseline.

### Observed

- Local GPU is a `Quadro K4000` with `3 GB` VRAM.
- `vulkaninfo` reports a working Vulkan runtime and detects the device.
- Vulkan instance version is `1.3.301`.
- Quadro K4000 device API version is `1.2.175`.
- `nvidia-smi` also reports a corrupted `infoROM`.

### Decision

- Vulkan-first GPU use is optional and bounded.
- CPU-first execution remains the correct default.
- Karoo stays after the matrix gate, not before it.
