package com.aigen.sims;

import java.lang.management.ManagementFactory;

/**
 * Real box vitals for GodHand's simulations (2026-11-05). Cached for one second because the render loop
 * asks every frame. CPU load is the whole machine, not just this JVM.
 */
public final class ViperVitals {
    private static final com.sun.management.OperatingSystemMXBean OS =
        (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static long readAt = 0;
    private static double cpu = 0.0;
    private static double freeRamGb = 0.0;
    private static double totalRamGb = 0.0;

    private ViperVitals() {}

    private static synchronized void refresh() {
        long now = System.currentTimeMillis();
        if (now - readAt < 1000) return;
        readAt = now;
        double c = OS.getCpuLoad();
        if (!Double.isNaN(c) && c >= 0) cpu = c;
        freeRamGb = OS.getFreeMemorySize() / 1073741824.0;
        totalRamGb = OS.getTotalMemorySize() / 1073741824.0;
    }

    /** Whole-box CPU load, 0..1. */
    public static double cpu() { refresh(); return cpu; }
    public static double freeRamGb() { refresh(); return freeRamGb; }
    public static double totalRamGb() { refresh(); return totalRamGb; }
}
