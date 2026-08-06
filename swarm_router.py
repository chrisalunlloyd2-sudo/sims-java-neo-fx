import zmq
import sqlite3
import json
import time
import hashlib
from datetime import datetime, timezone

class SwarmLedger:
    def __init__(self, db_path="swarm_ledger.db"):
        self.conn = sqlite3.connect(db_path)
        self._migrate()

    def _migrate(self):
        self.conn.executescript("""
            CREATE TABLE IF NOT EXISTS EVENT_LOG (
                event_id TEXT PRIMARY KEY,
                timestamp DATETIME,
                sender TEXT,
                receiver TEXT,
                payload_hash TEXT,
                payload TEXT,
                latency_ms INTEGER,
                status TEXT
            );
            CREATE TABLE IF NOT EXISTS MODEL_HEALTH (
                model_name TEXT PRIMARY KEY,
                safety_score REAL,
                total_requests INTEGER,
                failed_requests INTEGER,
                circuit_breaker_status TEXT,
                last_updated DATETIME
            );
        """)

    def log_event(self, sender, receiver, payload, latency_ms, status):
        payload_str = json.dumps(payload)
        payload_hash = hashlib.sha256(payload_str.encode()).hexdigest()
        event_id = f"EVT_{int(time.time()*1000)}_{payload_hash[:8]}"
        
        self.conn.execute(
            "INSERT INTO EVENT_LOG VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (event_id, datetime.now(timezone.utc).isoformat(), sender, receiver, payload_hash, payload_str, latency_ms, status)
        )
        self.conn.commit()

    def update_health(self, model_name, is_success):
        # Basic Circuit Breaker & Safety Scoring Logic
        self.conn.execute(
            "INSERT OR IGNORE INTO MODEL_HEALTH VALUES (?, 1.0, 0, 0, 'CLOSED', ?)",
            (model_name, datetime.now(timezone.utc).isoformat())
        )
        
        if is_success:
            self.conn.execute("UPDATE MODEL_HEALTH SET total_requests = total_requests + 1, safety_score = MIN(1.0, safety_score + 0.01) WHERE model_name = ?", (model_name,))
        else:
            self.conn.execute("UPDATE MODEL_HEALTH SET total_requests = total_requests + 1, failed_requests = failed_requests + 1, safety_score = MAX(0.0, safety_score - 0.1) WHERE model_name = ?", (model_name,))
            
        # Trip the circuit breaker if safety score drops below 0.5
        self.conn.execute("UPDATE MODEL_HEALTH SET circuit_breaker_status = 'OPEN' WHERE model_name = ? AND safety_score < 0.5", (model_name,))
        self.conn.commit()

class SwarmRouter:
    def __init__(self):
        self.context = zmq.Context()
        self.router = self.context.socket(zmq.ROUTER)
        self.router.bind("tcp://*:5555")
        self.ledger = SwarmLedger()
        print("[ROUTER] Neuromorphic Swarm Router Active on port 5555")

    def run(self):
        while True:
            # Wait for next request from client or agent
            ident, empty, msg = self.router.recv_multipart()
            packet = json.loads(msg.decode())
            
            # TODO: Implement dynamic routing based on ledger health
            # For now, echo back to simulate processing
            print(f"[ROUTER] Received from {ident.decode()}: {packet}")
            self.ledger.log_event(ident.decode(), "ROUTER", packet, 0, "RECEIVED")
            
            # Acknowledge
            response = json.dumps({"status": "ACK", "router_time": time.time()})
            self.router.send_multipart([ident, b"", response.encode()])

if __name__ == "__main__":
    router = SwarmRouter()
    router.run()
