import torch
import math
from transformers import AutoModelForCausalLM, AutoTokenizer
from peft import LoraConfig, get_peft_model
import numpy as np
import zmq
import json
import sqlite3

class RealSLMNode:
    def __init__(self, model_id="Qwen/Qwen2.5-0.5B"):
        print(f"[REAL SLM NODE] Initializing {model_id}...")
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        
        # 1. Base Model & Tokenizer
        print("[REAL SLM NODE] Loading Base Model & KV Cache...")
        self.tokenizer = AutoTokenizer.from_pretrained(model_id, trust_remote_code=True)
        self.model = AutoModelForCausalLM.from_pretrained(model_id, trust_remote_code=True).to(self.device)
        
        # 2. LoRA Adapter Injection
        print("[REAL SLM NODE] Injecting LoRA Adapters (r=8, alpha=32)...")
        lora_config = LoraConfig(
            r=8, 
            lora_alpha=32, 
            target_modules=["q_proj", "v_proj"], 
            lora_dropout=0.05,
            bias="none",
            task_type="CAUSAL_LM"
        )
        self.model = get_peft_model(self.model, lora_config)
        
        # 3. Knowledge Graph (KG) & KV Store
        print("[REAL SLM NODE] Initializing KG Nodes and Vector Embeddings...")
        self.kv_store = {}
        self.kg_edges = []
        
        # 4. ZMQ Swarm Hook
        self.context = zmq.Context()
        self.socket = self.context.socket(zmq.REP)
        self.socket.bind("tcp://127.0.0.1:5557")
        print("[REAL SLM NODE] Bound to Swarm Bus (tcp://127.0.0.1:5557). Listening...")

    def compute_shannon_entropy(self, logits):
        """Calculates Shannon Entropy for token generation uncertainty."""
        probs = torch.nn.functional.softmax(logits, dim=-1)
        log_probs = torch.nn.functional.log_softmax(logits, dim=-1)
        entropy = -torch.sum(probs * log_probs, dim=-1)
        return entropy.mean().item()

    def generate_with_rag(self, prompt):
        """Generates text using Markov Chain mechanics (next-token prediction) and RAG KV cache."""
        # 1. RAG Lookup
        context = self.kv_store.get("latest_memory", "No external context found.")
        augmented_prompt = f"Context: {context}\nCommand: {prompt}"
        
        inputs = self.tokenizer(augmented_prompt, return_tensors="pt").to(self.device)
        
        # 2. Generation with KV Caching
        with torch.no_grad():
            outputs = self.model.generate(
                **inputs, 
                max_new_tokens=50, 
                use_cache=True, 
                return_dict_in_generate=True, 
                output_scores=True
            )
            
        # 3. Markov / Shannon Entropy Analysis
        response_text = self.tokenizer.decode(outputs.sequences[0], skip_special_tokens=True)
        
        # Extract logits from the first generated token
        first_token_logits = outputs.scores[0]
        entropy = self.compute_shannon_entropy(first_token_logits)
        
        return response_text, entropy

    def run_node(self):
        while True:
            message = self.socket.recv_string()
            print(f"[RECV] {message}")
            
            # Simple RAG store update if message contains "STORE:"
            if "STORE:" in message:
                self.kv_store["latest_memory"] = message.split("STORE:")[1].strip()
                self.socket.send_string("KV Store Updated.")
                continue
                
            response, entropy = self.generate_with_rag(message)
            
            # Log to DB
            try:
                import os
                db_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "swarm_ledger.db")
                conn = sqlite3.connect(db_path)
                c = conn.cursor()
                c.execute("INSERT INTO EVENT_LOG (timestamp, sender, receiver, payload, status) VALUES (CURRENT_TIMESTAMP, 'QWEN_LORA', 'SWARM', ?, ?)", 
                          (response, f"Entropy: {entropy:.4f}"))
                conn.commit()
                conn.close()
            except Exception as e: print(f"DB Error: {e}")
            
            reply = json.dumps({"response": response, "shannon_entropy": entropy})
            self.socket.send_string(reply)

if __name__ == "__main__":
    node = RealSLMNode("Qwen/Qwen2.5-0.5B")
    node.run_node()
