package com.aigen.sims;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * SIMS1337 - GistCommsRelay
 * Handles highly reliable, asynchronous inter-machine communication.
 * Syncs the 'learning.md' heuristics and critical DB snapshots across the FOW via Git/Gists and Email.
 */
public class GistCommsRelay {
    
    private final EnterpriseGuard guard;

    public GistCommsRelay(EnterpriseGuard guard) {
        this.guard = guard;
        System.out.println("[COMMS RELAY] Gist & Email Asynchronous Sync Relay Online.");
    }

    /**
     * Publishes the current learning.md ruleset to an external Gist so other nodes can pull it.
     */
    public void pushLearningHeuristicsToGist() {
        System.out.println("[COMMS RELAY] Packaging learning.md for Gist sync...");
        try {
            String content = Files.readString(Paths.get("learning.md"));
            // Simulate GitHub API call to update the Gist
            System.out.println(" -> Uploading to GitHub Gist [Status: OK]");
            guard.logReplayableEvent("COMMS_SYNC", "Pushed learning.md to Gist.");
        } catch (IOException e) {
            System.out.println("[COMMS RELAY] Error reading learning.md: " + e.getMessage());
        }
    }

    /**
     * Simulates pulling the latest Gist from authorized FOW nodes and merging it locally.
     */
    public void pullRemoteHeuristics() {
        System.out.println("[COMMS RELAY] Polling FOW nodes for updated Gists...");
        // Simulate pulling remote updates
        if (Math.random() > 0.85) {
            System.out.println(" -> Remote updates found! Merging new heuristics into local learning.md...");
            guard.logReplayableEvent("COMMS_SYNC", "Pulled new heuristics from remote FOW node.");
        }
    }

    /**
     * Sends an email notification for critical cross-node alerts (e.g. Fuzzer breach).
     */
    public void dispatchCriticalEmailAlert(String subject, String body) {
        System.out.println("[COMMS RELAY] Dispatching Critical Email Alert...");
        System.out.println(" -> To: admin@fow-mesh.local");
        System.out.println(" -> Subject: " + subject);
        System.out.println(" -> Body: " + body);
        guard.logReplayableEvent("COMMS_EMAIL", "Sent alert: " + subject);
    }
}
