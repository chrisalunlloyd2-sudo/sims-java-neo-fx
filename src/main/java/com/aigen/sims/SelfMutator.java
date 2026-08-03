package com.aigen.sims;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SelfMutator {
    private final String targetFile = "src/main/java/com/aigen/sims/GodHandApp.java";

    public SelfMutator() {
        System.out.println("[MUTATOR] v0.21.0 Real-Time Code Mutator Engine Armed.");
    }

    public boolean injectMutation(String proposal) {
        System.out.println("[MUTATOR] Preparing to inject AST mutation into core runtime for: " + proposal);
        try {
            Path path = Paths.get(targetFile);
            if (!Files.exists(path)) {
                System.out.println("[MUTATOR] Target file not found: " + targetFile);
                return false;
            }

            List<String> lines = Files.readAllLines(path);
            List<String> mutatedLines = new ArrayList<>();
            boolean injectionPointFound = false;

            for (String line : lines) {
                mutatedLines.add(line);
                // Look for the end of the initHexGrid method to inject a new dynamic station
                if (line.contains("grid.get(\"0,2\").station = \"Knowledge Tree\";") && !injectionPointFound) {
                    System.out.println(" -> Found injection marker. Splicing AST structure.");
                    mutatedLines.add("        // AUTONOMOUSLY MUTATED BY v0.21.0 ENGINE");
                    mutatedLines.add("        grid.get(\"2,2\").station = \"" + proposal.toUpperCase() + "_NEXUS\";");
                    injectionPointFound = true;
                }
            }

            if (injectionPointFound) {
                Files.write(path, mutatedLines);
                System.out.println("[MUTATOR] AST Splicing successful. Source code permanently altered.");
                return triggerRecompile();
            } else {
                System.out.println("[MUTATOR] Injection marker not found.");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[MUTATOR ERROR] " + e.getMessage());
            return false;
        }
    }

    private boolean triggerRecompile() {
        System.out.println("[MUTATOR] Triggering localized autonomous recompilation (javac)...");
        try {
            String cp = System.getProperty("java.class.path");
            String mp = System.getProperty("jdk.module.path");
            
            // Construct the compile command safely
            List<String> cmd = new ArrayList<>();
            cmd.add("javac");
            cmd.add("-encoding"); cmd.add("UTF-8");
            cmd.add("-d"); cmd.add("target/classes");
            if (cp != null && !cp.isEmpty()) { cmd.add("-cp"); cmd.add(cp); }
            if (mp != null && !mp.isEmpty()) { 
                cmd.add("--module-path"); cmd.add(mp); 
                cmd.add("--add-modules"); cmd.add("javafx.controls,javafx.fxml");
            }
            cmd.add("src/main/java/com/aigen/sims/GodHandApp.java");
            cmd.add("src/main/java/com/aigen/sims/ModelManager.java");
            cmd.add("src/main/java/com/aigen/sims/KnowledgeGraph.java");
            cmd.add("src/main/java/com/aigen/sims/GistSync.java");
            cmd.add("src/main/java/com/aigen/sims/SQLiteMemory.java");
            cmd.add("src/main/java/com/aigen/sims/OllamaRouter.java");
            cmd.add("src/main/java/com/aigen/sims/SelfMutator.java");
            cmd.add("src/main/java/com/aigen/sims/EnterpriseGuard.java");
            cmd.add("src/main/java/com/aigen/sims/SwarmWatchdog.java");
            cmd.add("src/main/java/com/aigen/sims/MCTSPipeline.java");
            cmd.add("src/main/java/com/aigen/sims/AdversarialFuzzer.java");

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(" [JAVAC] " + line);
            }
            
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                System.out.println("[MUTATOR] Compilation SUCCESSFUL. Next JVM cycle will boot with new genetics.");
                return true;
            } else {
                System.out.println("[MUTATOR] Compilation FAILED. Reverting genetics (Requires AST repair module).");
                return false;
            }
        } catch (Exception e) {
            System.err.println("[MUTATOR ERROR] Recompilation execution failed: " + e.getMessage());
            return false;
        }
    }
}
