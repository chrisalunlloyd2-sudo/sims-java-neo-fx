package com.aigen.sims;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GistSync {
    private final String gistToken;
    private final String gistId = "87a6e878"; // Neuromorphic lineage gist example

    public GistSync() {
        this.gistToken = System.getenv("GIST_TOKEN");
        if (this.gistToken == null) {
            System.err.println("[GIST SYNC] WARNING: GIST_TOKEN environment variable not set. Sync disabled.");
        }
    }

    public void pushState(Map<String, String> filesAndContent) {
        if (gistToken == null) return;
        System.out.println("[GIST SYNC] Pushing autonomous state to GitHub Gists...");
        try {
            URL url = new URL("https://api.github.com/gists/" + gistId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PATCH");
            conn.setRequestProperty("Authorization", "token " + gistToken);
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setDoOutput(true);

            // Construct JSON manually to avoid 3rd party deps for this class
            StringBuilder json = new StringBuilder("{\"files\":{");
            int count = 0;
            for (Map.Entry<String, String> entry : filesAndContent.entrySet()) {
                if (count++ > 0) json.append(",");
                String escapedContent = entry.getValue().replace("\"", "\\\"").replace("\n", "\\n");
                json.append("\"").append(entry.getKey()).append("\":{\"content\":\"").append(escapedContent).append("\"}");
            }
            json.append("}}");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("[GIST SYNC] GitHub API Response: " + responseCode);
        } catch (Exception e) {
            System.err.println("[GIST SYNC] Sync failed: " + e.getMessage());
        }
    }
}
