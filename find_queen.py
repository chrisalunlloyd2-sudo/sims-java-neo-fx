"""Find the best Queen Bee model - test all small abliterated + standard models."""
import urllib.request
import json
import time

OLLAMA = "http://localhost:11434"

def test_model(name, timeout=60):
    body = json.dumps({
        "model": name,
        "prompt": "Say HIVE ONLINE",
        "stream": False,
        "options": {"num_ctx": 2048}
    }).encode()
    req = urllib.request.Request(
        f"{OLLAMA}/api/generate", data=body,
        headers={"Content-Type": "application/json"}
    )
    start = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            d = json.loads(resp.read())
        lat = time.time() - start
        tps = d.get("eval_count", 0) / (d.get("eval_duration", 1) / 1e9) if d.get("eval_duration", 0) > 0 else 0
        return True, d.get("response", "")[:60], round(lat, 1), round(tps, 1)
    except Exception as e:
        return False, str(e)[:60], round(time.time() - start, 1), 0

# Check what's loaded
print("=== CURRENTLY LOADED ===")
try:
    r = urllib.request.urlopen(f"{OLLAMA}/api/ps", timeout=5)
    ps = json.loads(r.read())
    for m in ps.get("models", []):
        print(f"  {m['name']:45} VRAM: {m.get('size_vram',0)//1024//1024}MB  RAM: {m.get('size',0)//1024//1024}MB")
except:
    print("  Could not query /api/ps")

# Test candidates for Queen Bee
print("\n=== QUEEN BEE CANDIDATES ===")
candidates = [
    "qwen2.5:0.5b",
    "tinyllama:1.1b", 
    "gemma2:2b",
    "qwen2.5:1.5b",
    "qwen2.5:3b",
    "aegis-gemma2-abliterated:2b-q8",
    "dagbs/qwen2.5-coder-3b-instruct-abliterated:q8_0",
]

for c in candidates:
    print(f"  Testing {c:50} ... ", end="", flush=True)
    ok, resp, lat, tps = test_model(c, timeout=90)
    if ok:
        print(f"OK  {lat}s  {tps} tok/s  '{resp}'")
    else:
        print(f"FAIL  {resp}")

print("\n=== RECOMMENDATION ===")
print("Pick the fastest abliterated model that succeeded as Queen Bee")
