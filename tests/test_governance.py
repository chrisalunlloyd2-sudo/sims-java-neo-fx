import unittest
import os
import sys

# Add parent directory to path so we can import modules
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from sla_enforcer import enforce_sla
from tractability_gate import tractability_gate

class TestSwarmGovernance(unittest.TestCase):
    
    def test_sla_enforcer_healthy(self):
        """Test that an agent within 10,000ms latency is marked HEALTHY."""
        success, status, runbook = enforce_sla("mock_agent_1", 2000)
        self.assertTrue(success)
        self.assertEqual(status, "HEALTHY")
        self.assertIsNone(runbook)
        
    def test_sla_enforcer_breach(self):
        """Test that an agent breaching 10,000ms latency is degraded and a runbook is generated."""
        success, status, runbook = enforce_sla("mock_agent_2", 15000)
        self.assertFalse(success)
        self.assertEqual(status, "KILLED_AND_REBOOTING")
        self.assertIsNotNone(runbook)
        self.assertTrue(os.path.exists(runbook))
        
        # Cleanup generated runbook
        if os.path.exists(runbook):
            os.remove(runbook)
            
    def test_tractability_gate_missing_file(self):
        """Test that the tractability gate fails if required dependencies are missing."""
        mock_required = ["non_existent_file.java"]
        mock_limits = {}
        result = tractability_gate(mock_required, mock_limits)
        self.assertFalse(result)
        
    def test_tractability_gate_success(self):
        """Test that the tractability gate passes if all dependencies are present."""
        # Create a dummy file
        with open("dummy_dep.txt", "w") as f:
            f.write("OK")
            
        mock_required = ["dummy_dep.txt"]
        mock_limits = {}
        result = tractability_gate(mock_required, mock_limits)
        self.assertTrue(result)
        
        # Cleanup
        os.remove("dummy_dep.txt")

if __name__ == '__main__':
    unittest.main()
