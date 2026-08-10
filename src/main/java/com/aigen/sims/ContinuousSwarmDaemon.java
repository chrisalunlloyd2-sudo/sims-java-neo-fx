package com.aigen.sims;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SIMS1337 - ContinuousSwarmDaemon
 * Continuous background daemon orchestrating ClosedLoopOrganism cycles every 30 seconds.
 */
public class ContinuousSwarmDaemon {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ClosedLoopOrganism organism = new ClosedLoopOrganism();

    public void startDaemon() {
        System.out.println("=================================================");
        System.out.println("[SWARM DAEMON] Starting Continuous Background Execution Swarm...");
        System.out.println("=================================================");

        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n[SWARM DAEMON TICK] Executing autonomous cycle tick...");
                organism.runAutonomousLoop();
            } catch (Exception e) {
                System.out.println("[SWARM DAEMON WARN] " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        ContinuousSwarmDaemon daemon = new ContinuousSwarmDaemon();
        daemon.startDaemon();
    }
}
