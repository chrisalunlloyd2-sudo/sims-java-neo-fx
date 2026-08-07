package com.aigen.sims;

import java.util.Arrays;

/**
 * SIMS1337 - StrainRatePhysicsKernel
 * Implements the Strain Rate & Rheological Physics Kernel:
 * 1. StrainRate primitive with 4-element ring buffer for stability smoothing
 * 2. Dynamic Viscosity binding: viscosity = base_viscosity * (1 + k * strain_rate)
 * 3. Stress computation: stress = viscosity * strain_rate
 * 4. Interstitial Cells: N cell memory snapshots absorbing stress (Cellular Microphone Gating)
 * 5. Fastmem load path: Instantly reloads last_stable cell state if stress > threshold
 * 6. CLI Command Interface: agent set strain, agent get stress, agent cell dump, agent load stable
 */
public class StrainRatePhysicsKernel {
    private double strainRate = 0.0;
    private double maxStrain = 10.0;
    private double[] strainRingBuffer = new double[4];
    private int ringIndex = 0;

    private double baseViscosity = 0.400;
    private double kSensitivity = 0.25;
    private double viscosity = 0.400;

    private double stress = 0.0;
    private double stressThreshold = 1.50;

    public static class InterstitialCell {
        public double strainRate;
        public double viscosity;
        public double stress;
        public long timestamp;

        public InterstitialCell() {
            this.strainRate = 0.0;
            this.viscosity = 0.400;
            this.stress = 0.0;
            this.timestamp = System.currentTimeMillis();
        }

        public void copyFrom(double sr, double visc, double str) {
            this.strainRate = sr;
            this.viscosity = visc;
            this.stress = str;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("{strain_rate: %.4f, viscosity: %.4f, stress: %.4f, timestamp: %d}",
                strainRate, viscosity, stress, timestamp);
        }
    }

    private final int N_CELLS = 16;
    private InterstitialCell[] cells = new InterstitialCell[N_CELLS];
    private int activeCellIdx = 0;
    private int lastStableIdx = 0;

    public static void main(String[] args) {
        StrainRatePhysicsKernel kernel = new StrainRatePhysicsKernel();
        System.out.println("=== STRAIN RATE PHYSICS KERNEL INITIALIZED ===");
        System.out.println(kernel.executeCliCommand("agent set strain 1.2"));
        System.out.println(kernel.executeCliCommand("agent get stress"));
        System.out.println(kernel.executeCliCommand("agent cell dump"));
        System.out.println(kernel.executeCliCommand("agent load stable"));
    }

    public StrainRatePhysicsKernel() {
        for (int i = 0; i < N_CELLS; i++) {
            cells[i] = new InterstitialCell();
        }
    }

    // Step 1: Update strain rate & ring buffer
    public synchronized void updateStrainRate(double deltaDeformation, double dt) {
        if (dt <= 0) dt = 0.02;
        double rawStrainRate = Math.abs(deltaDeformation / dt);
        this.strainRate = Math.min(Math.max(rawStrainRate, 0.0), maxStrain);

        // Ring buffer storage for stability smoothing
        strainRingBuffer[ringIndex] = this.strainRate;
        ringIndex = (ringIndex + 1) % strainRingBuffer.length;

        // Compute step 2, 3, 4 physics
        tickPhysics();
    }

    // Step 2 & 3: Viscosity & Stress Computation
    private void tickPhysics() {
        // Average strain rate from ring buffer for smooth stability
        double avgStrainRate = Arrays.stream(strainRingBuffer).average().orElse(strainRate);

        // Viscosity binding with dynamic resistance damping
        this.viscosity = baseViscosity * (1.0 + kSensitivity * avgStrainRate);

        // Stress computation (internal pressure signal)
        this.stress = this.viscosity * avgStrainRate;

        // Step 4: Interstitial Cell Snapshot Rotation
        cells[activeCellIdx].copyFrom(this.strainRate, this.viscosity, this.stress);
        activeCellIdx = (activeCellIdx + 1) % N_CELLS;

        // Step 5: Fastmem pointer update & auto-reload check
        if (this.stress <= stressThreshold) {
            lastStableIdx = (activeCellIdx - 1 + N_CELLS) % N_CELLS;
        } else {
            System.out.println(String.format("[STABILITY DAEMON] High Stress Detected: %.4f > Threshold %.4f. Triggering Fastmem Reload!", this.stress, stressThreshold));
            reloadLastStableState();
        }
    }

    // Step 5: Fastmem reload path
    public synchronized void reloadLastStableState() {
        InterstitialCell stable = cells[lastStableIdx];
        this.strainRate = stable.strainRate;
        this.viscosity = stable.viscosity;
        this.stress = stable.stress;
        Arrays.fill(strainRingBuffer, this.strainRate);
        System.out.println(String.format("[FASTMEM LOAD] Reloaded stable cell [%d]: %s", lastStableIdx, stable));
    }

    // Step 6: CLI Interface Processor
    public synchronized String executeCliCommand(String command) {
        if (command == null || command.isEmpty()) return "Error: empty command";
        String cmd = command.trim().toLowerCase();

        if (cmd.startsWith("agent set strain ")) {
            try {
                double deltaX = Double.parseDouble(cmd.replace("agent set strain ", "").trim());
                updateStrainRate(deltaX, 0.02);
                return String.format("[CLI] strain_rate updated to %.4f (stress: %.4f)", strainRate, stress);
            } catch (Exception e) {
                return "Error parsing strain value: " + e.getMessage();
            }
        } else if (cmd.equals("agent get stress")) {
            return String.format("Current Stress: %.4f (Viscosity: %.4f, Strain Rate: %.4f)", stress, viscosity, strainRate);
        } else if (cmd.equals("agent cell dump")) {
            StringBuilder sb = new StringBuilder("[CELL MEMORY DUMP]\n");
            for (int i = 0; i < N_CELLS; i++) {
                sb.append(String.format("Cell[%02d]%s: %s\n", i, (i == lastStableIdx ? " (STABLE)" : (i == activeCellIdx ? " (ACTIVE)" : "")), cells[i]));
            }
            return sb.toString();
        } else if (cmd.equals("agent load stable")) {
            reloadLastStableState();
            return "[CLI] Fastmem reload completed successfully.";
        }
        return "Unknown command: " + command;
    }

    // Getters
    public double getStrainRate() { return strainRate; }
    public double getViscosity() { return viscosity; }
    public double getStress() { return stress; }
}
