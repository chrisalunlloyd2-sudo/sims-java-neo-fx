"""
SIMS1337 - Cellular Microphone Engine
====================================
The "cellular microphone" technique:
- One Queen Bee model stays RAM-resident permanently (3b abliterated)
- All other models are cycled through via mmap from SSD with minimal RAM
- Each model gets directed prompts, builds KG nodes, and reports back
- Models are evicted after use (keep_alive=0) to free resources
- Queen Bee routes, summarizes, and maintains the shared memory

Ollama Optimization Settings:
- OLLAMA_FLASH_ATTENTION=1 (reduce memory, faster inference)
- OLLAMA_KV_CACHE_TYPE=q8_0 (compressed KV cache)
- OLLAMA_MAX_LOADED_MODELS=2 (queen + 1 worker at a time)
- num_gpu=0 for workers (CPU-only via mmap from SSD)
- num_gpu=99 for queen (GPU-resident)
- keep_alive=0 for workers (evict immediately after response)
- keep_alive=-1 for queen (never evict)
- num_ctx=2048 for workers (small context, fast)
- num_ctx=4096 for queen (larger context for routing)
"""

import sqlite3
import json
import time
import os
import urllib.request
import hashlib
import math
import random

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(SCRIPT_DIR, "swarm_ledger.db")
OLLAMA_URL = "http://localhost:11434"

# === QUEEN BEE CONFIG ===
QUEEN_MODEL = "dagbs/qwen2.5-coder-3b-instruct-abliterated:q8_0"
QUEEN_KEEP_ALIVE = "-1"  # Never evict
QUEEN_NUM_GPU = 99       # Full GPU

# === WORKER CONFIG ===
WORKER_KEEP_ALIVE = "0"  # Evict immediately after response
WORKER_NUM_GPU = 0        # CPU-only, mmap from SSD
WORKER_NUM_CTX = 2048     # Small context for speed

# All models to cycle through (excluding queen and embedding models)
EXCLUDE_MODELS = {QUEEN_MODEL, "nomic-embed-text:latest"}

# === SEED PROMPTS for directed work ===
SEED_TASKS = [
    {
        "type": "code_review",
        "prompt": "Review this code pattern and suggest one specific improvement. Be concise (2 sentences max): {context}",
        "weight": 3
    },
    {
        "type": "kg_node",
        "prompt": "Given this concept: '{context}', produce exactly ONE knowledge graph triple in format: (subject, predicate, object). Nothing else.",
        "weight": 2
    },
    {
        "type": "hex_strategy",
        "prompt": "You are agent at hex position ({q},{r}). Your neighbors are at positions {neighbors}. Suggest ONE strategic action for the swarm grid. One sentence only.",
        "weight": 2
    },
    {
        "type": "self_reflect",
        "prompt": "You are model '{model_name}' with {params} parameters. Describe your ONE strongest capability in exactly 10 words.",
        "weight": 1
    },
    {
        "type": "dream",
        "prompt": "Cross-correlate these two concepts and produce ONE novel insight: Concept A: '{concept_a}', Concept B: '{concept_b}'. One sentence.",
        "weight": 1
    },
    {
        "type": "vote",
        "prompt": "Should the swarm prioritize '{proposal}'? Answer ONLY 'YES' or 'NO' followed by a 5-word reason.",
        "weight": 1
    }
]

# Cross-correlation concepts for dreaming
DREAM_CONCEPTS = [
    "neuromorphic hex grid topology", "shannon entropy routing",
    "markov chain state transitions", "jaccard similarity vectors",
    "knowledge graph triple extraction", "cellular automata patterns",
    "fog of war visibility", "quorum voting consensus",
    "performative speech acts", "anti-fragile error handling",
    "semantic embedding distances", "gradient descent optimization",
    "topological homeomorphisms", "lexical vector compilation"
]


def ollama_generate(model, prompt, num_gpu=0, keep_alive="0", num_ctx=2048, timeout=30):
    """Send a prompt to an Ollama model with resource controls."""
    payload = json.dumps({
        "model": model,
        "prompt": prompt,
        "stream": False,
        "options": {
            "num_gpu": num_gpu,
            "num_ctx": num_ctx,
            "temperature": 0.7,
            "top_p": 0.9,
        },
        "keep_alive": keep_alive
    }).encode("utf-8")
    
    req = urllib.request.Request(
        f"{OLLAMA_URL}/api/generate",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    
    start = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            data = json.loads(resp.read().decode())
        latency = time.time() - start
        response = data.get("response", "").strip()
        
        # Calculate Shannon entropy of response
        entropy = compute_entropy(response)
        
        return {
            "success": True,
            "response": response,
            "latency_s": round(latency, 2),
            "entropy": round(entropy, 4),
            "eval_count": data.get("eval_count", 0),
            "eval_duration": data.get("eval_duration", 0),
            "tokens_per_second": round(
                data.get("eval_count", 0) / (data.get("eval_duration", 1) / 1e9)
                if data.get("eval_duration", 0) > 0 else 0, 1
            )
        }
    except Exception as e:
        return {
            "success": False,
            "response": str(e),
            "latency_s": round(time.time() - start, 2),
            "entropy": 0,
            "eval_count": 0,
            "eval_duration": 0,
            "tokens_per_second": 0
        }


def compute_entropy(text):
    """Shannon entropy of text - measures information density."""
    if not text:
        return 0.0
    freq = {}
    for c in text.lower():
        freq[c] = freq.get(c, 0) + 1
    total = len(text)
    entropy = 0.0
    for count in freq.values():
        p = count / total
        if p > 0:
            entropy -= p * math.log2(p)
    return entropy


def jaccard_similarity(set_a, set_b):
    """Jaccard similarity between two sets of tokens."""
    if not set_a or not set_b:
        return 0.0
    intersection = set_a & set_b
    union = set_a | set_b
    return len(intersection) / len(union) if union else 0.0


def extract_performative(text):
    """Extract the performative intent from text using lexical analysis."""
    text_lower = text.lower()
    tokens = set(text_lower.split())
    
    performatives = {
        "ASSERT": {"is", "are", "was", "has", "have", "been", "the", "it"},
        "DIRECT": {"do", "make", "create", "build", "run", "start", "stop", "fix"},
        "COMMIT": {"will", "shall", "going", "promise", "plan", "intend"},
        "DECLARE": {"announce", "declare", "define", "name", "call", "designate"},
        "QUERY": {"what", "how", "why", "when", "where", "which", "who", "?"},
        "EXPRESS": {"feel", "think", "believe", "want", "need", "hope", "wish"},
    }
    
    best_type = "ASSERT"
    best_score = 0.0
    for ptype, keywords in performatives.items():
        score = jaccard_similarity(tokens, keywords)
        if score > best_score:
            best_score = score
            best_type = ptype
    
    return best_type, best_score


def log_event(conn, sender, receiver, payload, status="SUCCESS"):
    """Log an event to the swarm ledger with SHA-256 hash."""
    payload_hash = hashlib.sha256(payload.encode()).hexdigest()[:16]
    conn.execute(
        "INSERT INTO EVENT_LOG (sender, receiver, payload, status) VALUES (?, ?, ?, ?)",
        (sender, receiver, f"[{payload_hash}] {payload[:500]}", status)
    )
    conn.commit()


def update_model_status(conn, model_name, status, latency_ms, entropy):
    """Update model status in the ledger."""
    conn.execute("""
        UPDATE MODEL_STATUS 
        SET status=?, last_latency_ms=?, last_entropy=?, last_active=CURRENT_TIMESTAMP
        WHERE model_name=?
    """, (status, latency_ms, entropy, model_name))
    conn.commit()


def get_model_info(conn, model_name):
    """Get model info from the ledger."""
    row = conn.execute(
        "SELECT * FROM MODEL_STATUS WHERE model_name=?", (model_name,)
    ).fetchone()
    if row:
        return dict(row)
    return None


def get_all_workers(conn):
    """Get all models except queen and embeddings."""
    rows = conn.execute("SELECT model_name, hex_q, hex_r, family, parameter_size FROM MODEL_STATUS").fetchall()
    workers = []
    for row in rows:
        name = row[0]
        if name not in EXCLUDE_MODELS:
            workers.append({
                "name": name, "q": row[1], "r": row[2],
                "family": row[3], "params": row[4]
            })
    return workers


def build_task_prompt(task_template, model_info, all_workers):
    """Build a concrete prompt from a task template."""
    prompt = task_template["prompt"]
    
    # Fill in template variables
    prompt = prompt.replace("{model_name}", model_info.get("name", "unknown"))
    prompt = prompt.replace("{params}", model_info.get("params", "?"))
    prompt = prompt.replace("{q}", str(model_info.get("q", 0)))
    prompt = prompt.replace("{r}", str(model_info.get("r", 0)))
    
    # Generate neighbor list
    q, r = model_info.get("q", 0), model_info.get("r", 0)
    neighbors = []
    for w in all_workers:
        dq = abs(w["q"] - q)
        dr = abs(w["r"] - r)
        if 0 < dq + dr <= 2:
            neighbors.append(f"({w['q']},{w['r']})")
    prompt = prompt.replace("{neighbors}", ", ".join(neighbors[:4]) if neighbors else "(none)")
    
    # Context for code review
    prompt = prompt.replace("{context}", random.choice([
        "Python function that reads JSON and writes to SQLite",
        "Event-driven message passing between concurrent agents",
        "Hex grid coordinate system with axial addressing",
        "SHA-256 hashing for immutable event log integrity",
        "Jaccard similarity for performative intent extraction"
    ]))
    
    # Dream concepts
    if "{concept_a}" in prompt:
        a, b = random.sample(DREAM_CONCEPTS, 2)
        prompt = prompt.replace("{concept_a}", a)
        prompt = prompt.replace("{concept_b}", b)
    
    # Proposals for voting
    prompt = prompt.replace("{proposal}", random.choice([
        "adding a weather system to the hex grid",
        "implementing skill trees for agents",
        "creating an economy with treasury points",
        "building a breeding system for model traits",
        "adding fog-of-war with 1-hop visibility"
    ]))
    
    return prompt


def wake_queen():
    """Ensure the Queen Bee is loaded and GPU-resident."""
    print(f"[QUEEN] Waking Queen Bee: {QUEEN_MODEL}")
    result = ollama_generate(
        QUEEN_MODEL,
        "You are the Queen Bee of the SIMS1337 neuromorphic swarm. Say 'HIVE ONLINE' and nothing else.",
        num_gpu=QUEEN_NUM_GPU,
        keep_alive=QUEEN_KEEP_ALIVE,
        num_ctx=4096,
        timeout=60
    )
    if result["success"]:
        print(f"[QUEEN] Online: {result['response'][:80]} ({result['latency_s']}s, {result['tokens_per_second']} tok/s)")
    else:
        print(f"[QUEEN] Failed to wake: {result['response'][:100]}")
    return result["success"]


def queen_route(user_message):
    """Queen routes a message by extracting its performative and deciding which worker handles it."""
    perf_type, perf_score = extract_performative(user_message)
    
    result = ollama_generate(
        QUEEN_MODEL,
        f"You are the routing queen of a model swarm. A user sent: '{user_message[:200]}'\n"
        f"Performative type detected: {perf_type} (confidence: {perf_score:.2f})\n"
        f"Summarize this into a ONE-SENTENCE task directive for a worker model. Be precise.",
        num_gpu=QUEEN_NUM_GPU,
        keep_alive=QUEEN_KEEP_ALIVE,
        num_ctx=4096,
        timeout=30
    )
    return result, perf_type, perf_score


def queen_summarize(worker_responses):
    """Queen summarizes all worker responses into a coherent output."""
    combined = "\n".join([
        f"[{r['model']}] {r['response'][:150]}" for r in worker_responses if r.get("success")
    ])
    
    result = ollama_generate(
        QUEEN_MODEL,
        f"You are synthesizing responses from {len(worker_responses)} worker models.\n"
        f"Responses:\n{combined[:2000]}\n\n"
        f"Provide a ONE-PARAGRAPH synthesis of the key insights. Be concise.",
        num_gpu=QUEEN_NUM_GPU,
        keep_alive=QUEEN_KEEP_ALIVE,
        num_ctx=4096,
        timeout=30
    )
    return result


def cellular_cycle(conn, workers, cycle_num):
    """
    The Cellular Microphone: cycle through workers one at a time.
    Each worker gets a directed task, responds, then is evicted.
    Queen stays resident and orchestrates.
    """
    # Pick a weighted random task type
    weighted_tasks = []
    for task in SEED_TASKS:
        weighted_tasks.extend([task] * task["weight"])
    
    # Pick workers for this cycle (rotate through all, ~5 per cycle)
    batch_size = min(5, len(workers))
    start_idx = (cycle_num * batch_size) % len(workers)
    batch = workers[start_idx:start_idx + batch_size]
    if len(batch) < batch_size:
        batch += workers[:batch_size - len(batch)]
    
    results = []
    
    for worker in batch:
        task = random.choice(weighted_tasks)
        prompt = build_task_prompt(task, worker, workers)
        
        print(f"  [{worker['name'][:30]}] Task: {task['type']} | ", end="", flush=True)
        
        result = ollama_generate(
            worker["name"],
            prompt,
            num_gpu=WORKER_NUM_GPU,
            keep_alive=WORKER_KEEP_ALIVE,
            num_ctx=WORKER_NUM_CTX,
            timeout=45
        )
        
        if result["success"]:
            print(f"OK ({result['latency_s']}s, {result['tokens_per_second']} tok/s, H={result['entropy']:.2f})")
            
            # Log to ledger
            log_event(conn, worker["name"], "QUEEN",
                f"[{task['type']}] {result['response'][:300]}")
            
            # Update model status
            update_model_status(conn, worker["name"], "ACTIVE",
                round(result["latency_s"] * 1000, 1), result["entropy"])
            
            results.append({
                "model": worker["name"],
                "task": task["type"],
                "response": result["response"],
                "success": True,
                "latency": result["latency_s"],
                "entropy": result["entropy"],
                "tps": result["tokens_per_second"]
            })
        else:
            print(f"FAIL ({result['response'][:60]})")
            update_model_status(conn, worker["name"], "OFFLINE", 0, 0)
            results.append({
                "model": worker["name"],
                "task": task["type"],
                "response": result["response"],
                "success": False
            })
    
    return results


def main():
    print("=" * 60)
    print("SIMS1337 CELLULAR MICROPHONE ENGINE")
    print("=" * 60)
    print(f"Queen Bee: {QUEEN_MODEL}")
    print(f"Worker GPU: {WORKER_NUM_GPU} (mmap from SSD)")
    print(f"Worker keep_alive: {WORKER_KEEP_ALIVE} (evict after use)")
    print(f"Worker context: {WORKER_NUM_CTX}")
    print()
    
    # Wake the queen
    if not wake_queen():
        print("[FATAL] Queen Bee failed to start. Check Ollama.")
        return
    
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    workers = get_all_workers(conn)
    print(f"[SWARM] {len(workers)} worker models ready for cycling")
    print()
    
    cycle = 0
    while True:
        print(f"\n{'='*60}")
        print(f"CYCLE {cycle} | {time.strftime('%H:%M:%S')} | Workers: {len(workers)}")
        print(f"{'='*60}")
        
        # Run cellular cycle
        results = cellular_cycle(conn, workers, cycle)
        
        successes = [r for r in results if r.get("success")]
        
        if successes:
            # Queen summarizes the cycle
            print(f"\n[QUEEN] Synthesizing {len(successes)} responses...")
            summary = queen_summarize(successes)
            if summary["success"]:
                print(f"[QUEEN SYNTHESIS] {summary['response'][:200]}")
                log_event(conn, "QUEEN", "SWARM",
                    f"[SYNTHESIS cycle={cycle}] {summary['response'][:400]}")
            
            # Stats
            avg_latency = sum(r["latency"] for r in successes) / len(successes)
            avg_entropy = sum(r["entropy"] for r in successes) / len(successes)
            avg_tps = sum(r["tps"] for r in successes) / len(successes)
            print(f"\n[STATS] Avg latency: {avg_latency:.1f}s | Avg entropy: {avg_entropy:.2f} | Avg TPS: {avg_tps:.1f}")
        
        cycle += 1
        
        # Pace: wait between cycles to avoid overloading
        wait = 10  # seconds between cycles
        print(f"[PACE] Next cycle in {wait}s...")
        time.sleep(wait)


if __name__ == "__main__":
    main()
