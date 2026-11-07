package com.aigen.sims.mining;
import java.util.*;
public class CodeMinerOrchestrator {
    private final CodeMiner miner;
    private final SuggestionRegistry registry;
    private final Map<String, int[]> modelPositions = new HashMap<>();
    private static final Map<String, List<String>> CAPS = new HashMap<>();
    static {
        CAPS.put("qwen2.5-coder:1.5b", Arrays.asList("java","python","shell","fast"));
        CAPS.put("qwen2.5-coder:0.5b", Arrays.asList("java","python","balanced"));
        CAPS.put("qwen2.5-coder:1.5b", Arrays.asList("java","python","reasoning"));
        CAPS.put("qwen2.5-coder:1.5b", Arrays.asList("java","python","deep"));
        CAPS.put("qwen2.5-coder:0.5b", Arrays.asList("java","python","tool"));
        CAPS.put("qwen2.5:1.5b", Arrays.asList("java","python","logic","deep"));
        CAPS.put("codellama:7b", Arrays.asList("java","codegen","backend"));
        CAPS.put("qwen2.5-coder:0.5b", Arrays.asList("java","python","balanced"));
    }
    public CodeMinerOrchestrator(String reposPath, String suggestionsPath) {
        this.miner = new CodeMiner(reposPath);
        this.registry = new SuggestionRegistry(suggestionsPath);
        modelPositions.put("qwen2.5-coder:1.5b", new int[]{0,0});
        modelPositions.put("qwen2.5-coder:0.5b", new int[]{0,0});
        modelPositions.put("qwen2.5-coder:1.5b", new int[]{3,-2});
        modelPositions.put("qwen2.5-coder:1.5b", new int[]{3,-2});
        modelPositions.put("qwen2.5-coder:0.5b", new int[]{-3,2});
        modelPositions.put("qwen2.5:1.5b", new int[]{-3,2});
        modelPositions.put("codellama:7b", new int[]{1,0});
        modelPositions.put("qwen2.5-coder:0.5b", new int[]{1,0});
    }
    public MiningReport runMiningCycle() {
        List<RepoContext> repos = miner.scanAllRepos();
        List<Suggestion> generated = new ArrayList<>();
        int skipped = 0;
        for (RepoContext repo : repos) {
            System.out.println("[CODE MINER DEBUG] Processing repo: " + repo.repoName + " (Changes: " + repo.hasChanges() + ")");
            if (!repo.hasChanges()) { skipped++; continue; }
            String bestModel = findBestModel(repo);
            System.out.println("[CODE MINER DEBUG] Best model for " + repo.repoName + ": " + bestModel);
            if (bestModel == null) { skipped++; continue; }
            Suggestion s = generateSuggestion(repo, bestModel);
            if (s != null) { 
                registry.addSuggestion(s); generated.add(s); 
                System.out.println("[CODE MINER DEBUG] Generated suggestion successfully!");
            } else {
                System.out.println("[CODE MINER DEBUG] Failed to generate suggestion for " + repo.repoName);
            }
        }
        return new MiningReport(repos.size(), generated.size(), skipped, registry.getSummary(), System.currentTimeMillis());
    }
    private String findBestModel(RepoContext repo) {
        String best = "qwen2.5-coder:1.5b"; // Fallback default
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, int[]> e : modelPositions.entrySet()) {
            int[] p = e.getValue();
            int dq = Math.abs(repo.hexQ - p[0]), dr = Math.abs(repo.hexR - p[1]);
            int ds = Math.abs((-repo.hexQ-repo.hexR) - (-p[0]-p[1]));
            int dist = Math.max(Math.max(dq, dr), ds);
            
            double score = 1.0 / (1.0 + dist);
            List<String> caps = CAPS.getOrDefault(e.getKey(), new ArrayList<>());
            for (String fp : repo.fileContents.keySet()) {
                String ext = fp.contains(".") ? fp.substring(fp.lastIndexOf('.')+1) : "";
                if (caps.contains(ext)) score += 0.5;
            }
            if (score > bestScore) { bestScore = score; best = e.getKey(); }
        }
        return best;
    }
    private Suggestion generateSuggestion(RepoContext repo, String modelName) {
        String targetFile = null; String content = null;
        for (Map.Entry<String, String> e : repo.fileContents.entrySet()) {
            if (e.getKey().endsWith(".java")) { targetFile = e.getKey(); content = e.getValue(); break; }
        }
        if (targetFile == null) return null;
        int lastBrace = content.lastIndexOf('}');
        if (lastBrace < 0) return null;
        int lineStart = content.lastIndexOf('\n', lastBrace);
        String insertAfter = content.substring(lineStart+1, lastBrace).trim();
        
        // Android Game Mechanic Injection
        String code = String.format(
            "\n    /** \n     * Android VM Test-Bed Injection via %s \n     * Mechanic: 6D Topo-State Evaluation \n     */\n" +
            "    public void evaluate6DState(android.content.Context ctx) {\n" +
            "        System.out.println(\"[%s] Executing Gamified Android 12 Logic...\");\n" +
            "        System.out.println(\" -> Memory Allocation OK. Checking SLM Bridge connection.\");\n" +
            "    }\n", modelName, modelName);
            
        return new Suggestion(repo.repoName, targetFile, insertAfter, code,
            modelName, repo.hexQ, repo.hexR, "Android VM Game Mechanic by " + modelName);
    }
    public void setModelPosition(String m, int q, int r) { modelPositions.put(m, new int[]{q,r}); }
    public SuggestionRegistry getRegistry() { return registry; }
    public static class MiningReport {
        public final int reposScanned, suggestionsGenerated, reposSkipped;
        public final Map<String, Integer> summary; public final long timestamp;
        public MiningReport(int r, int g, int s, Map<String, Integer> sum, long t) {
            reposScanned=r; suggestionsGenerated=g; reposSkipped=s; summary=sum; timestamp=t;
        }
        public String toEmailString() {
            return String.format("📊 Mining Report\n   Scanned: %d\n   Generated: %d\n   Skipped: %d\n   Total: %d | Pending: %d | Approved: %d | Rejected: %d | Deployed: %d",
                reposScanned, suggestionsGenerated, reposSkipped,
                summary.getOrDefault("total",0), summary.getOrDefault("pending",0),
                summary.getOrDefault("approved",0), summary.getOrDefault("rejected",0),
                summary.getOrDefault("deployed",0));
        }
    }
}
