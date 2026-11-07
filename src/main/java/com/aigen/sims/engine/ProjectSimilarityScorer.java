package com.aigen.sims.engine;

import java.util.*;

/**
 * SIMS1337 - ProjectSimilarityScorer
 * Phase 04: Programming Cube & Orchestrator (Steps 155, 158)
 */
public class ProjectSimilarityScorer {

    public static class ProjectArchetype {
        public String id;
        public String topologyKind;
        public double syntaxPressure;
        public double dependencyPressure;
        public double topologyPressure;

        public ProjectArchetype(String id, String topologyKind, double sp, double dp, double tp) {
            this.id = id;
            this.topologyKind = topologyKind;
            this.syntaxPressure = sp;
            this.dependencyPressure = dp;
            this.topologyPressure = tp;
        }
    }

    private List<ProjectArchetype> historicalProjects = new ArrayList<>();

    public void registerProject(ProjectArchetype archetype) {
        historicalProjects.add(archetype);
    }

    public ProjectArchetype findMostSimilar(ProjectArchetype target) {
        ProjectArchetype bestMatch = null;
        double minDistance = Double.MAX_VALUE;

        for (ProjectArchetype past : historicalProjects) {
            if (!past.topologyKind.equals(target.topologyKind)) continue;

            double dS = past.syntaxPressure - target.syntaxPressure;
            double dD = past.dependencyPressure - target.dependencyPressure;
            double dT = past.topologyPressure - target.topologyPressure;

            double distance = Math.sqrt(dS * dS + dD * dD + dT * dT);
            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = past;
            }
        }

        return bestMatch;
    }
}
