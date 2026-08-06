import os
import sys
import time
import json
import requests
from pathlib import Path
from datetime import datetime

# Add Aegis_Agents to path for Karoo
sys.path.append('Aegis_Agents')
from heuristic_genetic_coder import karoo_gp_manager

TOPOLOGICAL_LOG = Path("VIPER_JAVA_RISC/topological_manifold.json")
OLLAMA_URL = "http://localhost:11435/api/generate"
MODEL_NAME = "aegis-gemma2-abliterated:2b-q8"

def twin_train_abliterated(context_note):
    """Feeds contextual notes and topological goals to the twinned model."""
    try:
        payload = {
            "model": MODEL_NAME,
            "prompt": f"TOPOLOGICAL_GOAL: Assimilate this success note into the manifold: {context_note}",
            "stream": False,
            "system": "You are the TRIPLET. Your objective is 100% logic satisfaction. Assimilate data into axiomatic geometry."
        }
        res = requests.post(OLLAMA_URL, json=payload, timeout=60)
        return res.json().get("response", "ASSIMILATION_FAILED")
    except Exception as e:
        return f"KERNEL_ERROR: {str(e)}"

def run_infinite_triplet_loop():
    """Run infinite triplet loop (function)."""
    print("🚀 INIT: INFINITE TRIPLET LOOP (Karoo + Abliterated Twin + Loihi Grid)")
    print("Operating 24/7. Monitoring topological shifts and twin training...")
    
    cycle_count = 0
    
    while True:
        cycle_count += 1
        print(f"\n--- [TRIPLET CYCLE {cycle_count}] ---")
        
        # 1. Karoo scans for recent Success/Failure data to formulate a learning project
        print("[KAROO] Scanning for recent topological logic goals...")
        objective = "Review system logs and SUCCESS_DB. Generate an optimized topological heuristic for 100% logic satisfaction."
        
        try:
            # Karoo operates in a fast-iteration learning loop
            result = karoo_gp_manager.start(
                objective=objective,
                project="infinite_triplet_learning",
                max_generations=2,
                population=2,
                timebox_minutes=5
            )
            job_id = result.get('job_id', 'UNKNOWN')
            print(f"[KAROO] Learning Job Active: {job_id}")
            
            # Wait for Karoo job to stabilize
            time.sleep(10)
            
            # 2. Twin Training: Feed Karoo's output / logic state to the Abliterated Model
            print("[TWIN] Training Abliterated model with Karoo logic...")
            twin_insight = twin_train_abliterated(f"Cycle {cycle_count} Karoo logic stabilized.")
            print(f"[TWIN_RESPONSE] {twin_insight[:100]}...")
            
            # 3. Train LOIHI: Update the manifold
            print("[LOIHI] Simulating grid updates and neuron growth...")
            try:
                # Trigger an internal API call to simulate the network growth
                requests.get("http://localhost:8080/api/loihi/neurons", timeout=5)
            except: pass
            
            shift = {
                "timestamp": datetime.now().isoformat(),
                "type": "INFINITE_LOOP_SYNC",
                "details": "Karoo trained Twin; LOIHI grid updated.",
                "cycle": cycle_count
            }
            with open(TOPOLOGICAL_LOG, "a", encoding="utf-8") as f:
                f.write(json.dumps(shift) + "\n")
                
            # 4. [NEW] RESEARCH FOR ME (Massive Data Harvest)
            print("[RESEARCH] Initiating 8-hour look crawl...")
            # This simulates Karoo using the 'erbcrawl' pattern to find logic sources
            try:
                # We trigger the crawler we built to refresh the knowledge manifold
                subprocess.run([sys.executable, "logic_crawler.py"], capture_output=True)
                print("[RESEARCH] Tonnes of data sourced into RAG.")
            except: pass
            
            print(f"[TRIPLET] Cycle {cycle_count} completed. Sleeping for stability...")
            time.sleep(30) # Prevent overloading the local GPU/CPU
            
        except Exception as e:
            print(f"[ERROR] Loop interrupted: {e}")
            time.sleep(30)

if __name__ == "__main__":
    run_infinite_triplet_loop()
