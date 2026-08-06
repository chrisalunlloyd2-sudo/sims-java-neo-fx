"""
SIMS1337 - Integration Test Suite
Tests the full data pipeline: DB -> Bridge -> JSON -> Web API
"""
import sqlite3
import json
import os
import urllib.request
import time
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(ROOT, "swarm_ledger.db")
STATE_PATH = os.path.join(ROOT, "gui_state.json")
PASSED = 0
FAILED = 0

def test(name, condition, detail=""):
    global PASSED, FAILED
    if condition:
        PASSED += 1
        print(f"  PASS: {name}")
    else:
        FAILED += 1
        print(f"  FAIL: {name} -- {detail}")

def test_db_schema():
    print("\n[TEST 1] Database Schema")
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    tables = [t[0] for t in c.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
    test("EVENT_LOG table exists", "EVENT_LOG" in tables, f"Found: {tables}")
    test("MODEL_STATUS table exists", "MODEL_STATUS" in tables, f"Found: {tables}")
    test("AGENT_POSITIONS table exists", "AGENT_POSITIONS" in tables, f"Found: {tables}")
    
    # Check EVENT_LOG columns
    cols = [r[1] for r in c.execute("PRAGMA table_info(EVENT_LOG)").fetchall()]
    test("EVENT_LOG has 'sender' column", "sender" in cols, f"Cols: {cols}")
    test("EVENT_LOG has 'payload' column", "payload" in cols, f"Cols: {cols}")
    
    # Check MODEL_STATUS columns
    cols = [r[1] for r in c.execute("PRAGMA table_info(MODEL_STATUS)").fetchall()]
    test("MODEL_STATUS has 'hex_q' column", "hex_q" in cols, f"Cols: {cols}")
    test("MODEL_STATUS has 'hex_r' column", "hex_r" in cols, f"Cols: {cols}")
    test("MODEL_STATUS has 'last_entropy' column", "last_entropy" in cols, f"Cols: {cols}")
    conn.close()

def test_db_data():
    print("\n[TEST 2] Database Content")
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    model_count = c.execute("SELECT COUNT(*) FROM MODEL_STATUS").fetchone()[0]
    test("MODEL_STATUS has models", model_count > 0, f"Count: {model_count}")
    test("MODEL_STATUS has 30+ models", model_count >= 30, f"Count: {model_count}")
    
    event_count = c.execute("SELECT COUNT(*) FROM EVENT_LOG").fetchone()[0]
    test("EVENT_LOG has events", event_count > 0, f"Count: {event_count}")
    
    pos_count = c.execute("SELECT COUNT(*) FROM AGENT_POSITIONS").fetchone()[0]
    test("AGENT_POSITIONS has entries", pos_count > 0, f"Count: {pos_count}")
    
    # Check hex positions are within valid range
    bad = c.execute("SELECT COUNT(*) FROM MODEL_STATUS WHERE hex_q < -4 OR hex_q > 4 OR hex_r < -4 OR hex_r > 4").fetchone()[0]
    test("All hex positions in range [-4,4]", bad == 0, f"Out of range: {bad}")
    
    # Check unique model names
    dupes = c.execute("SELECT model_name, COUNT(*) as cnt FROM MODEL_STATUS GROUP BY model_name HAVING cnt > 1").fetchall()
    test("No duplicate model names", len(dupes) == 0, f"Dupes: {dupes}")
    conn.close()

def test_bridge_json():
    print("\n[TEST 3] Bridge JSON Output")
    test("gui_state.json exists", os.path.exists(STATE_PATH), f"Path: {STATE_PATH}")
    
    if not os.path.exists(STATE_PATH):
        test("gui_state.json is valid JSON", False, "File missing")
        return
    
    with open(STATE_PATH) as f:
        raw = f.read()
    
    try:
        data = json.loads(raw)
        test("gui_state.json is valid JSON", True)
    except:
        test("gui_state.json is valid JSON", False, "Parse error")
        return
    
    test("JSON has 'models' key", "models" in data, f"Keys: {list(data.keys())}")
    test("JSON has 'chats' key", "chats" in data, f"Keys: {list(data.keys())}")
    test("JSON has 'model_count' key", "model_count" in data)
    test("model_count > 0", data.get("model_count", 0) > 0, f"Count: {data.get('model_count')}")
    
    if "models" in data and len(data["models"]) > 0:
        m = data["models"][0]
        test("Model has 'name' field", "name" in m, f"Keys: {list(m.keys())}")
        test("Model has 'q' field", "q" in m)
        test("Model has 'r' field", "r" in m)
        test("Model has 'status' field", "status" in m)
        test("Model has 'family' field", "family" in m)
        test("Model has 'params' field", "params" in m)

def test_ollama():
    print("\n[TEST 4] Ollama Connectivity")
    try:
        req = urllib.request.Request("http://localhost:11434/api/tags")
        with urllib.request.urlopen(req, timeout=5) as resp:
            data = json.loads(resp.read().decode())
        models = data.get("models", [])
        test("Ollama is reachable", True)
        test("Ollama has models installed", len(models) > 0, f"Count: {len(models)}")
        test("Ollama has 10+ models", len(models) >= 10, f"Count: {len(models)}")
    except Exception as e:
        test("Ollama is reachable", False, str(e))

def test_web_api():
    print("\n[TEST 5] Web API (Port 1337)")
    try:
        req = urllib.request.Request("http://localhost:1337/api/status")
        with urllib.request.urlopen(req, timeout=5) as resp:
            raw = resp.read().decode()
            data = json.loads(raw)
        test("Port 1337 /api/status responds", True)
        test("API returns model_count", "model_count" in data, f"Keys: {list(data.keys())}")
        test("API returns models array", "models" in data and len(data["models"]) > 0)
    except Exception as e:
        test("Port 1337 /api/status responds", False, str(e))
    
    try:
        req = urllib.request.Request("http://localhost:1337/")
        with urllib.request.urlopen(req, timeout=5) as resp:
            html = resp.read().decode()
        test("Port 1337 / serves HTML", "<!DOCTYPE html>" in html or "<html" in html)
        test("HTML contains canvas", "canvas" in html.lower())
        test("HTML contains SIMS1337", "SIMS1337" in html or "sims1337" in html.lower())
    except Exception as e:
        test("Port 1337 / serves HTML", False, str(e))

def test_chat_insert():
    print("\n[TEST 6] Chat Insert Pipeline")
    conn = sqlite3.connect(DB_PATH)
    before = conn.execute("SELECT COUNT(*) FROM EVENT_LOG").fetchone()[0]
    conn.execute("INSERT INTO EVENT_LOG (sender, receiver, payload, status) VALUES ('TEST', 'TEST', 'Integration test message', 'SUCCESS')")
    conn.commit()
    after = conn.execute("SELECT COUNT(*) FROM EVENT_LOG").fetchone()[0]
    test("Can insert into EVENT_LOG", after == before + 1, f"Before: {before}, After: {after}")
    
    last = conn.execute("SELECT payload FROM EVENT_LOG ORDER BY id DESC LIMIT 1").fetchone()[0]
    test("Last event is our test message", last == "Integration test message", f"Got: {last}")
    conn.close()

if __name__ == "__main__":
    print("=" * 50)
    print("SIMS1337 INTEGRATION TEST SUITE")
    print("=" * 50)
    
    test_db_schema()
    test_db_data()
    test_bridge_json()
    test_ollama()
    test_web_api()
    test_chat_insert()
    
    print("\n" + "=" * 50)
    print(f"RESULTS: {PASSED} passed, {FAILED} failed, {PASSED + FAILED} total")
    print("=" * 50)
    
    sys.exit(0 if FAILED == 0 else 1)
