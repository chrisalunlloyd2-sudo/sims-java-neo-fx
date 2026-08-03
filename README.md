# AEGIS GodHand - System Architecture & Manifold Blueprint

## System Overview
This repository contains the Java GodHand GUI (`GodHandApp.java`) and the Master Bootloader (`launch_everything.ps1`). It serves as the central command manifold for a distributed local architecture, integrating Java-based visualization with Python-based inference and legacy desktop automation scripts.

## ASCII Topology Tree
```text
[LOCAL HOST]
 ├── C:\Users\viper\AIGEN_SYS\repos\sims-java-neo-fx\
 │   ├── launch_everything.ps1 (Master Bootloader)
 │   ├── launch_gui.ps1        (Compiles & Runs GodHandApp)
 │   └── src\main\java\com\aigen\sims\GodHandApp.java (Java GUI)
 │
 ├── C:\Users\viper\OneDrive\Desktop\kstaats-karoo_gp-1bc3859\
 │   ├── sims1337_dashboard_server.py (Dashboard)
 │   └── launch_swarm.py              (Python Swarm Core)
 │
 └── C:\Users\viper\OneDrive\Desktop\local_desktop-main\ (Legacy Scripts)
     ├── START_LOGIC_BLOCKCHAIN_PORT.ps1
     ├── START_TOPOLOGY_SIDECAR.ps1
     ├── START_HOUSE_ENGINE_RECOVERY.ps1
     ├── SPIN_UP_AGENT_NODE.ps1
     ├── START_LAB_SUITE.ps1
     ├── START_NOTES_SUITE.ps1
     ├── START_NOTES_TUNNEL.ps1
     └── LAUNCH_MOLTBOOK.ps1
```

## The Manifold Hierarchy (10 Systems)
When `launch_everything.ps1` is executed, it boots the following 10 background systems:

1. **Dashboard Server (Karoo)**: Python web dashboard.
2. **Python Swarm Core**: Autonomous Python scaler and watchdog processes.
3. **Logic Blockchain Shipper**: Local port 18081 data shipper.
4. **Topology Sidecar**: Topology looping mechanism.
5. **House Inference Engine**: Python inference engine (Port 11435).
6. **Lab Suite Server**: Java-based lab execution server.
7. **Notes Suite Server**: Java-based notes server (Port 8091).
8. **Notes Cloudflare Tunnel**: Exposes Port 8091 to a public URL.
9. **Moltbook Triplet Loop**: Ignites Python RISC bridge and Cloudflare tunnel (Port 8080).
10. **AEGIS GodHand Java GUI**: The primary graphical interface displaying the Hex Grid and Manifold Control panel.

## Data Flow & Architecture
* **GUI Command Center**: `GodHandApp.java` uses `Runtime.getRuntime().exec()` to trigger the legacy `.ps1` scripts located in `local_desktop-main`.
* **Local Inference**: The Java application makes HTTP requests to a local `Ollama` instance on port `11434` to process prompts for `deepseek-r1:1.5b`, `tinyllama:1.1b`, and `qwen2.5:0.5b`.
* **Threading**: The Java GUI utilizes `Platform.runLater` and `CopyOnWriteArrayList` to ensure thread-safe updates when background cycles (NightCycleEngine, MCTS Mining) modify the active state.

## Terminal Health & Maintenance
* All background processes are launched with `-WindowStyle Hidden` (except the GUI and Moltbook).
* To shut down the manifold cleanly, use `Stop-Process -Name java` and `Stop-Process -Name python` or let `launch_everything.ps1` handle the zombie process purge on its next run.
