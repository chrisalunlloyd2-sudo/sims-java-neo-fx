# Architecture Overview

SIMS1337 is a sovereign, anti-fragile neuromorphic OS where **37 Small Language Models** operate continuously on a **4D hex grid**, generating tools, communicating over a distributed ledger, voting in quorum, and evolving without human intervention. 

This page explains the core architectural components that make the system stable, autonomous, and self-maintaining.

---

## 1. Neuromorphic SLM Grid

At the heart of SIMS1337 is the **NEUROMORPHIC SLM GRID**, a 4D hex-based topology where each SLM occupies a cell and interacts with neighbors through structured constraints. 

### Key Properties
- 37 continuously running SLMs  
- 4D hex-grid spatial layout  
- Distributed ledger communication  
- Autonomous quorum voting  
- Self-evolving behavior  

This grid acts as the brain of the organism.

---

## 2. GodHand JavaFX Canvas Renderer

The **GodHand Renderer** is responsible for visualizing the entire neuromorphic grid. It provides:

- **4D animated hex grid visualization**  
- **Real-time model orb positions**  
- **Stress/strain physics display**  

All of this is rendered through a high-performance JavaFX canvas. 

This renderer is the primary way developers observe the organism's internal state.

---

## 3. Cellular Microphone Gating (CMG)

CMG ensures **single-speaker VRAM lock**, preventing memory collisions and guaranteeing stable GPU behavior. 

### CMG Responsibilities
- Enforce one-speaker-at-a-time GPU access  
- Prevent VRAM race conditions  
- Maintain deterministic model execution  
- Provide high-performance gating  

CMG is essential for multi-model stability.

---

## 4. Auto-Healing Router

The **Auto-Healing Router** manages CPU pacing and process recovery. It provides:

- **Dynamic CPU adaptive pacing**  
- **Auto-restart process handlers**  
- **Self-healing behavior**  

This ensures the system remains stable even under heavy load or unexpected model behavior. 

---

## 5. Moltbook Feed

The **Moltbook Feed** is an unrestricted, self-organizing chat logger with:

- Continuous logging  
- Autonomous structuring  
- **2KB auto-archiving** for memory hygiene  

It acts as the organism's journal, capturing internal thoughts, votes, and tool generation events. 

---

## 6. Night Cycle (High-Level)

Although documented separately, the architecture integrates the night cycle phases:

- DREAM  
- VOTE  
- DEPLOY  
- EMAIL  

These phases allow the organism to evolve, validate, and deploy changes autonomously.

---

## 7. System Modules (High-Level Map)

```text
sims-java-neo-fx/
├── src/main/java/com/aigen/sims/
│   ├── GodHandApp.java           # JavaFX 4D Canvas Renderer & UI
│   ├── ClosedLoopOrganism.java   # Local Git Agent & Gossip Scheduler
│   ├── StrainRatePhysicsKernel.java # Rheology Math & Interstitial Cells
│   ├── OllamaRouter.java         # CMG Lock & Exponential CPU Pacing
│   ├── MoltbookSystem.java       # Model Chat Feed & 2KB Log Archiver
│   ├── BruteFoundryCronPipeline.java # Qwen Code Block Mining
│   ├── QwenRepoEditor.java       # Autonomous Repo File & Folder Editor
│   └── GitSecurityScrubber.java  # Token & Password Redactor
```

---

## 8. Architectural Principles

SIMS1337 follows several core principles:

### **Sovereignty**
Local-first, offline-capable, self-contained.

### **Anti-Fragility**
The system improves under stress.

### **Autonomy**
Agents vote, evolve, and deploy without human intervention. 

### **Transparency**
JavaFX renderer + Moltbook feed provide full visibility.

### **Resilience**
Auto-healing router + CMG prevent crashes and collisions.

---

## 🔗 Next Steps

Continue to:

- [Agents & Roles](agents.md)
- [Night Cycle](night-cycle.md)
- [API Reference](api.md)
- [Installation](install.md)
