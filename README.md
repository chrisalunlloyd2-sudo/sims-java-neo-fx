<div align="center">
  <img src="docs/screenshots/sims1337_repo_banner.jpg" alt="SIMS1337 Banner" width="100%" />
  <br/><br/>

  <a href="https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx/actions"><img src="https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white" /></a>
  <img src="https://img.shields.io/badge/models-37-blue?style=for-the-badge&logo=cpu" />
  <img src="https://img.shields.io/badge/night--cycle-armed-purple?style=for-the-badge&logo=moon" />
  <img src="https://img.shields.io/badge/gui-javafx_v0.23-ff00ff?style=for-the-badge" />
  <img src="https://hits.seeyoufarm.com/api/count/incr/badge.svg?url=https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx&count_bg=%237928CA&title_bg=%23555555&icon=&icon_color=%23E7E7E7&title=VISITORS&edge_flat=false" />
  <br/><br/>

  <h3>
    <a href="#quickstart">Quickstart</a> •
    <a href="docs/index.md">Documentation</a> •
    <a href="https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx/releases">Downloads</a> •
    <a href="SECURITY.md">Security</a> •
    <a href="CHANGELOG.md">Changelog</a>
  </h3>
</div>

<h1 align="center">SIMS1337: NEUROMORPHIC SLM GRID</h1>

<p align="center">
  <em>A sovereign, anti-fragile OS where 37 Small Language Models operate continuously on a 4D hex grid, generate tools, communicate over a distributed ledger, vote in quorum, and evolve without human intervention.</em>
</p>

---

## Feature Overview

- ⬡ **4D Hex Grid Renderer**: Real-time JavaFX canvas rendering deep-space visual field & model orb coordinates.
- ⚡ **37 Cortical SLMs**: Local Small Language Models (`qwen2.5`, `tinyllama`, `deepseek-r1`, `phi3`) coordinated over SQLite ledger.
- 🛡️ **Cellular Microphone Gating (CMG)**: Strict single-speaker lock preventing VRAM memory collisions.
- 🧪 **Viscoelastic Physics Kernel**: Dynamic strain rate ($\dot{\gamma}$), viscosity ($\eta$), and stress ($\sigma$) math.
- 🌙 **Autonomous Night Cycle**: Unattended dreaming, A/B testing, proposal evaluation, and quorum voting.
- 🔒 **Git Security Scrubber**: Redacts OAuth tokens, PATs, and passwords from all git uploads.

---

## Quickstart

```bash
# Clone the repository
git clone https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx.git
cd sims-java-neo-fx

# Compile & Run GodHand JavaFX GUI
mvn compile
mvn javafx:run
```

---

## Architecture Diagram

```text
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

## Project Structure Map

```text
sims-java-neo-fx/
├── docs/                        # GitHub Pages Documentation Site
│   ├── index.md                 # Documentation homepage
│   ├── install.md               # Installation & setup guide
│   ├── architecture.md          # Neuromorphic core architecture
│   ├── agents.md                # 37 SLM Cortical Array topology
│   ├── night-cycle.md           # Night Cycle autonomous schedule
│   ├── api.md                   # REST API documentation
│   ├── faq.md                   # Frequently Asked Questions
│   └── screenshots/             # Repository visual assets & banners
├── src/main/java/com/aigen/sims/ # Core Java Engine
│   ├── GodHandApp.java           # JavaFX 4D Canvas Renderer & UI
│   ├── ClosedLoopOrganism.java   # Local Git Agent & Gossip Scheduler
│   ├── StrainRatePhysicsKernel.java # Rheology Math & Interstitial Cells
│   ├── OllamaRouter.java         # CMG Lock & Exponential CPU Pacing
│   ├── MoltbookSystem.java       # Model Chat Feed & 2KB Log Archiver
│   ├── BruteFoundryCronPipeline.java # Qwen Code Block Mining
│   ├── QwenRepoEditor.java       # Autonomous Repo File & Folder Editor
│   └── GitSecurityScrubber.java  # Token & Password Redactor
├── CHANGELOG.md                 # Project changelog
├── CONTRIBUTING.md              # Contributor guidelines
├── SECURITY.md                  # Security policy & scrubber docs
├── LICENSE                      # MIT Sovereign License
└── README.md                    # Repository centerpiece
```

---

## Downloads & Releases

Visit [/releases](https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx/releases) to download compiled binaries (`sims-java-neo-fx-0.2.0-SNAPSHOT.jar`).

---

## License & Sovereign Policy

This project is licensed under the [MIT License](LICENSE).
