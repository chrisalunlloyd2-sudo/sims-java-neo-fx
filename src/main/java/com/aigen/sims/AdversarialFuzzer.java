package com.aigen.sims;

/**
 * ADVERSARIAL FUZZER
 * Probes the network and AST structures for vulnerabilities and forces emergent defense behaviors.
 */
public class AdversarialFuzzer {
    private final OllamaRouter router;
    private final EnterpriseGuard guard;

    public AdversarialFuzzer(OllamaRouter router, EnterpriseGuard guard) {
        this.router = router;
        this.guard = guard;
        System.out.println("[FUZZER] Adversarial Fuzzing Engine Armed.");
    }

    public void fuzzNetwork() {
        guard.logReplayableEvent("FUZZ_INJECT", "Target: Core Topology");
        System.out.println("[FUZZER] Injecting chaotic bit-flips into simulated environment...");
        
        if (guard.checkCircuit("qwen2.5-coder:0.5b")) {
            String attackVector = router.query("qwen2.5-coder:0.5b", "Generate a random nonsense string of 10 characters to fuzz the system.");
            if (attackVector.equals("NO_RESPONSE")) guard.registerFailure("qwen2.5-coder:0.5b");
            else {
                guard.registerSuccess("qwen2.5-coder:0.5b");
                boolean valid = guard.validateProposal(attackVector);
                System.out.println("[FUZZER] Attack Vector [" + attackVector + "] Deflected: " + (!valid));
            }
        }
    }
}
