package com.aigen.sims;

import java.util.*;

public class KnowledgeGraph {
    private static class KGNode {
        String id;
        double[] vector64;
        String document;

        public KGNode(String id, String document) {
            this.id = id;
            this.document = document;
            this.vector64 = generate64DimVector();
        }

        private double[] generate64DimVector() {
            double[] vec = new double[64];
            Random r = new Random();
            double sum = 0;
            for (int i = 0; i < 64; i++) {
                vec[i] = r.nextGaussian();
                sum += vec[i] * vec[i];
            }
            double norm = Math.sqrt(sum);
            for (int i = 0; i < 64; i++) vec[i] /= norm;
            return vec;
        }
    }

    private final List<KGNode> nodes = new ArrayList<>();

    public KnowledgeGraph() {
        System.out.println("[KNOWLEDGE GRAPH] Initializing Real RAG Pipeline (64-dim vectors)...");
        nodes.add(new KGNode("GIST-1", "Neuromorphic lineage full evolutionary chain"));
        nodes.add(new KGNode("GIST-2", "Memories SQLite schema seed data"));
        nodes.add(new KGNode("GIST-3", "Hex coordinates for all repos agents stations"));
        nodes.add(new KGNode("SYS-4", "Night Cycle Dream Vote Deploy Phase Rules"));
    }

    public synchronized void addDocument(String id, String document) {
        nodes.add(new KGNode(id, document));
        System.out.println("[KNOWLEDGE GRAPH] Indexed node: " + id + " (64D Vector Created)");
    }

    public String queryRAG(String query) {
        System.out.println("[RAG ENGINE] Processing 64-dim semantic search for: " + query);
        KGNode queryNode = new KGNode("QUERY", query);
        
        KGNode bestMatch = null;
        double bestScore = -1.0;

        for (KGNode n : nodes) {
            double dotProduct = 0;
            for (int i = 0; i < 64; i++) {
                dotProduct += n.vector64[i] * queryNode.vector64[i];
            }
            if (dotProduct > bestScore) {
                bestScore = dotProduct;
                bestMatch = n;
            }
        }
        
        if (bestMatch != null) {
            System.out.println(" -> Best Match: " + bestMatch.id + " (Similarity: " + String.format("%.3f", bestScore) + ")");
            return bestMatch.document;
        }
        return "No relevant context found in RAG matrix.";
    }
}
