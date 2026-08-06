import sqlite3

# The definitive 22 Nodes of SIMS1337
SIMS1337_NODES = [
    ("Hospital", "qwen2.5:0.5b", "Diagnostics and error recovery"),
    ("Brute Foundry", "codellama:7b", "Raw tool generation and script compilation"),
    ("Knowledge Graph", "phi3:mini", "Vector mapping and concept relationships"),
    ("Server Orchestration", "gemma2:2b", "Load balancing and ZMQ backpressure"),
    ("Self-Exploration", "deepseek-r1:1.5b", "Autonomous hypothesis generation"),
    ("Error Logging", "tinyllama:1.1b", "High-speed log parsing"),
    ("Design", "qwen2.5:0.5b", "UI/UX component generation"),
    ("Real RAG", "phi3:mini", "TimescaleDB retrieval and context injection"),
    ("Fine-Tuning", "llama3.2:1b", "Dataset generation and LoRA prep"),
    ("Multi-Agent Topology", "gemma2:2b", "Swarm routing map updates"),
    ("Web Dashboard", "qwen2.5:0.5b", "Fast API telemetry endpoints"),
    ("Plugin System", "codellama:7b", "Dynamic module importing"),
    ("Perfect Prompts", "phi:latest", "Prompt optimization and compression"),
    ("Map Guidance", "tinyllama:1.1b", "Navigation and execution tracing"),
    ("Perfect Patterns", "deepseek-r1:1.5b", "Architectural anti-pattern detection"),
    ("Tools System", "llama3.2:1b", "Tool parameter routing"),
    ("Persistent Memory", "phi3:mini", "SQLite to Timescale synchronization"),
    ("FOW — Fog of War", "gemma2:2b", "Gist sync and external context masking"),
    ("Hex TODO System", "tinyllama:1.1b", "Task queue parsing and prioritization"),
    ("Gist Context", "qwen2.5:0.5b", "GitHub Gist payload construction"),
    ("Gist Sync", "llama3.2:1b", "Bidirectional Git state replication"),
    ("Night Cycle", "deepseek-r1:1.5b", "Dream phase cross-correlation")
]

def initialize_22_nodes():
    """Merges the 22 SIMS1337 Java Nodes into the Python Swarm Ledger as Virtual Stations."""
    conn = sqlite3.connect("swarm_ledger.db")
    
    # Create the stations table if it doesn't exist
    conn.executescript("""
        CREATE TABLE IF NOT EXISTS VIRTUAL_STATIONS (
            station_id INTEGER PRIMARY KEY AUTOINCREMENT,
            station_name TEXT UNIQUE,
            assigned_slm TEXT,
            description TEXT,
            status TEXT DEFAULT 'ONLINE'
        );
    """)
    
    print("Integrating 22 SIMS1337 Stations into the Neuromorphic Grid...")
    for name, slm, desc in SIMS1337_NODES:
        conn.execute(
            "INSERT OR IGNORE INTO VIRTUAL_STATIONS (station_name, assigned_slm, description) VALUES (?, ?, ?)",
            (name, slm, desc)
        )
    conn.commit()
    conn.close()
    print("All 22 SIMS1337 Nodes successfully registered into the ZMQ Swarm Router.")

if __name__ == "__main__":
    initialize_22_nodes()
