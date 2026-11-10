package com.aigen.sims.agents;

import com.aigen.sims.OllamaRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SLM Agent - Wrapper for Ollama model inference
 * Supports 4 model tiers with mmap optimization
 *
 * 2026-11-06: routed through OllamaRouter.query(...) (box-mic yield, options.num_gpu = 0,
 * NO_RESPONSE on error, adaptive pacing). The raw HttpClient /api/generate call is gone.
 * Default model qwen2.5-coder:0.5b: installed (397 MB), under the 1.5b local ceiling,
 * house-preferred for code. codellama:7b is not installed and over the ceiling, so it
 * can no longer reach Ollama through any SLM agent.
 */
public class SLMAgent {
    private static final Logger logger = LoggerFactory.getLogger(SLMAgent.class);

    /** House default: small, installed, under the 1.5b local ceiling, preferred for code. */
    public static final String DEFAULT_MODEL = "qwen2.5-coder:0.5b";
    private static final int DEFAULT_NUM_CTX = 2048;   // floor 2048, Chris 11-09

    private final String modelName;
    private final OllamaRouter router;
    private boolean isWarm;
    private final Map<String, Object> context;

    public SLMAgent(String modelName) {
        this.modelName = sanitizeModel(modelName);
        this.router = new OllamaRouter();
        this.isWarm = false;
        this.context = new HashMap<>();
    }

    /** codellama:7b is not installed and over the 1.5b local ceiling: it falls back to the house default. */
    static String sanitizeModel(String requested) {
        if (requested == null || requested.isBlank()) {
            logger.warn("No model requested; using default {}", DEFAULT_MODEL);
            return DEFAULT_MODEL;
        }
        if (requested.contains("codellama")) {
            logger.warn("Model '{}' is not installed and over the 1.5b local ceiling; using {}", requested, DEFAULT_MODEL);
            return DEFAULT_MODEL;
        }
        return requested;
    }
    
    /**
     * Warm up the model (load into RAM)
     */
    public void warmUp() {
        logger.info("Warming up model: {}", modelName);
        try {
            String prompt = "Ready";
            generate(prompt, 10);
            isWarm = true;
            logger.info("Model {} warmed up successfully", modelName);
        } catch (Exception e) {
            logger.error("Failed to warm up model {}: {}", modelName, e.getMessage());
        }
    }
    
    /**
     * Generate text from prompt.
     *
     * 2026-11-06: all traffic goes through the OllamaRouter's single mic-gated
     * generate call; num_predict = maxTokens, default 1024-token context.
     * Throws IOException when the router answers NO_RESPONSE (Ollama down or HTTP error),
     * matching the old failure contract.
     */
    public String generate(String prompt, int maxTokens) throws IOException, InterruptedException {
        String reply = router.query(modelName, prompt, maxTokens, null, DEFAULT_NUM_CTX);
        if ("NO_RESPONSE".equals(reply)) {
            throw new IOException("Ollama call failed for " + modelName + " (NO_RESPONSE from OllamaRouter)");
        }
        return reply;
    }
    
    /**
     * Set context for LoRA adapter switching
     */
    public void setContext(String key, Object value) {
        context.put(key, value);
    }
    
    /**
     * Get context value
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key) {
        return (T) context.get(key);
    }
    
    public String getModelName() {
        return modelName;
    }
    
    public boolean isWarm() {
        return isWarm;
    }
    
    public void clearContext() {
        context.clear();
    }
    
    @Override
    public String toString() {
        return String.format("SLMAgent{model='%s', warm=%s}", modelName, isWarm);
    }
}
