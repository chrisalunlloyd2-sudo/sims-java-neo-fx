"""
SIMS1337 - GUI State Bridge (Phase 1 Step 3)
Polls SQLite + Ollama live, writes gui_state.json for both UIs to consume.
Runs as a background daemon.
"""
import sqlite3
import json
import time
import os
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(SCRIPT_DIR, "swarm_ledger.db")
STATE_PATH = os.path.join(SCRIPT_DIR, "gui_state.json")

def ping_ollama_model(model_name):
    """Quick health check - asks the model to say one word."""
    try:
        payload = json.dumps({
            "model": model_name,
            "prompt": "Say OK",
            "stream": False
        }).encode("utf-8")
        req = urllib.request.Request(
            "http://localhost:11434/api/generate",
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        start = time.time()
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode())
        latency = (time.time() - start) * 1000
        return True, latency, data.get("response", "")
    except:
        return False, 0, ""

def build_state():
    """Read DB + Ollama status, build complete JSON state."""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    c = conn.cursor()
    
    # Models with positions
    models = []
    for row in c.execute("SELECT * FROM MODEL_STATUS ORDER BY model_name"):
        models.append({
            "name": row["model_name"],
            "family": row["family"],
            "params": row["parameter_size"],
            "quant": row["quant_level"],
            "q": row["hex_q"],
            "r": row["hex_r"],
            "status": row["status"],
            "latency_ms": row["last_latency_ms"],
            "entropy": row["last_entropy"],
            "context_length": row["context_length"],
            "embedding_length": row["embedding_length"]
        })
    
    # Recent chat events
    chats = []
    for row in c.execute("SELECT sender, receiver, payload, status, timestamp FROM EVENT_LOG ORDER BY id DESC LIMIT 30"):
        chats.append({
            "sender": row["sender"],
            "receiver": row["receiver"],
            "message": row["payload"],
            "status": row["status"],
            "time": row["timestamp"]
        })
    
    # Position history (last 100 moves)
    positions = []
    for row in c.execute("SELECT model_name, hex_q, hex_r, timestamp, action FROM AGENT_POSITIONS ORDER BY id DESC LIMIT 100"):
        positions.append({
            "model": row["model_name"],
            "q": row["hex_q"],
            "r": row["hex_r"],
            "time": row["timestamp"],
            "action": row["action"]
        })
    
    conn.close()
    
    return {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "model_count": len(models),
        "models": models,
        "chats": chats,
        "position_history": positions
    }

def heartbeat_cycle(conn, fast_models):
    """Ping a small set of models each cycle to update their status."""
    c = conn.cursor()
    for model_name in fast_models:
        alive, latency, resp = ping_ollama_model(model_name)
        status = "ACTIVE" if alive else "OFFLINE"
        c.execute("""
            UPDATE MODEL_STATUS 
            SET status=?, last_latency_ms=?, last_active=CURRENT_TIMESTAMP
            WHERE model_name=?
        """, (status, round(latency, 2), model_name))
        
        if alive:
            c.execute("""
                INSERT INTO EVENT_LOG (sender, receiver, payload, status)
                VALUES (?, 'BRIDGE', ?, ?)
            """, (model_name, f"Heartbeat OK ({latency:.0f}ms): {resp[:60]}", status))
    
    conn.commit()

def main():
    print(f"[BRIDGE] DB: {DB_PATH}")
    print(f"[BRIDGE] Output: {STATE_PATH}")
    print("[BRIDGE] Starting continuous sync loop...")
    
    # Pick 3 small models to heartbeat (won't bog down GPU)
    fast_models = ["qwen2.5:0.5b", "tinyllama:1.1b", "smollm2:360m"]
    
    cycle = 0
    while True:
        try:
            # Every 10th cycle (~10s), ping a model for real status
            if cycle % 10 == 0:
                conn = sqlite3.connect(DB_PATH)
                heartbeat_cycle(conn, [fast_models[cycle // 10 % len(fast_models)]])
                conn.close()
            
            # Every cycle, rebuild and write state
            state = build_state()
            
            with open(STATE_PATH, "w") as f:
                json.dump(state, f, indent=2)
            
            if cycle % 5 == 0:
                print(f"[BRIDGE] Cycle {cycle}: {state['model_count']} models, {len(state['chats'])} events written")
            
        except Exception as e:
            print(f"[BRIDGE ERROR] {e}")
        
        cycle += 1
        time.sleep(1)

if __name__ == "__main__":
    main()
