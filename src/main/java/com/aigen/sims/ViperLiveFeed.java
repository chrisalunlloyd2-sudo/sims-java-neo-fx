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
    /** Notes "Swarm Memory" tab: loaded models, mic, hive cells, latest votes. */
    public static volatile String swarm = "[reading ollama :11434 and hexflow :7766 ...]";

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
        JsonNode ps = fetchUrl(OLLAMA_PS, "ollama ps", errs);
        String mic = null;
        try { mic = OllamaRouter.boxMicModel(); } catch (Exception ignored) {}
        swarm = swarmText(ps, h, v, mic);
        lastError = String.join("; ", errs);
    }

    private static final String OLLAMA_PS = "http://127.0.0.1:11434/api/ps";

    // ---- one-line status for the tray menu (11-07); read the snapshot, never fetch ----
    static String quorumLine(boolean[] v) {
        if (v == null) return "Quorum: hexflow not reached";
        int p = 0;
        for (boolean b : v) if (b) p++;
        return "Quorum: " + p + " passed / " + v.length + " latest";
    }

    static String hiveLine(double s) {
        return Double.isNaN(s) ? "Hive: not reached" : String.format("Hive: %.0f%% of live cells healthy", s * 100);
    }

    // ---- daily archival (11-07): C:\Viper\scripts\daily-archival.py, nightly 01:00 + tray ----
    static final Path LOGS = Paths.get("C:\\Viper\\logs");
    static final String PY311 = "C:\\Users\\viper\\AppData\\Local\\Programs\\Python\\Python311\\python.exe";
    static final String ARCHIVAL = "C:\\Viper\\scripts\\daily-archival.py";

    /** Newest daily-archival-YYYYMMDD.log in dir, or null. */
    static Path latestArchivalLog(Path dir) {
        Path best = null;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "daily-archival-*.log")) {
            for (Path p : ds) if (best == null || p.getFileName().toString().compareTo(best.getFileName().toString()) > 0) best = p;
        } catch (IOException ignored) {}
        return best;
    }

    /** "Archive: last run 2026-11-07 01:06 DONE" from the newest log's last stamped line. */
    static String archivalLine(Path dir) {
        Path p = latestArchivalLog(dir);
        if (p == null) return "Archive: never run";
        try {
            List<String> lines = Files.readAllLines(p, java.nio.charset.StandardCharsets.UTF_8);
            for (int i = lines.size() - 1; i >= 0; i--) {
                String l = lines.get(i);
                if (l.startsWith("[") && l.length() > 21) {
                    String when = l.substring(1, 17).replace('T', ' ');
                    String what = l.contains("=== DONE ===") ? "done" : l.contains("in progress") ? "skipped (already running)" : "running: " + trim(l.substring(l.indexOf(']') + 1).trim(), 40);
                    return "Archive: " + when + " " + what;
                }
            }
            return "Archive: log empty";
        } catch (IOException e) {
            return "Archive: log unreadable";
        }
    }

    public static String archivalLine() { return archivalLine(LOGS); }

    /** Start daily-archival.py detached (it logs itself and holds its own one-run lock). */
    public static void runArchival(boolean dry) {
        try {
            List<String> cmd = new ArrayList<>(List.of(PY311, ARCHIVAL));
            if (dry) cmd.add("--dry-run");
            new ProcessBuilder(cmd).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException e) {
            System.out.println("[TRAY] daily archival failed to start: " + e);
        }
    }

    public static void openArchivalLog() {
        Path p = latestArchivalLog(LOGS);
        ViperTray.open(p == null ? LOGS.toString() : p.toString());
    }

    public static String quorumLine() { return quorumLine(votes); }
    public static String hiveLine() { return hiveLine(stability); }
    public static String micLine() {
        try { String m = OllamaRouter.boxMicModel(); return "Mic: " + (m == null ? "free" : "held by " + m); }
        catch (Exception e) { return "Mic: unreadable"; }
    }

    /** Plain report of what is actually running. Any null source is said, not filled in. */
    static String swarmText(JsonNode ps, JsonNode hive, JsonNode voting, String micModel) {
        StringBuilder sb = new StringBuilder("# LIVE SWARM  (refreshes every " + POLL_S + " s)\n");
        sb.append("Viper mic: ").append(micModel == null ? "free" : "held by " + micModel).append("\n\n");
        sb.append("Models loaded in ollama:\n");
        if (ps == null) sb.append("  [ollama :11434 not reached]\n");
        else if (ps.path("models").size() == 0) sb.append("  (none loaded)\n");
        else for (JsonNode m : ps.path("models"))
            sb.append(String.format("  %-28s %6.0f MB  vram %.0f MB%n", m.path("name").asText("?"),
                    m.path("size").asDouble(0) / 1048576.0, m.path("size_vram").asDouble(0) / 1048576.0));
        sb.append("\nHive cells:\n");
        if (hive == null) sb.append("  [hexflow :7766 not reached]\n");
        else {
            int live = 0, failing = 0;
            List<String> bad = new ArrayList<>();
            for (JsonNode c : hive.path("cells")) {
                if (c.path("retired").asBoolean(false)) continue;
                live++;
                if (c.path("consecutive_fails").asInt(0) > 0) { failing++; bad.add(c.path("cell").asText("?")); }
            }
            sb.append(String.format("  %d live, %d healthy, %d failing%n", live, live - failing, failing));
            if (!bad.isEmpty()) sb.append("  failing: ").append(trim(String.join(", ", bad), 300)).append('\n');
        }
        sb.append("\nLatest quorum decisions:\n");
        if (voting == null) sb.append("  [hexflow :7766 not reached]\n");
        else {
            JsonNode ds = voting.path("decisions");
            if (ds.size() == 0) sb.append("  (none)\n");
            for (int i = 0; i < Math.min(6, ds.size()); i++) {
                JsonNode d = ds.get(i);
                sb.append(String.format("  %s %-5s %s%n", d.path("t").asText(""),
                        d.path("passed").asBoolean(false) ? "PASS" : "FAIL",
                        d.path("module").asText(d.path("subject").asText("?"))));
            }
        }
        return sb.toString();
    }

    private static JsonNode fetch(String endpoint, List<String> errs) {
        return fetchUrl(HEXFLOW + endpoint, endpoint, errs);
    }

    private static JsonNode fetchUrl(String url, String endpoint, List<String> errs) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
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
