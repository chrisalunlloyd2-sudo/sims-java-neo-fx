import sqlite3
import json
import requests
import time
import subprocess
from datetime import datetime, timezone

def fetch_ledger_summary():
    """Reads today's events from the Immutable Ledger."""
    conn = sqlite3.connect("swarm_ledger.db")
    cursor = conn.cursor()
    try:
        cursor.execute("SELECT sender, receiver, payload, status FROM EVENT_LOG LIMIT 10")
        rows = cursor.fetchall()
        summary = "Today's Ledger Events:\n"
        for r in rows:
            summary += f"- {r[0]} -> {r[1]}: {r[3]}\n"
        return summary
    except Exception as e:
        return "Ledger empty or unavailable."
    finally:
        conn.close()

def query_ollama(model_name, prompt):
    try:
        res = requests.post(
            "http://localhost:11434/api/generate",
            json={"model": model_name, "prompt": prompt, "stream": False},
            timeout=120
        )
        if res.status_code == 200:
            return res.json().get("response", "")
        return f"Error: {res.status_code}"
    except Exception as e:
        return str(e)

def night_cycle_dream():
    print("=== BEGINNING 00:00 NIGHT CYCLE DREAM ===")
    
    # 1. Read Ledger
    ledger_context = fetch_ledger_summary()
    print(f"Ledger Context:\n{ledger_context}")
    
    # 2. Dream Engine (deepseek-r1)
    print("Awakening deepseek-r1 to cross-correlate memories...")
    dream_prompt = f"Analyze these logs and propose exactly ONE new tool or node capability to improve the swarm:\n\n{ledger_context}"
    proposal = query_ollama("deepseek-r1:1.5b", dream_prompt)
    print(f"\n[PROPOSAL GENERATED]\n{proposal}\n")
    
    # 3. Third-Party Sanity Check (llama3.2:1b)
    print("Awakening llama3.2:1b for Proposal Validation (Cross-Model Sanity Check)...")
    sanity_prompt = f"Does the following proposal make logical sense and is it safe to implement? Reply 'SAFE' or 'UNSAFE'.\nProposal: {proposal}"
    sanity_check = query_ollama("llama3.2:1b", sanity_prompt)
    print(f"\n[SANITY CHECK RESULT]\n{sanity_check.strip()}\n")
    
    if "UNSAFE" in sanity_check.upper():
        print("PROPOSAL REJECTED AT SANITY GATE.")
        return
    
    # 4. Voting Engine (phi3:mini)
    print("Awakening phi3:mini to vote on the proposal...")
    vote_prompt = f"Evaluate this validated proposal for the swarm. Reply with only YES or NO.\nProposal: {proposal}"
    vote = query_ollama("phi3:mini", vote_prompt)
    print(f"\n[VOTE RESULT]\n{vote.strip()}\n")
    
    # 4. Deploy (Simulated)
    if "YES" in vote.upper():
        print("PROPOSAL APPROVED. Pushing to dynamic registry and updating Roadmap...")
        subprocess.run(["python", "autonomous_roadmap_updater.py"])
    else:
        print("PROPOSAL REJECTED.")

if __name__ == "__main__":
    night_cycle_dream()
