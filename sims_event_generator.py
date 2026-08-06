
import zmq
import time
import json
import random

def generate_events():
    context = zmq.Context()
    socket = context.socket(zmq.PUB)
    socket.bind("tcp://127.0.0.1:5556")
    
    events = ["RESOURCE_DEPLETED", "MUTATION_TRIGGERED", "AGENT_COLLISION", "THREAT_DETECTED"]
    print("[EVENT GENERATOR] Hooked to ZMQ. Emitting Sims1337 events...")
    
    while True:
        event = random.choice(events)
        payload = json.dumps({"event": event, "intensity": random.random()})
        socket.send_string(f"SIMS_EVENT {payload}")
        print(f"Emitted: {event}")
        time.sleep(10)

if __name__ == "__main__":
    generate_events()
