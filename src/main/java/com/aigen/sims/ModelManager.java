package com.aigen.sims;

import com.aigen.sims.voting.WeightedQuorumVote;
import com.aigen.sims.voting.HexCoord;
import java.util.*;

public class ModelManager {
    public static class ModelProfile {
        public String name;
        public String role;
        public List<String> abilities;
        public HexCoord hex;

        public ModelProfile(String name, String role, HexCoord hex, String... abilities) {
            this.name = name;
            this.role = role;
            this.hex = hex;
            this.abilities = Arrays.asList(abilities);
        }
    }

    private final Map<String, ModelProfile> profiles = new LinkedHashMap<>();
    private final WeightedQuorumVote quorumEngine;

    public ModelManager() {
        // Setup 192-edge Hexeract topological positions for SLM Nodes
        profiles.put("qwen2.5:0.5b", new ModelProfile("qwen2.5:0.5b", "Agent Driver", new HexCoord(0, 0, 0), "grid", "ability"));
        profiles.put("tinyllama:1.1b", new ModelProfile("tinyllama:1.1b", "Fast Query", new HexCoord(1, 0, 0), "ability", "grid"));
        profiles.put("deepseek-r1:1.5b", new ModelProfile("deepseek-r1:1.5b", "Reasoning Auditor", new HexCoord(0, 1, 0), "logic", "backend"));
        profiles.put("qwen2.5:3b", new ModelProfile("qwen2.5:3b", "Code Miner", new HexCoord(1, 1, 0), "tool", "backend"));
        profiles.put("phi:latest", new ModelProfile("phi:latest", "Strategic Planner", new HexCoord(0, 0, 1), "logic", "tool"));

        // Initialize Stratum 3 Quorum Homology Engine (fowHop=2, quorumMin=3, approveMin=2)
        this.quorumEngine = new WeightedQuorumVote(2, 3, 2);
        for (ModelProfile m : profiles.values()) {
            quorumEngine.setModelPosition(m.name, m.hex.q, m.hex.r, m.hex.z);
        }
    }

    public List<ModelProfile> getSwarm() {
        return new ArrayList<>(profiles.values());
    }

    public boolean executeVote(String proposalAbilities, OllamaRouter router) {
        System.out.println("[QUORUM HOMOLOGY BUS] Executing Vietoris-Rips Gated Consensus on: " + proposalAbilities);
        
        String propId = "PROP-" + System.currentTimeMillis() % 10000;
        WeightedQuorumVote.Proposal prop = new WeightedQuorumVote.Proposal(propId, proposalAbilities, new HexCoord(0, 0, 0), 0.5);

        int totalWeight = 0;
        int yesCount = 0;

        for (ModelProfile model : profiles.values()) {
            String prompt = "You are a strictly rational AI voting node. Your specialty is: " + String.join(",", model.abilities) + 
                            ". Evaluate this proposal: [" + proposalAbilities + "]. If it aligns with your specialty or seems useful, reply with exactly YES. Otherwise reply with CSV format: NO. Say nothing else. No preamble.";
                            
            String response = router.query(model.name, prompt).toUpperCase();
            
            boolean vote = response.contains("YES");
            if (vote) {
                prop.votes.put(model.name, WeightedQuorumVote.Vote.APPROVE);
                yesCount++;
            } else {
                prop.votes.put(model.name, WeightedQuorumVote.Vote.REJECT);
            }
            
            System.out.println(" -> SLM Node " + model.name + " (" + model.role + " at Hex " + model.hex + "): " + (vote ? "APPROVE" : "REJECT"));
        }

        // Evaluate status using 4D Time-Pulse and Hex-Proximity Weighted Homology
        Map<String, WeightedQuorumVote.ModelPosition> modelPosMap = new HashMap<>();
        for (ModelProfile mp : profiles.values()) {
            WeightedQuorumVote.ModelPosition pos = new WeightedQuorumVote.ModelPosition(mp.name, mp.hex);
            pos.pulsePhase = 0.5; // Resonant phase bonus
            modelPosMap.put(mp.name, pos);
        }

        String consensusStatus = prop.status(modelPosMap, 3, 2);
        boolean approved = "APPROVED".equals(consensusStatus);
        
        System.out.println("[QUORUM HOMOLOGY TALLY] " + prop + " | Consensus Status: " + consensusStatus + " (Approved: " + approved + ")");
        return approved;
    }
}
