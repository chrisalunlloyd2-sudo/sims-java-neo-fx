package com.aigen.sims.engine;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SIMS1337 - TemplateSuccessDB
 * Phase 04: Programming Cube (Step 153)
 * Tracks the success rate of generated code templates across epochs.
 */
public class TemplateSuccessDB {

    public static class TemplateRecord {
        public String templateId;
        public int usageCount = 0;
        public int compileSuccessCount = 0;
        public int testSuccessCount = 0;

        public TemplateRecord(String id) {
            this.templateId = id;
        }

        public double getReliabilityScore() {
            if (usageCount == 0) return 0.0;
            return (compileSuccessCount + testSuccessCount) / (2.0 * usageCount);
        }
    }

    private final ConcurrentHashMap<String, TemplateRecord> db = new ConcurrentHashMap<>();

    public void logUsage(String templateId, boolean compiled, boolean tested) {
        TemplateRecord record = db.computeIfAbsent(templateId, TemplateRecord::new);
        synchronized (record) {
            record.usageCount++;
            if (compiled) record.compileSuccessCount++;
            if (tested) record.testSuccessCount++;
        }
    }

    public TemplateRecord getRecord(String templateId) {
        return db.get(templateId);
    }
}
