"""Quick test of the Cellular Microphone Engine components."""
import sys
sys.path.insert(0, ".")
from cellular_microphone import *

print("=== CELLULAR MICROPHONE QUICK TEST ===")
print()

# Test 1: Wake Queen
print("[TEST 1] Waking Queen Bee...")
result = ollama_generate(
    QUEEN_MODEL,
    "Say HIVE ONLINE and nothing else.",
    num_gpu=QUEEN_NUM_GPU,
    keep_alive=QUEEN_KEEP_ALIVE,
    num_ctx=4096,
    timeout=60
)
print(f"  Queen: {result['success']} | {result['response'][:80]} | {result['latency_s']}s | {result['tokens_per_second']} tok/s")
print()

# Test 2: Worker via mmap
print("[TEST 2] Testing mmap worker (qwen2.5:0.5b, CPU-only)...")
result2 = ollama_generate(
    "qwen2.5:0.5b",
    "You are a grid agent. Say OK and your model name. Nothing else.",
    num_gpu=0,
    keep_alive="0",
    num_ctx=2048,
    timeout=30
)
print(f"  Worker: {result2['success']} | {result2['response'][:80]} | {result2['latency_s']}s | {result2['tokens_per_second']} tok/s")
print()

# Test 3: Performative extraction
print("[TEST 3] Performative extraction...")
tests = [
    "What is the status of the swarm?",
    "Create a new agent node",
    "I think the grid should be bigger",
    "Run diagnostics on all models"
]
for t in tests:
    ptype, score = extract_performative(t)
    print(f'  "{t[:40]}" -> {ptype} ({score:.2f})')
print()

# Test 4: Shannon entropy
print("[TEST 4] Shannon entropy...")
print(f"  High entropy (random): {compute_entropy('asjkdhf2398fhakjsd'):.2f}")
print(f"  Low entropy (repeat):  {compute_entropy('aaaaaaaaaa'):.2f}")
print(f"  Medium (english):      {compute_entropy('the quick brown fox jumps over the lazy dog'):.2f}")
print()

# Test 5: Jaccard similarity
print("[TEST 5] Jaccard similarity...")
print(f"  Identical: {jaccard_similarity({'a','b','c'}, {'a','b','c'}):.2f}")
print(f"  Half:      {jaccard_similarity({'a','b','c','d'}, {'a','b','e','f'}):.2f}")
print(f"  None:      {jaccard_similarity({'a','b'}, {'c','d'}):.2f}")
print()

print("=== ALL TESTS COMPLETE ===")
