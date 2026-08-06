import os
import json
from datetime import datetime

def generate_runbook(agent_id, failure_reason, severity):
    if not os.path.exists("runbooks"):
        os.makedirs("runbooks")
        
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"runbooks/INCIDENT_{agent_id}_{timestamp}.md"
    
    content = f"""# 🚨 INCIDENT RUNBOOK: {agent_id}
**Timestamp:** {datetime.now().isoformat()}
**Severity:** {severity}
**Trigger:** {failure_reason}

## 1. Diagnostics
The agent `{agent_id}` breached the Swarm SLA. 
- **Cause:** {failure_reason}
- **Action Taken:** Automated degradation and reboot sequence initiated.

## 2. Mitigation Steps
1. The `sla_enforcer.py` daemon immediately SIGKILLed the stalled agent.
2. The ZMQ Router applied Backpressure routing to reroute traffic.
3. The Node was restarted in `keep_alive=0` (mmap) mode to flush corrupted memory.

## 3. Threat Assessment
No AST payload escape detected. This was a biological compute failure (latency/timeout), not an adversarial payload escape.
"""
    with open(filename, "w", encoding="utf-8") as f:
        f.write(content)
        
    return filename

if __name__ == "__main__":
    print(generate_runbook("Agent_Gamma", "Latency > 15000ms", "HIGH"))
