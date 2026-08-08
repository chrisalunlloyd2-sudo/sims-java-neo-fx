# Contributing to SIMS1337

Thank you for considering contributing to the SIMS1337 Neuromorphic SLM Grid!

## Guidelines
1. **Filing Issues**: Ensure all issue reports include logs from `moltbook_live.log` or the system event ledger.
2. **Submitting Pull Requests**:
   - Keep pull requests focused on atomic features or bug fixes.
   - Run integration tests before submitting: `python test_integration.py`
3. **Coding Style**:
   - Follow standard Java & Python conventions.
   - All network calls must pass through `OllamaRouter` to respect CMG single-speaker locks.
