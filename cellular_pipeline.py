import time
import requests
import json
import logging
import os
import json
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

OLLAMA_HOST = "http://localhost:11434/api/generate"

# A talks to B -> B talks to C and D (AB Route) -> E summarizes
# Strictly Serial Execution Pipeline
# Model A: qwen2.5:0.5b
# Model B: tinyllama:1.1b
# Model C: llama3.2:1b
# Model D: gemma2:2b
# Model E: phi3:mini (Big model for moderation/summary)

def query_model(model, prompt):
    logging.info(f"[{model}] Querying...")
    start_time = time.time()
    try:
        response = requests.post(OLLAMA_HOST, json={
            "model": model,
            "prompt": prompt,
            "stream": False,
            "keep_alive": "5m" if model == "phi3:mini" else "0" # Queen Aegis stays in memory, others mmap/unload
        }, timeout=45)
        
        elapsed = time.time() - start_time
        if response.status_code == 200:
            content = response.json().get("response", "").strip()
            logging.info(f"[{model}] Transaction Complete in {elapsed:.2f}s.")
            return content, elapsed
        else:
            logging.error(f"[{model}] Failed with status {response.status_code}")
            return "ERROR", elapsed
    except Exception as e:
        elapsed = time.time() - start_time
        logging.error(f"[{model}] Connection failed: {e}")
        return "ERROR", elapsed

def load_memory():
    if os.path.exists("cellular_memory.json"):
        with open("cellular_memory.json", "r") as f:
            try:
                return json.load(f)
            except json.JSONDecodeError:
                return []
    return []

def save_memory(memory_list):
    with open("cellular_memory.json", "w") as f:
        json.dump(memory_list[-5:], f, indent=4) # Keep last 5 memory cycles

def run_serial_cellular_pipeline(seed_topic, current_pacing):
    logging.info(f"=== STARTING CELLULAR PIPELINE (Pacing: {current_pacing}s) ===")
    
    # Load past memory
    memory = load_memory()
    memory_context = ""
    if memory:
        memory_context = "Previous Context:\n" + "\n".join([f"- {m}" for m in memory]) + "\n\n"
        
    telemetry = []
        
    # Step 1: Model A Generates Initial Thought
    prompt_a = f"{memory_context}You are Cell A. Briefly introduce an idea about: {seed_topic}"
    result_a, lat_a = query_model("qwen2.5:0.5b", prompt_a)
    telemetry.append(lat_a)
    
    # SHADOW DEPLOYMENT (Step 6) - Mirror traffic to experimental node
    logging.info("[SHADOW DEPLOYMENT] Mirroring Node A traffic to experimental shadow branch...")
    shadow_prompt = f"You are the Experimental Shadow Node. Propose an alternate radical theory based on: {seed_topic}"
    shadow_result, _ = query_model("codellama:7b", shadow_prompt)
    with open("shadow_deployments.log", "a") as f:
        f.write(f"[{time.ctime()}] SHADOW LOG: {shadow_result}\n")
    logging.info("[SHADOW DEPLOYMENT] Shadow evaluation logged safely. Main pipeline unaffected.")
    
    time.sleep(current_pacing) # Dynamic Pacing Telemetry
    
    # Step 2: Model B Receives A's Thought and Expands
    prompt_b = f"You are Cell B. Cell A said: '{result_a}'. Briefly expand on this with one technical detail."
    result_b, lat_b = query_model("tinyllama:1.1b", prompt_b)
    telemetry.append(lat_b)
    
    time.sleep(current_pacing)
    
    # Step 3: Model C processes AB route (Branch 1, strictly serial)
    prompt_c = f"You are Cell C. Critique this statement from Cell B: '{result_b}'. Keep it to one sentence."
    result_c, lat_c = query_model("llama3.2:1b", prompt_c)
    telemetry.append(lat_c)
    
    time.sleep(current_pacing)
    
    # Step 4: Model D processes AB route (Branch 2, strictly serial)
    prompt_d = f"You are Cell D. Support this statement from Cell B: '{result_b}'. Keep it to one sentence."
    result_d, lat_d = query_model("gemma2:2b", prompt_d)
    telemetry.append(lat_d)
    
    time.sleep(current_pacing)
    
    # Step 5: Model E (Queen Aegis) aggregates and moderates
    avg_latency = sum(telemetry) / len(telemetry) if telemetry else 0
    agg_prompt = (
        f"You are the Queen Aegis Node. The swarm experienced an average latency of {avg_latency:.2f}s per hop.\n"
        f"Cell A: {result_a}\n"
        f"Cell B: {result_b}\n"
        f"Cell C (Critique): {result_c}\n"
        f"Cell D (Support): {result_d}\n\n"
        "Synthesize these outputs into a final 2-sentence conclusion."
    )
    result_e, lat_e = query_model("phi3:mini", agg_prompt)
    
    logging.info("=== CELLULAR PIPELINE COMPLETE ===")
    logging.info(f"[FINAL OUPUT E]: {result_e}")
    
    # Save E's conclusion to memory
    if result_e != "ERROR":
        memory.append(result_e)
        save_memory(memory)
        
    return {
        "A": result_a,
        "B": result_b,
        "C": result_c,
        "D": result_d,
        "E": result_e,
        "avg_latency": avg_latency
    }

if __name__ == "__main__":
    import sys
    
    current_pacing = 2 # initial spacing
    
    # If test mode is passed, just run once.
    if len(sys.argv) > 1 and sys.argv[1] == "--test":
        run_serial_cellular_pipeline("Autonomous Swarm Networking", current_pacing)
    else:
        while True:
            data = run_serial_cellular_pipeline("Autonomous Swarm Networking", current_pacing)
            avg_lat = data.get("avg_latency", 0)
            
            # Telemetry logic: Aegis dictates pacing based on latency space
            if avg_lat > 15:
                current_pacing = min(60, current_pacing * 2)
                logging.warning(f"TELEMETRY: High latency detected ({avg_lat:.2f}s). Queen Aegis widened pacing to {current_pacing}s.")
            elif avg_lat < 5 and current_pacing > 2:
                current_pacing = max(2, current_pacing // 2)
                logging.info(f"TELEMETRY: System nominal ({avg_lat:.2f}s). Queen Aegis tightened pacing to {current_pacing}s.")
                
            logging.info(f"Resting pipeline for {current_pacing * 10} seconds before next overarching pulse...")
            time.sleep(current_pacing * 10)
