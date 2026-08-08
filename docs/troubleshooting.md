# Troubleshooting Guide

Common issues and solutions for **SIMS1337 Java Neo FX**.

---

## 🖥️ JavaFX Window Doesn't Open
- **Ensure Java 17+**: Check version with `java -version`. Update using `choco install openjdk17`.
- **Module Exports / Flags**: Try using explicit `--add-opens` flags:
  ```bash
  mvn javafx:run
  ```
  Or launch manually:
  ```bash
  java --module-path "$PATH_TO_FX" --add-modules javafx.controls,javafx.fxml -jar target/sims-java-neo-fx-0.2.0-SNAPSHOT.jar
  ```

---

## 🌐 Web UI Blank Page
- **Dependencies**: Re-run package installation:
  ```bash
  npm install
  ```
- **Port Availability**: Check if port `1337` is blocked or already bound:
  ```bash
  netstat -ano | findstr :1337
  ```

---

## 🌙 Night Cycle Not Advancing
- **Timestamp Verification**: Verify cycle timestamps and phase values in `config/system/night-cycle.json`.
- **Database Connection**: Ensure `swarm_ledger.db` is initialized and accessible by `gui_state_bridge.py`.
