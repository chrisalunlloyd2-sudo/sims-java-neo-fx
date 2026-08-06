import zmq
import json
import time
import requests

class SwarmAgent:
    def __init__(self, agent_id, model_name, router_address="tcp://localhost:5555"):
        self.agent_id = agent_id
        self.model_name = model_name
        self.context = zmq.Context()
        self.dealer = self.context.socket(zmq.DEALER)
        self.dealer.setsockopt_string(zmq.IDENTITY, self.agent_id)
        self.dealer.connect(router_address)
        print(f"[{self.agent_id}] Swarm Agent Active. Bound to {model_name}. Connected to {router_address}")

    def query_ollama(self, prompt):
        try:
            start_time = time.time()
            res = requests.post(
                "http://localhost:11434/api/generate",
                json={"model": self.model_name, "prompt": prompt, "stream": False},
                timeout=120
            )
            latency = int((time.time() - start_time) * 1000)
            if res.status_code == 200:
                return res.json().get("response", ""), latency, True
            return f"Ollama Error: {res.status_code}", latency, False
        except Exception as e:
            return str(e), 0, False

    def run(self):
        # Register with the router
        reg_packet = json.dumps({"action": "REGISTER", "model": self.model_name})
        self.dealer.send_string(reg_packet)
        
        while True:
            # Poll for tasks (simulated here with an interactive loop for now, 
            # but usually this listens for ZMQ messages from the Router)
            try:
                msg = self.dealer.recv_string(flags=zmq.NOBLOCK)
                print(f"[{self.agent_id}] Router says: {msg}")
            except zmq.Again:
                pass
                
            time.sleep(1)

if __name__ == "__main__":
    agent = SwarmAgent(agent_id="AGENT_ALPHA_QWEN", model_name="qwen2.5:0.5b")
    agent.run()
