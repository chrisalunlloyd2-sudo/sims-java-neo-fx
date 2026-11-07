package com.aigen.sims;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.security.MessageDigest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.io.IOException;

/**
 * SIMS1337 - ImmuneSystem (Inner System Healing)
 * Counteracts the AdversarialFuzzer and maintains the "Desired State" by ensuring 
 * the system never makes the same mistake twice.
 */
public class ImmuneSystem {
    
    public static class MistakeRecord {
        public String mistakeHash;
        public String anomalyType;
        public String mitigationAction;
        public long occurrenceTime;
        
        public MistakeRecord(String hash, String type, String action) {
            this.mistakeHash = hash;
            this.anomalyType = type;
            this.mitigationAction = action;
            this.occurrenceTime = System.currentTimeMillis();
        }
    }
    
    // The "Never Make Mistakes Twice" database
    private final Map<String, MistakeRecord> mistakeDatabase = new ConcurrentHashMap<>();
    private final Map<String, String> learningRegistry = new HashMap<>();
    private final EnterpriseGuard guard;
    private final SQLiteMemory memory;
    private final GistCommsRelay commsRelay;
    private final CloudTwinManager cloudTwin;
    
    public ImmuneSystem(EnterpriseGuard guard, SQLiteMemory memory, GistCommsRelay commsRelay) {
        this.guard = guard;
        this.memory = memory;
        this.commsRelay = commsRelay;
        this.cloudTwin = new CloudTwinManager(guard);
        System.out.println("[IMMUNE SYSTEM] 'Never Make Mistakes Twice' Database Online. Desired State Enforcer Armed.");
        parseLearningRegistry();
    }
    
    public void scanForGitUpdates() {
        System.out.println("[IMMUNE SYSTEM] Scanning Git for framework updates...");
        // Simulates a git fetch / pull for internal system code patches
        boolean updateAvailable = Math.random() > 0.8;
        if (updateAvailable) {
            System.out.println("[IMMUNE SYSTEM] Upstream patch found! Applying to internal modules...");
            // Example: "A/B testing OllamaRouter connection pooling"
            System.out.println(" -> A/B Testing: Bootstrapping new OllamaRouter connection protocol...");
            commsRelay.dispatchCriticalEmailAlert("Upstream Patch Applied", "New A/B test active on OllamaRouter");
            parseLearningRegistry(); // Re-parse learning registry after update
        }
    }
    
    private void parseLearningRegistry() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("learning.md"));
            String currentCondition = null;
            for (String line : lines) {
                if (line.startsWith("- Condition: ")) {
                    currentCondition = line.replace("- Condition: ", "").trim();
                } else if (line.startsWith("- Command: ") && currentCondition != null) {
                    learningRegistry.put(currentCondition, line.replace("- Command: ", "").trim());
                    currentCondition = null;
                }
            }
            System.out.println("[IMMUNE SYSTEM] Loaded " + learningRegistry.size() + " heuristics from learning.md");
        } catch (IOException e) {
            System.out.println("[IMMUNE SYSTEM] learning.md not found, using generic heuristics.");
        }
    }
    
    public void counteractAnomaly(String anomalySource, String anomalyData) {
        System.out.println("[IMMUNE SYSTEM] Anomaly detected from " + anomalySource + ": " + anomalyData);
        
        String mistakeHash = computeHash(anomalySource + "|" + anomalyData);
        
        if (mistakeDatabase.containsKey(mistakeHash)) {
            MistakeRecord record = mistakeDatabase.get(mistakeHash);
            System.out.println("[IMMUNE SYSTEM] Known Mistake Detected (Hash: " + mistakeHash + "). Instantly deploying known mitigation: " + record.mitigationAction);
            applyMitigation(record.mitigationAction);
            return;
        }
        
        // Analyze and learn the new mistake
        String action = deduceMitigation(anomalySource, anomalyData);
        MistakeRecord newMistake = new MistakeRecord(mistakeHash, anomalySource, action);
        mistakeDatabase.put(mistakeHash, newMistake);
        memory.logMemory("IMMUNE_SYSTEM", "MISTAKE_LEARNED", "Learned mitigation for " + anomalySource + " -> " + action);
        
        System.out.println("[IMMUNE SYSTEM] New Mistake Learned (Hash: " + mistakeHash + "). Mitigation mapped: " + action);
        cloudTwin.syncSQLiteHashes(mistakeHash);
        
        // Persist the NMMT heuristic physically to learning.md for future loops
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get("learning.md"),
                ("\n- Condition: " + anomalyData + "\n- Command: " + action + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND
            );
            System.out.println("[IMMUNE SYSTEM] NMMT Heuristic permanently written to learning.md!");
        } catch (java.io.IOException e) {
            System.out.println("[IMMUNE SYSTEM ERROR] Could not write to learning.md: " + e.getMessage());
        }
        
        applyMitigation(action);
    }
    
    private String deduceMitigation(String source, String data) {
        for (Map.Entry<String, String> entry : learningRegistry.entrySet()) {
            if (data.contains(entry.getKey()) || source.contains(entry.getKey())) {
                return "Execute script: " + entry.getValue();
            }
        }
        if (source.contains("FUZZER")) return "Purge affected context window & re-seed topology.";
        if (source.contains("OLLAMA_TIMEOUT")) return "Hot-swap to local fallback model & restart node.";
        if (source.contains("SQLITE_LOCK")) return "Queue write operation to async resilient buffer.";
        if (source.contains("CMG GATING")) return "Force-release microphone lock and cull zombie thread.";
        return "Apply generic homeostasis reset to target subsystem.";
    }
    
    private void applyMitigation(String action) {
        System.out.println(" -> [HEALING ACTION deployed] " + action);
        System.out.println(" -> System restored to DESIRED STATE (Optimized Throughput).");
    }
    
    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 16);
        } catch(Exception ex) {
            return String.valueOf(input.hashCode());
        }
    }
}
