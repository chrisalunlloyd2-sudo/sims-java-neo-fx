package com.aigen.sims;

import java.util.*;

public class ModelManager {
    public static class ModelProfile {
        public String name;
        public String role;
        public List<String> abilities;

        public ModelProfile(String name, String role, String... abilities) {
            this.name = name;
            this.role = role;
            this.abilities = Arrays.asList(abilities);
        }
    }

    private final Map<String, ModelProfile> profiles = new HashMap<>();

    public ModelManager() {
        profiles.put("deepseek-r1:1.5b", new ModelProfile("deepseek-r1:1.5b", "Reasoning", "logic", "backend"));
        profiles.put("codellama:7b", new ModelProfile("codellama:7b", "Code Gen", "tool", "backend"));
        profiles.put("qwen2.5:0.5b", new ModelProfile("qwen2.5:0.5b", "Agent Driver", "grid", "ability"));
        profiles.put("phi3:mini", new ModelProfile("phi3:mini", "Deep Reason", "logic", "node"));
        profiles.put("llama3.2:1b", new ModelProfile("llama3.2:1b", "General", "tool", "ability"));
        profiles.put("tinyllama:1.1b", new ModelProfile("tinyllama:1.1b", "Fast Query", "ability", "grid"));
        profiles.put("gemma2:2b", new ModelProfile("gemma2:2b", "Balanced", "node", "grid"));
        profiles.put("phi:latest", new ModelProfile("phi:latest", "Reasoning", "logic", "tool"));
    }

    public List<ModelProfile> getSwarm() {
        return new ArrayList<>(profiles.values());
    }

    public boolean executeVote(String proposalAbilities, OllamaRouter router) {
        System.out.println("[VOTE ENGINE] Executing Role-Based Consensus on: " + proposalAbilities);
        int yesVotes = 0;
        
        for (ModelProfile model : profiles.values()) {
            String prompt = "You are a strictly rational AI voting node. Your specialty is: " + String.join(",", model.abilities) + 
                            ". Evaluate this proposal: [" + proposalAbilities + "]. If it aligns with your specialty or seems useful, reply with exactly YES. Otherwise reply with exactly NO. Say nothing else. No preamble.";
                            
            String response = router.query(model.name, prompt).toUpperCase();
            
            boolean vote = response.contains("YES");
            if (vote) yesVotes++;
            System.out.println(" -> " + model.name + " (" + model.role + "): " + (vote ? "YES" : "NO"));
        }
        
        boolean approved = yesVotes >= 5;
        System.out.println("[VOTE ENGINE] Final Tally: " + yesVotes + "/8. Status: " + (approved ? "APPROVED" : "REJECTED"));
        return approved;
    }
}
