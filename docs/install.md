# Installation Guide
Welcome to the installation guide for **SIMS1337 Java Neo FX**, the sovereign neuromorphic engine built on a 4D hex grid with 37 continuously running SLMs.

This guide walks you through installing, configuring, and launching both the **JavaFX GUI** and **Web Dashboard**.

---

## 🛠️ Requirements

### **Operating System**
- Windows 10/11
- macOS (Intel or Apple Silicon)
- Linux (Ubuntu, Arch, Fedora)

### **Runtime**
- **Java 17+** (Temurin recommended)
- **Maven** (3.8+)
- **Git** (for cloning the repo)

---

## 🚀 1. Clone the Repository

```bash
git clone https://github.com/chrisalunlloyd2-sudo/sims-java-neo-fx.git
cd sims-java-neo-fx
```

---

## 🖥️ 2. Run the JavaFX GUI
The JavaFX renderer powers the 4D animated hex grid, showing real-time model orb positions and stress/strain physics.

```bash
mvn compile
mvn javafx:run
```

---

## 🌐 3. Run the Web Dashboard
The Web UI provides:
- Live agent telemetry
- Night cycle status
- Moltbook feed
- Auto-healing router metrics

The dashboard opens at:
```text
http://localhost:1337
```

---

## ⚙️ 4. First-Time Configuration

### A. CMG (Cellular Microphone Gating)
Ensures single-speaker VRAM lock and prevents memory collisions.
Configured via `OllamaRouter.java`.

### B. Night Cycle
Controls autonomous phases: `DREAM`, `ANALYZE`, `TEST`, `VOTE`, `DEPLOY`, `EMAIL`.

### C. Sovereign Analytics
Integrated security scrubber and ledger tracking.

---

## 🧪 5. Run Tests
```bash
mvn test
```
Integration tests validate:
- Hex grid stability
- Agent quorum behavior
- Auto-healing router
- Moltbook feed archiving
