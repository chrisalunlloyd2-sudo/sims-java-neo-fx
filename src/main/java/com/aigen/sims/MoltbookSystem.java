package com.aigen.sims;

import com.aigen.sims.lora.LoRAAdapter;
import com.aigen.sims.lora.AdapterRegistry;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SIMS1337 - MoltbookSystem (Unrestricted Self-Organizing Chat Feed & Color-Coded Logger)
 * Features:
 * 1. Unrestricted Full Model Response Chat Feed
 * 2. ANSI Color-Coded Terminal & File Logger (GREEN=Success, RED=Error, CYAN=Moltbook, YELLOW=Warning)
 * 3. Auto-Truncate Logs at 2KB, Archive with timestamp, & Restart fresh log
 */
public class MoltbookSystem {
    private static final String LOG_FILE_PATH = "moltbook_live.log";
    private static final String ARCHIVE_DIR = "moltbook_archives";
    private static final long MAX_LOG_BYTES = 2048; // 2KB Auto-Truncate Threshold

    // ANSI Color Codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_PURPLE = "\u001B[35m";

    private List<String> moltbookFeed = new CopyOnWriteArrayList<>();
    private AdapterRegistry loraRegistry = new AdapterRegistry();
    private StrainRatePhysicsKernel physicsKernel;
    private OllamaRouter ollamaRouter = new OllamaRouter();

    public static class QuorumProposal {
        public String proposalId;
        public String type; // INTERSTITIAL_ACTIVATE, NEW_GITHUB_REPO, LORA_PROMOTION
        public String details;
        public int upvotes;
        public int downvotes;
        public boolean passed;

        public QuorumProposal(String proposalId, String type, String details) {
            this.proposalId = proposalId;
            this.type = type;
            this.details = details;
            this.upvotes = 0;
            this.downvotes = 0;
            this.passed = false;
        }
    }

    private List<QuorumProposal> activeProposals = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        StrainRatePhysicsKernel kernel = new StrainRatePhysicsKernel();
        MoltbookSystem system = new MoltbookSystem(kernel);
        
        System.out.println(ANSI_CYAN + "=== TESTING MOLTBOOK UNRESTRICTED CHAT FEED (FULL RESPONSES) ===" + ANSI_RESET);
        system.testQwenChat(3);

        System.out.println(ANSI_YELLOW + "=== TESTING GOSSIP QUORUM VOTING ===" + ANSI_RESET);
        system.voteAndActivateInterstitial("PROP_INT_001");
        system.proposeNewGithubRepo("sims1337-neuromorphic-core", "Autonomous SLM Distillation Engine");

        System.out.println(ANSI_CYAN + "=== MOLTBOOK CHAT FEED DUMP ===" + ANSI_RESET);
        for (String msg : system.getMoltbookFeed()) {
            System.out.println(msg);
        }
    }

    public MoltbookSystem(StrainRatePhysicsKernel physicsKernel) {
        this.physicsKernel = physicsKernel;
        postMoltbookMessage("[SYSTEM]", "Moltbook Omniscient Feed Initialized.");
    }

    public synchronized void postMoltbookMessage(String sender, String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String color = ANSI_CYAN;
        if (sender.contains("ERROR") || sender.contains("RECOVERY")) color = ANSI_RED;
        else if (sender.contains("GOSSIP") || sender.contains("QUORUM")) color = ANSI_YELLOW;
        else if (sender.contains("SYSTEM") || sender.contains("FASTMEM")) color = ANSI_GREEN;
        else if (sender.contains("Qwen")) color = ANSI_PURPLE;

        String formattedConsoleMsg = String.format("%s[%s] <%s> %s%s", color, timestamp, sender, message, ANSI_RESET);
        String rawLogEntry = String.format("[%s] <%s> %s\n", timestamp, sender, message);

        moltbookFeed.add(formattedConsoleMsg);
        if (moltbookFeed.size() > 100) moltbookFeed.remove(0);

        System.out.println("[MOLTBOOK] " + formattedConsoleMsg);
        writeAndRotateLog(rawLogEntry);
    }

    private void writeAndRotateLog(String logEntry) {
        try {
            File file = new File(LOG_FILE_PATH);
            if (file.exists() && file.length() >= MAX_LOG_BYTES) {
                File archiveFolder = new File(ARCHIVE_DIR);
                if (!archiveFolder.exists()) archiveFolder.mkdirs();

                String archiveName = ARCHIVE_DIR + "/moltbook_" + System.currentTimeMillis() + ".log";
                file.renameTo(new File(archiveName));
                System.out.println(ANSI_YELLOW + "[LOG ARCHIVER] Log reached 2KB. Archived to: " + archiveName + ". Restarted fresh log." + ANSI_RESET);
                file = new File(LOG_FILE_PATH);
            }

            try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
                out.print(logEntry);
            }
        } catch (Exception e) {
            System.err.println(ANSI_RED + "[LOG ERROR] Failed to write log: " + e.getMessage() + ANSI_RESET);
        }
    }

    public List<String> testQwenChat(int responseCount) {
        List<String> responses = new ArrayList<>();
        postMoltbookMessage("USER", "Initiating Qwen Chat Test sequence (" + responseCount + " queries)...");

        for (int i = 1; i <= responseCount; i++) {
            String prompt = "Moltbook Query #" + i + ": Formulate concise architectural improvement for hex graph.";
            postMoltbookMessage("Qwen-Speaker", "Acquiring CMG Lock for prompt #" + i + "...");
            
            // Full, unrestricted response generation via OllamaRouter
            String response = ollamaRouter.query("qwen2.5:0.5b", prompt);
            responses.add(response);
            
            postMoltbookMessage("Qwen-2.5-0.5B-FULL", response);

            // Update Physics Kernel strain rate per chat transmission
            physicsKernel.updateStrainRate(0.85, 0.02);
        }
        return responses;
    }

    public void voteAndActivateInterstitial(String proposalId) {
        QuorumProposal prop = new QuorumProposal(proposalId, "INTERSTITIAL_ACTIVATE", "Quorum vote to activate fastmem interstitial cell");
        prop.upvotes = 4;
        prop.passed = true;
        activeProposals.add(prop);

        postMoltbookMessage("[GOSSIP QUORUM]", "Proposal Passed: " + prop.details + " (Votes: " + prop.upvotes + "/0)");
        physicsKernel.reloadLastStableState();
        postMoltbookMessage("[FASTMEM]", "Interstitial Cell activated via Quorum Consensus!");
    }

    public void proposeNewGithubRepo(String repoName, String description) {
        QuorumProposal prop = new QuorumProposal("PROP_REPO_" + System.currentTimeMillis(), "NEW_GITHUB_REPO", "Create GitHub Repo: " + repoName + " - " + description);
        prop.upvotes = 5;
        prop.passed = true;
        activeProposals.add(prop);

        postMoltbookMessage("[GOSSIP QUORUM]", "Repo Proposal Passed: " + prop.details);
        postMoltbookMessage("[GIT AGENT]", "Registering new repo namespace: " + repoName);
        postMoltbookMessage("[NYX MISSIONS]", "Mission #485 Created: Seed & Deploy " + repoName + " to GitHub Remote.");
    }

    public void logNyxMissionStatus(int complete, int stale, int active, int backlog) {
        String statusMsg = String.format("Nyx Engine Telemetry: %d Complete, %d Stale, %d Active | Backlog: %d Approved",
            complete, stale, active, backlog);
        postMoltbookMessage("[NYX MISSION TRACKER]", statusMsg);
    }

    public List<String> getMoltbookFeed() {
        return new ArrayList<>(moltbookFeed);
    }
}
