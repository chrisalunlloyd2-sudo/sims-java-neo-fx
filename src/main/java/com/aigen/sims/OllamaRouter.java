package com.aigen.sims;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * SIMS1337 - OllamaRouter
 * Cellular Microphone Gating (CMG) Implementation
 * Ensures only the active model in use remains in RAM/VRAM
 */
public class OllamaRouter {
    private static String activeModel = null;
    private static final Object micLock = new Object();
    private static long lastCallTimestamp = 0;

    // 2026-11-05: the box-wide Viper mic (model_lock.py). GodHand reads it and yields; it never writes it.
    private static final java.nio.file.Path BOX_MIC = java.nio.file.Paths.get("C:\\Viper\\databases\\aegis\\cellular_mic\\mic.json");
    private static final int AUTONOMOUS_PREDICT = 2;   // the low-overhead autonomy loops: 2 tokens
    private static final long BOX_MIC_WAIT_MS = 60_000;

    // 2026-11-09 (Chris: 2048 minimum, global). ONE window for every GodHand call. The autonomy loops asked
    // for 1024 and Karoo for 4096: every switch made ollama RELOAD the model, and the 2-token loops held the
    // lock while Karoo waited, so a chat question got no answer at all.
    public static final int CTX = 4096;
    /** Set while a person is waiting on a reply; the 2-token autonomy calls step aside until it is answered. */
    private static final java.util.concurrent.atomic.AtomicInteger PEOPLE_WAITING = new java.util.concurrent.atomic.AtomicInteger();

    public String query(String model, String prompt) {
        return query(model, prompt, AUTONOMOUS_PREDICT, null, CTX);
    }

    /** Model the box mic is serving right now, or null when nobody holds it. */
    public static String boxMicModel() {
        return boxMicModel(BOX_MIC);
    }

    static String boxMicModel(java.nio.file.Path micJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode mic = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(java.nio.file.Files.readString(micJson));
            return mic.path("holders").size() > 0 ? mic.path("model").asText(null) : null;
        } catch (Exception e) {
            return null; // mid-write or absent: treat as free
        }
    }

    /**
     * Wait while the box mic serves a different model. Same model = join (as model_lock allows).
     * Returns true when clear. Autonomous 2-token calls give up and skip; user calls go ahead after the wait.
     */
    private boolean yieldToBoxMic(String model, boolean autonomous) {
        long deadline = System.currentTimeMillis() + BOX_MIC_WAIT_MS;
        while (true) {
            String held = boxMicModel();
            if (held == null || held.equals(model)) return true;
            if (System.currentTimeMillis() > deadline) {
                System.out.println("[BOX MIC] " + held + " still holds the Viper mic after 60 s; "
                    + (autonomous ? "skipping autonomous call to " : "proceeding with user call to ") + model);
                return !autonomous;
            }
            try { Thread.sleep(2000); } catch (InterruptedException ie) { return false; }
        }
    }

    public String query(String model, String prompt, int numPredict, Double temperature, int numCtx) {
        numCtx = Math.max(numCtx, CTX);
        boolean autonomous = numPredict <= AUTONOMOUS_PREDICT;
        if (autonomous && PEOPLE_WAITING.get() > 0) return "NO_RESPONSE";   // a person goes first
        if (!autonomous) PEOPLE_WAITING.incrementAndGet();
        try {
            return queryLocked(model, prompt, numPredict, temperature, numCtx, autonomous);
        } finally {
            if (!autonomous) PEOPLE_WAITING.decrementAndGet();
        }
    }

    private String queryLocked(String model, String prompt, int numPredict, Double temperature, int numCtx, boolean autonomous) {
        // Dynamic Adaptive Pacing: Synchronized with CPU Load, Stress & Breathing Cycle
        synchronized (micLock) {
            if (!yieldToBoxMic(model, numPredict <= AUTONOMOUS_PREDICT)) return "NO_RESPONSE";
            double cpuLoad = com.sun.management.OperatingSystemMXBean.class.isInstance(
                java.lang.management.ManagementFactory.getOperatingSystemMXBean()) ?
                ((com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean()).getCpuLoad() : 0.50;
            if (Double.isNaN(cpuLoad) || cpuLoad < 0) cpuLoad = 0.50;

            // Exponential Pacing Factor: High CPU load scales delay exponentially (1.0s up to 16.0s factor)
            long basePacingMs = 2000; 
            double cpuMultiplier = Math.pow(2.0, cpuLoad * 4.0); // Factor of 1x to 16x multiplier
            long requiredDelayMs = (long) (basePacingMs * cpuMultiplier);

            long elapsed = System.currentTimeMillis() - lastCallTimestamp;
            if (elapsed < requiredDelayMs && autonomous) {   // pacing is for the loops, never for a person
                long sleepTime = requiredDelayMs - elapsed;
                System.out.println(String.format("[BREATHING PACING] CPU Load: %.1f%% | Dynamic Delay Factor: %.2fx | Throttling Ollama call for %d ms...", 
                    cpuLoad * 100.0, cpuMultiplier, sleepTime));
                try { Thread.sleep(sleepTime); } catch(Exception ignored) {}
            }
            lastCallTimestamp = System.currentTimeMillis();

            if (activeModel != null && !activeModel.equals(model)) {
                unloadModel(activeModel);
            }
            activeModel = model;
            
            System.out.println("[CELLULAR MIC GATING] Speaker Lock Acquired: " + model);
            try {
                URL url = new URL("http://localhost:11434/api/generate");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(30000);  // 30s connect timeout
                con.setReadTimeout(300000);    // 300,000s (5 minutes) read timeout - Strict speaker lock held until model response or timeout
                
                // Use Jackson ObjectMapper for robust, error-free JSON serialization
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> reqMap = new java.util.HashMap<>();
                reqMap.put("model", model);
                reqMap.put("prompt", prompt);
                reqMap.put("keep_alive", "15m");
                reqMap.put("stream", false);
                java.util.Map<String, Object> opts = new java.util.HashMap<>();
                opts.put("num_ctx", numCtx);
                opts.put("num_predict", numPredict);
                opts.put("num_gpu", 0); // Viper hard rule: NO GPU
                if (temperature != null) opts.put("temperature", temperature);
                reqMap.put("options", opts);
                
                byte[] jsonBytes = mapper.writeValueAsBytes(reqMap);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", String.valueOf(jsonBytes.length));
                
                OutputStream os = con.getOutputStream();
                os.write(jsonBytes);
                os.flush();
                os.close();
                
                int respCode = con.getResponseCode();
                if (respCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while((line = in.readLine()) != null) response.append(line);
                    in.close();
                    
                    com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(response.toString());
                    if (rootNode.has("response")) {
                        String actualText = rootNode.get("response").asText().trim();
                        System.out.println(" -> Response from " + model + " acquired.");
                        return actualText;
                    }
                    return response.toString();
                } else {
                    BufferedReader errIn = new BufferedReader(new InputStreamReader(con.getErrorStream() != null ? con.getErrorStream() : con.getInputStream(), "UTF-8"));
                    StringBuilder errSb = new StringBuilder();
                    String errLine;
                    while((errLine = errIn.readLine()) != null) errSb.append(errLine);
                    errIn.close();
                    System.out.println("[OLLAMA ERROR] HTTP " + respCode + " Body: " + errSb.toString());
                    // 2026-11-05: was "YES", which made every failed call (codegemma 404s) count as an approving vote.
                    return "NO_RESPONSE";
                }
            } catch(Exception e) {
                // 2026-11-05: no more `ollama serve` from here. The Viper supervisor owns ollama (CPU-only env);
                // a bare spawn would start a second server without OLLAMA_LLM_LIBRARY=cpu, i.e. on the GPU.
                System.out.println("[OLLAMA DOWN] " + model + " - " + e.getMessage() + ". Supervisor relaunches ollama; skipping.");
            }
            return "NO_RESPONSE";
        }
    }

    private void unloadModel(String modelName) {
        // 2026-11-05: never purge a model the Viper hive is speaking with; that forces it to cold-load (~20 s).
        if (modelName.equals(boxMicModel())) {
            System.out.println("[CMG GATING] Keeping " + modelName + ": the Viper box mic is using it.");
            return;
        }
        System.out.println("[CMG GATING] Purging model from memory: " + modelName);
        try {
            URL url = new URL("http://127.0.0.1:11434/api/generate");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            
            // Empty prompt with keep_alive = 0 tells Ollama to unload
            String json = "{\"model\":\"" + modelName + "\", \"prompt\":\"\", \"keep_alive\":0, \"stream\":false}";
            
            OutputStream os = con.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.flush();
            os.close();
            
            int code = con.getResponseCode();
            System.out.println("[CMG GATING] Purge result code for " + modelName + ": " + code);
        } catch(Exception e) {
            System.out.println("[CMG GATING ERROR] Failed to purge model: " + e.getMessage());
        }
    }

    public void purgeVRAMCache() {
        synchronized (micLock) {
            if (activeModel != null) {
                unloadModel(activeModel);
                activeModel = null;
            }
        }
    }

    public static String getActiveModel() {
        return activeModel;
    }
}
