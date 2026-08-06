"""
SIMS1337 - Phase 1: Database Foundation
Creates all required tables and seeds with real Ollama model data.
"""
import sqlite3
import hashlib
import json
import os
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(SCRIPT_DIR, "swarm_ledger.db")

def get_hex_coord(name, index):
    """Deterministic hex coordinate from model name hash."""
    h = int(hashlib.md5(name.encode()).hexdigest()[:8], 16)
    radius = 4
    q = (h % (2 * radius + 1)) - radius
    r_min = max(-radius, -q - radius)
    r_max = min(radius, -q + radius)
    r = r_min + (h // 7) % (r_max - r_min + 1) if r_max > r_min else r_min
    return q, r

def create_schema(conn):
    c = conn.cursor()
    
    # Core event log - the backbone of everything
    c.execute("""
        CREATE TABLE IF NOT EXISTS EVENT_LOG (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            sender TEXT NOT NULL,
            receiver TEXT NOT NULL,
            payload TEXT,
            status TEXT DEFAULT 'SUCCESS'
        )
    """)
    
    # Model registry with hex positions
    c.execute("""
        CREATE TABLE IF NOT EXISTS MODEL_STATUS (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            model_name TEXT UNIQUE NOT NULL,
            family TEXT,
            parameter_size TEXT,
            quant_level TEXT,
            hex_q INTEGER DEFAULT 0,
            hex_r INTEGER DEFAULT 0,
            status TEXT DEFAULT 'IDLE',
            last_latency_ms REAL DEFAULT 0,
            last_entropy REAL DEFAULT 0,
            last_active DATETIME DEFAULT CURRENT_TIMESTAMP,
            context_length INTEGER DEFAULT 0,
            embedding_length INTEGER DEFAULT 0
        )
    """)
    
    # Agent position history for 4D time tracking
    c.execute("""
        CREATE TABLE IF NOT EXISTS AGENT_POSITIONS (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            model_name TEXT NOT NULL,
            hex_q INTEGER,
            hex_r INTEGER,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            action TEXT
        )
    """)
    
    conn.commit()
    print("[SCHEMA] All tables created: EVENT_LOG, MODEL_STATUS, AGENT_POSITIONS")

def seed_from_ollama(conn):
    """Query real Ollama instance and seed MODEL_STATUS with actual installed models."""
    c = conn.cursor()
    
    try:
        req = urllib.request.Request("http://localhost:11434/api/tags")
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
    except Exception as e:
        print(f"[SEED] Ollama unreachable: {e}")
        return 0
    
    models = data.get("models", [])
    count = 0
    
    for i, m in enumerate(models):
        name = m.get("name", "unknown")
        details = m.get("details", {})
        family = details.get("family", "unknown")
        param_size = details.get("parameter_size", "?")
        quant = details.get("quantization_level", "?")
        ctx_len = details.get("context_length", 0)
        emb_len = details.get("embedding_length", 0)
        
        q, r = get_hex_coord(name, i)
        
        try:
            c.execute("""
                INSERT OR REPLACE INTO MODEL_STATUS 
                (model_name, family, parameter_size, quant_level, hex_q, hex_r, status, context_length, embedding_length)
                VALUES (?, ?, ?, ?, ?, ?, 'IDLE', ?, ?)
            """, (name, family, param_size, quant, q, r, ctx_len, emb_len))
            
            # Log initial position
            c.execute("""
                INSERT INTO AGENT_POSITIONS (model_name, hex_q, hex_r, action)
                VALUES (?, ?, ?, 'SPAWNED')
            """, (name, q, r))
            
            count += 1
        except Exception as e:
            print(f"[SEED] Error inserting {name}: {e}")
    
    # Seed a welcome event
    c.execute("""
        INSERT INTO EVENT_LOG (sender, receiver, payload, status)
        VALUES ('SYSTEM', 'SWARM', 'SIMS1337 initialized. All models seeded.', 'SUCCESS')
    """)
    
    conn.commit()
    print(f"[SEED] Inserted {count} real Ollama models into MODEL_STATUS")
    return count

def verify(conn):
    c = conn.cursor()
    
    tables = c.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()
    print(f"\n[VERIFY] Tables: {[t[0] for t in tables]}")
    
    event_count = c.execute("SELECT COUNT(*) FROM EVENT_LOG").fetchone()[0]
    print(f"[VERIFY] EVENT_LOG rows: {event_count}")
    
    model_count = c.execute("SELECT COUNT(*) FROM MODEL_STATUS").fetchone()[0]
    print(f"[VERIFY] MODEL_STATUS rows: {model_count}")
    
    print("\n[VERIFY] Model positions:")
    for row in c.execute("SELECT model_name, hex_q, hex_r, status FROM MODEL_STATUS"):
        print(f"  {row[0]:50s} -> hex({row[1]:+d}, {row[2]:+d})  [{row[3]}]")
    
    pos_count = c.execute("SELECT COUNT(*) FROM AGENT_POSITIONS").fetchone()[0]
    print(f"\n[VERIFY] AGENT_POSITIONS rows: {pos_count}")
    
    return event_count > 0 and model_count > 0

if __name__ == "__main__":
    print(f"[DB] Using: {DB_PATH}")
    conn = sqlite3.connect(DB_PATH)
    create_schema(conn)
    seed_from_ollama(conn)
    ok = verify(conn)
    conn.close()
    
    if ok:
        print("\n✅ PHASE 1 STEP 1-2 PASSED: Database foundation is solid.")
    else:
        print("\n❌ PHASE 1 STEP 1-2 FAILED: Check errors above.")
