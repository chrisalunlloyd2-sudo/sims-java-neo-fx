<div align="center">
  <img src="https://img.shields.io/badge/STATUS-ARMED_AND_ACTIVE-00ff00?style=for-the-badge&logo=power&logoColor=white" />
  <img src="https://img.shields.io/badge/ARCH-NEUROMORPHIC_SWARM-8a2be2?style=for-the-badge" />
  <img src="https://img.shields.io/badge/MODELS-37_SLMS_ONLINE-cyan?style=for-the-badge" />
  <img src="https://img.shields.io/badge/TESTS-37%2F37_PASS-00ff00?style=for-the-badge" />
  <img src="https://img.shields.io/badge/GUI-JAVAFX_v0.23-ff00ff?style=for-the-badge" />
  <img src="https://img.shields.io/badge/WEB_UI-PORT_1337-00F2FE?style=for-the-badge" />
</div>

<h1 align="center">SIMS1337: NEUROMORPHIC SLM GRID</h1>

<p align="center">
  <em>A sovereign, anti-fragile OS where 37 Small Language Models operate continuously on a 4D hex grid, generate tools, communicate over a distributed ledger, vote in quorum, and evolve without human intervention.</em>
</p>

---

## Architecture Overview

```
                    +---------------------------+
                    |     GodHand Java GUI       |
                    |   (JavaFX Canvas Renderer) |
                    |   Deep-Space 4D Hex Grid   |
                    +----------+----------------+
                               |
                    +----------v----------------+
                    |   Web UI (Port 1337)       |
                    |   HTML5 Canvas + REST API  |
                    |   Phone-accessible         |
                    +----------+----------------+
                               |
                    +----------v----------------+
                    |   gui_state_bridge.py      |
                    |   Polls DB + Ollama Live   |
                    |   Writes gui_state.json    |
                    +----------+----------------+
                               |
              +----------------v----------------+
              |        swarm_ledger.db           |
              |  EVENT_LOG | MODEL_STATUS |      |
              |  AGENT_POSITIONS | VIRTUAL_STATIONS |
              +----------------+----------------+
                               |
              +----------------v----------------+
              |     Ollama (37 Local Models)     |
              |  localhost:11434                 |
              |  qwen, tinyllama, deepseek,      |
              |  gemma, phi, codellama, aegis...  |
              +----------------------------------+
```

---

## Current State: Fully Verified

| Component | Status | Details |
|-----------|--------|---------|
| Java GUI (Hex Grid) | **LIVE** | Deep-space theme, 37 models rendered as orbs |
| Web UI (Port 1337) | **LIVE** | Canvas grid, models panel, chat, stats |
| Data Bridge | **RUNNING** | Polls DB + Ollama heartbeat every 1s |
| Database | **SEEDED** | 37 models with hex coordinates |
| Ollama | **RUNNING** | 37 models installed |
| Integration Tests | **37/37 PASS** | Full pipeline verified |

---

## Quick Start

### Prerequisites

| Requirement | Version | Install |
|-------------|---------|---------|
| Java JDK | 17+ | `choco install openjdk17` |
| Ollama | Latest | [ollama.com](https://ollama.com) |
| Python | 3.11+ | [python.org](https://python.org) |
| Git | Any | `choco install git` |

### Install & Run

```powershell
# 1. Clone
git clone https://github.com/chrisalunlloyd2-sudo/local_desktop-main.git
cd local_desktop-main

# 2. Initialize Database (seeds all 37 Ollama models with hex positions)
python init_db.py

# 3. Start the Bridge Daemon (background)
Start-Process python -ArgumentList "gui_state_bridge.py" -WindowStyle Hidden

# 4. Launch the GUI + Web Server
cd sims_java_neo_fx_source
.\LAUNCH_REAL_GUI.ps1

# 5. Open Web UI on your phone
# http://192.168.0.180:1337
```

### Verify Everything Works

```powershell
python test_integration.py
# Expected: 37/37 PASS
```

---

## The 37 Cortical Columns (Installed Models)

| Model | Family | Size | Quant | Capabilities |
|-------|--------|------|-------|-------------|
| qwen2.5:0.5b | qwen2 | 0.5B | Q4_K_M | completion, tools |
| qwen2.5:1.5b | qwen2 | 1.5B | Q4_K_M | completion, tools |
| qwen2.5:3b | qwen2 | 3.1B | Q4_K_M | completion, tools |
| qwen3:4b | qwen3 | 4.0B | Q4_K_M | completion, tools, thinking |
| qwen3:latest | qwen3vl | 8.8B | Q4_K_M | vision, completion, tools |
| tinyllama:1.1b | llama | 1.1B | Q4_0 | completion |
| deepseek-r1:1.5b | deepseek | 1.5B | Q4_K_M | completion |
| llama3.2:1b | llama | 1.2B | Q8_0 | completion, tools |
| gemma2:2b | gemma2 | 2.6B | Q4_0 | completion |
| codellama:7b | llama | 6.7B | Q4_0 | completion |
| phi:latest | phi2 | 2.7B | Q4_0 | completion |
| phi3:mini | phi3 | 3.8B | Q4_K_M | completion |
| nomic-embed-text | nomic-bert | 137M | F16 | embedding |
| aegis-distilled-27b | qwen35 | 26.9B | Q2_K | completion |
| aegis-gemma2-abliterated:2b-q8 | gemma2 | 2.6B | Q8_0 | completion |
| huihui_ai/qwen3-abliterated:8b | qwen3 | 8.2B | Q4_K_M | completion, tools, thinking |
| kiwi_kiwi/qwen3.5-abliterated:9b | qwen35 | 9.7B | Q4_K_M | vision, completion, tools, thinking |
| *...and 20 more* | | | | |

---

## GUI Tab System

| Tab | Button | Status | Description |
|-----|--------|--------|-------------|
| HEX GRID | View | **WORKING** | 4D animated hex grid with model orbs, star field, connection lines |
| SWARM INFO | View | **WORKING** | Real-time model registry with status, hex positions, latency |
| LIVE CHAT | View | **WORKING** | Event stream from swarm_ledger.db |
| LOGIC BLOCKCHAIN PORT | Action | **ACTIVE** | Self-evolution engine, model voting on GUI additions |
| TOPOLOGY SIDECAR | Action | **ACTIVE** | Edit agent interaction patterns |
| HOUSE ENGINE RECOVERY | Action | **ACTIVE** | Auto-diagnose and repair Ollama/model issues |
| SPIN UP AGENT NODE | Action | **ACTIVE** | Full agent builder with 130+ parameters |
| LAB SUITE | Action | **ACTIVE** | A/B testing lab with epochs and statistical validation |
| NOTES SUITE | Action | **ACTIVE** | viper_notes v18 with rich text, clipboard, GitHub archive |
| NOTES TUNNEL | Action | **ACTIVE** | UDP compute lend with SHA-256 proof |
| MOLTBOOK | Action | **ACTIVE** | Untethered dreaming - models cross-correlate freely |

---

## Data Pipeline

```
User Input (GUI/Web)
    |
    v
EVENT_LOG (SQLite)  <---->  gui_state_bridge.py (1s poll)
    |                              |
    v                              v
MODEL_STATUS (37 models)     gui_state.json
    |                              |
    v                              v
AGENT_POSITIONS (hex history)  Java GUI + Web UI
    |
    v
Ollama (localhost:11434)
    |
    v
37 Local SLMs (GPU/CPU inference)
```

---

## Night Cycle — Autonomous Operation

```
00:00  DREAM    Models cross-correlate, generate proposals
06:00  ANALYZE  Review proposals against fitness criteria
12:00  TEST     A/B test promising proposals
18:00  VOTE     Quorum voting (5+ yes = approved)
20:00  DEPLOY   Approved changes become real tools/stations
22:00  EMAIL    Brief sent to chrisalunlloyd2@gmail.com
```

---

## 22 Backend Systems

| # | System | Status |
|---|--------|--------|
| 1 | Hospital (Agent diagnostics) | ACTIVE |
| 2 | Brute Foundry (Code generation) | ACTIVE |
| 3 | Knowledge Graph (23 nodes, 19 edges) | ACTIVE |
| 4 | Server Orchestration | ACTIVE |
| 5 | Self-Exploration | ACTIVE |
| 6 | Error Logging | ACTIVE |
| 7 | Design System | ACTIVE |
| 8 | Real RAG (64-dim vectors) | ACTIVE |
| 9 | Fine-Tuning (4 datasets) | ACTIVE |
| 10 | Multi-Agent Topology | ACTIVE |
| 11 | Web Dashboard (:1337) | ACTIVE |
| 12 | Plugin System (5 plugins) | ACTIVE |
| 13 | Perfect Prompts (8 templates) | ACTIVE |
| 14 | Map Guidance (61 hex weights) | ACTIVE |
| 15 | Perfect Patterns (8 routes) | ACTIVE |
| 16 | Tools System (10+dynamic) | ACTIVE |
| 17 | Persistent Memory (3 agents) | ACTIVE |
| 18 | FOW - Fog of War (1-hop) | ACTIVE |
| 19 | Hex TODO System | ACTIVE |
| 20 | Gist Context (11 fragments) | ACTIVE |
| 21 | Gist Sync (30min) | ACTIVE |
| 22 | Night Cycle (Armed) | ACTIVE |

---

## Secure Build Pipelines

| Pipeline | File | Purpose |
|----------|------|---------|
| Brute Foundry | `brute_foundry.bat` | Compile + test + deploy cycle |
| Java SIMS | `build_java_sims.ps1` | JavaFX compilation |
| Python Sandbox | `build_python_sandbox.ps1` | Virtualenv + dependency install |
| Native C | `build_native_c.ps1` | GCC/MSVC native compilation |

---

## Game Engine Bridges

| Engine | File | Method |
|--------|------|--------|
| Unity | `Unity_Ollama_Bridge.cs` | UnityWebRequest + Coroutines |
| Unreal | `Unreal_Ollama_Bridge.py` | Python API + Blueprint node |
| Godot | `Godot_Ollama_Bridge.gd` | HTTPRequest + Signals |

---

## Testing

```powershell
# Full integration test (37 tests)
python test_integration.py

# Database check
python check_db.py

# Ollama connectivity
curl http://localhost:11434/api/tags

# Web UI health
curl http://localhost:1337/api/status

# Java process
tasklist /FI "IMAGENAME eq java.exe"
```

---

## Mathematical Framework: Semantic Routing

The performative extraction engine uses topological homeomorphisms to map user intent to swarm actions:

```
Source Manifold (Natural Language) --[Phi]--> Target Manifold (Actions)

F(x) = -nabla V  (gradient vector field)

Where:
- Embedding: nomic-embed-text (768-dim vectors)
- Similarity: Jaccard + Cosine distance
- Routing: Gradient descent to nearest actionable attractor
- Validation: Shannon entropy threshold for confidence
```

---

## License

Sovereign. This system belongs to its architect.
