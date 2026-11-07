package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 2026-11-07: real data for the GodHand dashboard (batch 2, Hermes build order 1-3).
 *
 * The quorum grid lit cells at random, the stability index only ever counted up,
 * and the Interstitials view printed three hardcoded KQML messages. This polls the
 * real sources every 10 s on one daemon thread; render code reads the snapshot
 * fields only, so the FX thread never waits on the network or the disk.
 *
 * Read-only: hexflow :7766 /api/voting and /api/hive, and the C:\Viper\kqml_channel dirs.
 */
public final class ViperLiveFeed {

    private static final String HEXFLOW = "http://127.0.0.1:7766/api/";
    static final Path KQML = Paths.get("C:\\Viper\\kqml_channel");
    static final String[] QUEUES = {"inbox", "outbox", "to_kai"};
    private static final int POLL_S = 10;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    /** Latest quorum verdicts, newest decision first; null = hexflow not reached yet. */
    public static volatile boolean[] votes;
    /** Healthy share of the live (non-retired) hive cells, 0..1; NaN = unknown. */
    public static volatile double stability = Double.NaN;
    public static volatile String kqml = "[reading C:\\Viper\\kqml_channel ...]";
    public static volatile String lastError = "";

    private static ScheduledExecutorService poll;

    private ViperLiveFeed() {}

    public static synchronized void start() {
        if (poll != null) return;
        poll = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "viper-live-feed");
            t.setDaemon(true);
            return t;
        });
        poll.scheduleWithFixedDelay(ViperLiveFeed::refresh, 0, POLL_S, TimeUnit.SECONDS);
    }

    static void refresh() {
        List<String> errs = new ArrayList<>();
        JsonNode v = fetch("voting", errs);
        if (v != null) votes = votes(v, 64);
        JsonNode h = fetch("hive", errs);
        if (h != null) stability = stability(h);
        kqml = kqmlText(KQML, 4);
        lastError = String.join("; ", errs);
    }

    private static JsonNode fetch(String endpoint, List<String> errs) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(HEXFLOW + endpoint))
                    .timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) { errs.add(endpoint + " http " + resp.statusCode()); return null; }
            return JSON.readTree(resp.body());
        } catch (Exception e) {
            errs.add(endpoint + " " + e.getClass().getSimpleName());
            return null;
        }
    }

    /** passed flag of up to n decisions, in the order hexflow lists them. */
    static boolean[] votes(JsonNode voting, int n) {
        JsonNode ds = voting.path("decisions");
        int k = Math.min(n, ds.size());
        boolean[] out = new boolean[k];
        for (int i = 0; i < k; i++) out[i] = ds.get(i).path("passed").asBoolean(false);
        return out;
    }

    /** Healthy / live cells. Retired cells are neither healthy nor failing. */
    static double stability(JsonNode hive) {
        int live = 0, ok = 0;
        for (JsonNode c : hive.path("cells")) {
            if (c.path("retired").asBoolean(false)) continue;
            live++;
            if (c.path("consecutive_fails").asInt(0) == 0) ok++;
        }
        return live == 0 ? Double.NaN : (double) ok / live;
    }

    /** Real queue contents: counts per queue plus the newest few messages. Never invents traffic. */
    static String kqmlText(Path channel, int newest) {
        StringBuilder sb = new StringBuilder("--- KQML CHANNEL  " + channel + " ---\n");
        int total = 0;
        for (String q : QUEUES) {
            List<Path> files = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(channel.resolve(q), "*.json")) {
                for (Path p : ds) files.add(p);
            } catch (IOException e) {
                sb.append(String.format("%-7s unreadable (%s)%n", q, e.getClass().getSimpleName()));
                continue;
            }
            total += files.size();
            sb.append(String.format("%-7s %d message(s)%n", q, files.size()));
            files.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
            for (Path p : files.subList(0, Math.min(newest, files.size()))) {
                sb.append("  ").append(p.getFileName()).append('\n');
                try {
                    String acl = JSON.readTree(p.toFile()).path("acl").asText("");
                    if (!acl.isEmpty()) sb.append("    ").append(trim(acl.replace("\n", "\n    "), 360)).append('\n');
                } catch (IOException e) {
                    sb.append("    [unreadable json]\n");
                }
            }
        }
        if (total == 0) sb.append("\n[queue empty]\n");
        return sb.toString();
    }

    private static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 3) + "..."; }
}
