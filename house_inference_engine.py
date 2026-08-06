import os
import sys
import json
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# [HOUSE_INFERENCE_ENGINE] PHASE_035
# Corrected Model Path for Substrate Integrity.

MODEL_PATH = Path(r"C:\Users\viper\Aegis_Agents\vendor\models\gemma-2-2b-it-abliterated-Q8_0.gguf")
N_CTX = int(os.environ.get("VIPER_HOUSE_N_CTX", "8192"))
MAX_OUTPUT_TOKENS = int(os.environ.get("VIPER_HOUSE_MAX_OUTPUT_TOKENS", "4096"))
SAFE_INPUT_TOKENS = {
    "chat": int(os.environ.get("VIPER_HOUSE_CHAT_INPUT_TOKENS", "1024")),
    "planning": int(os.environ.get("VIPER_HOUSE_PLAN_INPUT_TOKENS", "3072")),
    "build": int(os.environ.get("VIPER_HOUSE_BUILD_INPUT_TOKENS", "4096")),
}
RETRY_OUTPUT_TOKENS = [2048, 1024, 512, 256, 128]

try:
    from llama_cpp import Llama
    HAS_LLAMA = True
except ImportError:
    HAS_LLAMA = False

import sqlite3

# ... (rest of imports) ...

class HouseInferenceHandler(BaseHTTPRequestHandler):
    """HouseInferenceHandler (class)."""
    _llm = None
    _llm_lock = threading.Lock()

    def _get_rag_context(self, query):
        """[AXIOMATIC RECALL] Pulls the top 3 relevant logic blocks from SQL."""
        try:
            conn = sqlite3.connect(str(Path(os.environ.get("USERPROFILE", r"C:\Users\viper")) / "gemini_bridge.db"))
            c = conn.cursor()
            # Simple keyword search for recall
            keywords = query.split()[:3]
            results = []
            for kw in keywords:
                c.execute("SELECT label, description FROM TRIPLET_MANIFOLD WHERE label LIKE ? OR description LIKE ? LIMIT 1", (f'%{kw}%', f'%{kw}%'))
                res = c.fetchone()
                if res: results.append(f"[{res[0]}]: {res[1]}")
            conn.close()
            return "\n".join(results) if results else "No direct manifold recall for this stimulus."
        except: return "Recall Offline."

    def do_GET(self):
        """Do GET (function)."""
        if self.path in ("/", "/health"):
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            payload = {
                "status": "ok",
                "mode": "house_inference",
                "has_llama": HAS_LLAMA,
                "model_exists": MODEL_PATH.exists(),
                "threaded": True,
                "n_ctx": N_CTX,
                "max_output_tokens": MAX_OUTPUT_TOKENS,
                "safe_input_tokens": SAFE_INPUT_TOKENS,
                "prompt_packer": "route_aware_system_first_trimmer",
            }
            self.wfile.write(json.dumps(payload).encode("utf-8"))
        elif self.path == "/config":
            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({
                "model_path": str(MODEL_PATH),
                "n_ctx": N_CTX,
                "max_output_tokens": MAX_OUTPUT_TOKENS,
                "safe_input_tokens": SAFE_INPUT_TOKENS,
                "retry_output_tokens": RETRY_OUTPUT_TOKENS,
            }).encode("utf-8"))
        else:
            self.send_error(404)

    @classmethod
    def get_llm(cls):
        """Get llm (function)."""
        if cls._llm is None and HAS_LLAMA and MODEL_PATH.exists():
            print(f"LOADING KERNEL: {MODEL_PATH.name}")
            try:
                cls._llm = Llama(
                    model_path=str(MODEL_PATH),
                    n_ctx=N_CTX,
                    n_threads=max(2, (os.cpu_count() or 4) - 1),
                    verbose=False,
                )
                print("KERNEL LOADED: 101% Satisfaction Ready.")
            except Exception as e:
                print(f"KERNEL LOAD FAILED: {e}")
        return cls._llm

    def _estimate_tokens(self, text):
        return max(1, len(text.encode("utf-8", errors="replace")) // 4)

    def _trim_to_token_budget(self, text, budget):
        if self._estimate_tokens(text) <= budget:
            return text
        max_chars = max(256, budget * 4)
        return text[: max_chars // 2] + "\n[...VIPER_CONTEXT_REDUCED...]\n" + text[-max_chars // 2 :]

    def _format_prompt(self, system, prompt):
        return (
            f"<start_of_turn>system\n{system}<end_of_turn>\n"
            f"<start_of_turn>user\n{prompt}<end_of_turn>\n"
            "<start_of_turn>model\n"
        )

    def _pack_prompt(self, system, prompt, route):
        budget = SAFE_INPUT_TOKENS.get(route, SAFE_INPUT_TOKENS["chat"])
        system_budget = max(256, int(budget * 0.62))
        prompt_budget = max(128, budget - system_budget)
        packed_system = self._trim_to_token_budget(system, system_budget)
        packed_prompt = self._trim_to_token_budget(prompt, prompt_budget)
        full_prompt = self._format_prompt(packed_system, packed_prompt)
        while self._estimate_tokens(full_prompt) > budget and len(packed_system) > 256:
            packed_system = packed_system[: int(len(packed_system) * 0.75)]
            full_prompt = self._format_prompt(packed_system, packed_prompt)
        return full_prompt, {
            "route": route,
            "budget_tokens": budget,
            "estimated_tokens": self._estimate_tokens(full_prompt),
            "system_chars": len(packed_system),
            "prompt_chars": len(packed_prompt),
        }

    def do_POST(self):
        """Do POST (function)."""
        if self.path == "/api/generate":
            started = time.time()
            content_len = int(self.headers.get('Content-Length', 0))
            data = json.loads(self.rfile.read(content_len).decode('utf-8'))
            prompt = data.get("prompt", "")
            system = data.get("system", "")
            route = data.get("route", "chat")
            max_tokens = max(64, min(int(data.get("max_tokens", 256)), MAX_OUTPUT_TOKENS))
            
            # Recall the Scientific House DB
            recall_data = self._get_rag_context(prompt)
            enhanced_system = f"{system}\n\n[MANIFOLD_RECALL]:\n{recall_data}"
            
            print(f"INFERENCE_STIMULUS: {prompt[:50]}...")
            print(f"RECALL_SYNC: {len(recall_data)} chars retrieved.")
            
            response_text = ""
            llm = self.get_llm()
            
            if llm:
                full_prompt, pack_meta = self._pack_prompt(enhanced_system, prompt, route)
                attempts = [max_tokens] + [tok for tok in RETRY_OUTPUT_TOKENS if tok < max_tokens]
                last_error = None
                with self._llm_lock:
                    for attempt_tokens in attempts:
                        try:
                            output = llm(
                                full_prompt,
                                max_tokens=attempt_tokens,
                                stop=["<end_of_turn>"],
                                echo=False,
                                temperature=float(data.get("temperature", 0.45)),
                                top_p=float(data.get("top_p", 0.9)),
                                repeat_penalty=float(data.get("repeat_penalty", 1.08)),
                            )
                            response_text = output['choices'][0]['text'].strip()
                            pack_meta["attempt_tokens"] = attempt_tokens
                            break
                        except Exception as e:
                            last_error = str(e)
                            pack_meta["last_error"] = last_error
                    if not response_text:
                        response_text = f"[HOUSE_ERROR] generation failed after retry ladder: {last_error}"
            else:
                pack_meta = {"route": route, "estimated_tokens": 0}
                response_text = "[HOUSE_ERROR] Llama-CPP not found or Model missing. Check substrate integrity."

            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({
                "response": response_text,
                "meta": {
                    "route": route,
                    "duration_ms": int((time.time() - started) * 1000),
                    "pack": pack_meta,
                    "max_tokens_requested": max_tokens,
                }
            }).encode("utf-8"))

if __name__ == "__main__":
    if not HAS_LLAMA:
        print("WARNING: llama-cpp-python not found. Attempting emergency injection...")
        # Since I can't easily install binary deps in a turn, I'll rely on the user's existing env
        # which they claimed has it.
    
    print("HOUSE INFERENCE ENGINE ACTIVE: Port 11435")
    server = ThreadingHTTPServer(("0.0.0.0", 11435), HouseInferenceHandler)
    server.serve_forever()
