package com.aigen.sims;

import java.util.Stack;
import java.util.concurrent.Callable;

/**
 * SIMS1337 - MotherSystem (The Overseer)
 * Ensures that every change proposed by the Fuzzer, ImmuneSystem, or Swarm is evaluated.
 * If a change deviates from the "Desired State" (perfect optimization/throughput ratio),
 * the Mother System instantly enforces a rollback.
 */
public class MotherSystem {

    public static class TransactionSnapshot {
        public final String context;
        public final long timestamp;
        public final Runnable rollbackAction;

        public TransactionSnapshot(String context, Runnable rollbackAction) {
            this.context = context;
            this.timestamp = System.currentTimeMillis();
            this.rollbackAction = rollbackAction;
        }
    }

    private final Stack<TransactionSnapshot> changeHistory = new Stack<>();
    private final EnterpriseGuard guard;

    public MotherSystem(EnterpriseGuard guard) {
        this.guard = guard;
        System.out.println("[MOTHER SYSTEM] Overseer Online. Global Rollback Protection Armed.");
    }

    /**
     * Executes a change and verifies if the Desired State is maintained. 
     * If not, automatically rolls back.
     */
    public void enforceChange(String context, Runnable changeAction, Runnable rollbackAction, Callable<Boolean> verifyDesiredState) {
        System.out.println("[MOTHER SYSTEM] Intercepting proposed change: " + context);
        
        TransactionSnapshot snapshot = new TransactionSnapshot(context, rollbackAction);
        changeHistory.push(snapshot);
        
        try {
            changeAction.run();
            
            if (!verifyDesiredState.call()) {
                System.out.println("[MOTHER SYSTEM WARNING] Desired State Compromised after: " + context);
                triggerRollback();
            } else {
                System.out.println("[MOTHER SYSTEM] Change approved. Desired State maintained.");
                // Optimization stabilized
            }
        } catch (Exception e) {
            System.out.println("[MOTHER SYSTEM CRITICAL] Exception during change execution: " + e.getMessage());
            triggerRollback();
        }
    }

    private void triggerRollback() {
        if (changeHistory.isEmpty()) return;
        
        TransactionSnapshot lastChange = changeHistory.pop();
        System.out.println("[MOTHER SYSTEM] Executing Rollback for: " + lastChange.context);
        
        try {
            lastChange.rollbackAction.run();
            System.out.println("[MOTHER SYSTEM] Rollback successful. System restored.");
            guard.logReplayableEvent("MOTHER_SYSTEM_ROLLBACK", "Rolled back: " + lastChange.context);
        } catch (Exception e) {
            System.out.println("[MOTHER SYSTEM FATAL] Rollback failed! " + e.getMessage());
        }
    }
}
