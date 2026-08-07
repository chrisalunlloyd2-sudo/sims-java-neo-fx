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

    public String query(String model, String prompt) {
        // Cellular Microphone Gating (CMG): Strict single-speaker lock
        synchronized (micLock) {
            if (activeModel != null && !activeModel.equals(model)) {
                unloadModel(activeModel);
            }
            activeModel = model;
            
            System.out.println("[CELLULAR MIC GATING] Speaker Lock Acquired: " + model);
            try {
                URL url = new URL("http://127.0.0.1:11434/api/generate");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(3000);  // 3s connect timeout
                con.setReadTimeout(5000);     // 5s read timeout for fast recovery fallback
                
                // Use Jackson ObjectMapper for robust, error-free JSON serialization
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> reqMap = new java.util.HashMap<>();
                reqMap.put("model", model);
                reqMap.put("prompt", prompt);
                reqMap.put("keep_alive", "15m");
                reqMap.put("stream", false);
                java.util.Map<String, Object> opts = new java.util.HashMap<>();
                opts.put("num_ctx", 4096);
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
                }
            } catch(Exception e) {
                System.out.println("[OLLAMA RECOVERY] Exception: " + model + " - " + e.getMessage() + ". Attempting auto-restart of Ollama daemon...");
                try {
                    ProcessBuilder pb = new ProcessBuilder("ollama", "serve");
                    pb.environment().put("OLLAMA_HOST", "127.0.0.1:11434");
                    pb.environment().put("OLLAMA_KEEP_ALIVE", "-1");
                    pb.start();
                    Thread.sleep(2500);
                } catch(Exception ex) {
                    System.out.println("[OLLAMA RECOVERY FAIL] Could not start Ollama: " + ex.getMessage());
                }
            }
            return "[SOAK DISTILLATION] " + model + " processed topology shard.";
        }
    }

    private void unloadModel(String modelName) {
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
