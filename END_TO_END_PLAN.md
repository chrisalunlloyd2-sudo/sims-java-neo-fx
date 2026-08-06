# 🌐 CODEX OMEGA: END-TO-END SYSTEM ORCHESTRATION

## 1. DAILY MAINTENANCE & ACTIVE WORKSPACE (03:00 AM)
**Goal:** Keep the Commander and Slave nodes lean, fast, and focused only on immediate priorities.
- **Log Truncation:** All system logs (`.log`, `.txt` in log dirs) are truncated to the last 500 lines or 2KB to prevent bloat.
- **Temp Purge:** Deletion of all `.tmp`, `.bak`, and `__pycache__` files.
- **Versioning:** Any file marked as `*_old` or `*_v[X]` not currently active is moved into a local `/versions` subdirectory within its project.
- **The 14-Day Rule:** A recursive scan of the Desktop. Any project or file with a `last_modified` date older than 14 days is automatically packaged and shipped to the `Master_Archive`. The Desktop MUST remain a zone of active execution only.

## 2. STRICT SECURITY SCRUBBING (04:00 AM)
**Goal:** Prevent leaks of personal data, API keys, and conversational context into production builds or shared staging areas.
- **Target:** All projects not actively modified in the last 24 hours.
- **Scrubbing Parameters:**
  - Deletion of Gemini chat exports (`.json`, `.md` dialogues).
  - Deletion/Quarantine of `.env` files, hardcoded `sk-` keys, and `cloudflared` tunnel links from source code.
  - Removal of personal identifier files (`personal_*.txt`).
- **Action:** Offending lines are replaced with `[REDACTED_BY_SOVEREIGN]` and flagged in a security audit log.

## 3. ENVIRONMENT OPTIMIZATION (05:00 AM)
**Goal:** Maximum RAM availability and CPU scheduling for active tasks.
- **Memory Clear:** A forceful sweep of orphaned processes. Any Python, Node, or Java instance not explicitly registered in the `COMMANDER_INIT` or active project PID list is terminated.
- **Garbage Collection:** Force OS-level RAM cache clearing.
- **Network Reset:** Flush DNS and clear dormant TCP sockets to ensure clean Shipper tunnel connections.

## 4. THE FRIDAY ARCHITECT (Fridays @ 23:00 PM)
**Goal:** Comprehensive, AI-driven synthesis of all project states for the week.
- **Coordination:** All active agents (Sovereign, Curator, Shipper) halt active mutation and enter *Synthesis Mode*.
- **Documentation Generation:**
  - **Topology:** Full ASCII trees mapped for every active project.
  - **Blueprints:** 900+ part breakdown of all scripts, UI components, and integrations.
  - **READMEs:** Extensive, verbose descriptions of "How to Use", current state, and architecture.
- **TODO Extraction:** Deep scan of all source files for `// TODO` and `// FIXME`. Compiled into a master `WEEKLY_TODO.md` per project.
- **Final Scrub & Ship:** Intensive final security scrub of all generated documents, followed by packaging and dropping into `VIPER_NAS_SYNC_STAGING` to be broadcast to ALL desktops.

## 5. MANDATED MULTI-NODE TOPOLOGY
To ensure sync compatibility across all desktops (Commander & Slaves), the following directories are permanently locked onto the active Desktop:
1. `SimAgentCity`
2. `DarwinGeneticLLMPIPE`
3. `GITAUTOSHIP`
4. `ViperNotes` (Links to Sovereign Suite)
5. `JRMCHRONOS`
6. `COUNTER`
7. `BVD_ASSIST`
8. `ViperAI`
9. `ViperAiTrain`

---
*Execution automated via `daily_maintenance.py` and `friday_synthesis.py` within the Sovereign ecosystem.*