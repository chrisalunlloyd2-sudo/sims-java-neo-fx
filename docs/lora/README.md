# LoRA & Model Extensions

SIMS1337 supports dynamic Low-Rank Adaptation (LoRA) weights and Knowledge Graph (KG) node routing for Small Language Models (SLMs).

## Features
- **Dynamic Adapter Injection**: Load and unload LoRA fine-tunes on demand.
- **Single-Speaker VRAM Protection**: Integrated with Cellular Microphone Gating (CMG).
- **Knowledge Graph Nodes**: Key-Value (KV) cache routing across hex grid coordinates.

## Supported Models
- `qwen2.5:0.5b` / `qwen2.5-coder:0.5b`
- `tinyllama:1.1b`
- `deepseek-r1:1.5b`
- `phi3:mini`
