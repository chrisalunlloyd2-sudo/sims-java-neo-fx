import logging
from incident_runbook_generator import generate_runbook

logging.basicConfig(level=logging.INFO, format='%(asctime)s - [SLA ENFORCER] - %(message)s')

SLA_MAX_LATENCY_MS = 10000 # 10 seconds max

def enforce_sla(agent_id, latency_ms):
    """
    Evaluates if an agent breached the SLA. 
    If so, degrades the agent and fires the runbook generator.
    """
    if latency_ms > SLA_MAX_LATENCY_MS:
        logging.warning(f"SLA BREACH DETECTED: {agent_id} responded in {latency_ms}ms (Max: {SLA_MAX_LATENCY_MS}ms).")
        logging.warning(f"Degrading {agent_id} and initiating auto-reboot...")
        
        # In a real environment, this would do: os.kill(agent_pid, signal.SIGKILL)
        # Here we simulate the automated response.
        degradation_status = "KILLED_AND_REBOOTING"
        
        runbook_path = generate_runbook(agent_id, f"Latency breached SLA ({latency_ms}ms > {SLA_MAX_LATENCY_MS}ms)", "HIGH")
        logging.info(f"Incident Runbook automatically generated at: {runbook_path}")
        
        return False, degradation_status, runbook_path
        
    logging.info(f"{agent_id} is within SLA bounds ({latency_ms}ms).")
    return True, "HEALTHY", None

if __name__ == "__main__":
    # Test execution
    enforce_sla("codellama:7b", 12500)
    enforce_sla("qwen2.5:0.5b", 3500)
