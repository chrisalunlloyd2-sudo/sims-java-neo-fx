package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Live view of the Viper hive inside GodHand: quorum verdicts, hive cells, mining jobs, the box mic.
 * Read-only. Everything comes from hexflow (:7766) and the mic files; no model calls, no writes.
 * 2026-11-05: Chris wants to "see quorum actually in a gui".
 */
public final class ViperHivePanel {
    private static final String HEXFLOW = "http://127.0.0.1:7766/api/";
    private static final Path MIC_JSON = Paths.get("C:\\Viper\\databases\\aegis\\cellular_mic\\mic.json");
    private static final Path MIC_LOG = Paths.get("C:\\Viper\\logs\\cellular_mic.jsonl");
    private static final int REFRESH_S = 15;
    private static final String TEXT = "-fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: 12px;";
    private static final String BOX = "-fx-control-inner-background: #0b0716; " + TEXT;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private ViperHivePanel() {}

    public static VBox create() {
        Label status = new Label("loading...");
        status.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 12px;");

        ListView<String> quorum = list();
        TextArea ballots = area();
        ballots.setPrefRowCount(9);
        List<JsonNode> decisions = new ArrayList<>();
        quorum.getSelectionModel().selectedIndexProperty().addListener((o, a, i) -> {
            int n = i.intValue();
            if (n >= 0 && n < decisions.size()) ballots.setText(ballotText(decisions.get(n)));
        });
        VBox quorumBox = new VBox(4, quorum, ballots);
        VBox.setVgrow(quorum, Priority.ALWAYS);

        TextArea cells = area();
        TextArea jobs = area();
        TextArea mic = area();

        TabPane tabs = new TabPane(
                tab("Quorum", quorumBox), tab("Cells", cells), tab("Jobs", jobs), tab("Mic", mic));
        VBox root = new VBox(6, status, tabs);
        root.setStyle("-fx-padding: 6;");
        VBox.setVgrow(tabs, Priority.ALWAYS);

        ScheduledExecutorService poll = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "viper-hive-panel");
            t.setDaemon(true);
            return t;
        });
        poll.scheduleWithFixedDelay(() -> {
            List<String> errors = new ArrayList<>();
            JsonNode voting = fetch("voting", errors);
            JsonNode hive = fetch("hive", errors);
            JsonNode mining = fetch("mining", errors);
            String micText = micText(errors);
            Platform.runLater(() -> {
                if (voting != null) {
                    int keep = quorum.getSelectionModel().getSelectedIndex();
                    decisions.clear();
                    List<String> rows = new ArrayList<>();
                    for (JsonNode d : voting.path("decisions")) {
                        decisions.add(d);
                        rows.add(decisionRow(d, voting.path("total_weight").asDouble()));
                    }
                    quorum.getItems().setAll(rows);
                    if (keep >= 0 && keep < rows.size()) quorum.getSelectionModel().select(keep);
                    else if (!rows.isEmpty()) quorum.getSelectionModel().select(0);
                }
                if (hive != null) cells.setText(cellsText(hive));
                if (mining != null) jobs.setText(jobsText(mining));
                mic.setText(micText);
                status.setText("updated " + LocalTime.now().withNano(0) + "  every " + REFRESH_S + " s"
                        + (errors.isEmpty() ? "" : "   ERR " + String.join("; ", errors)));
            });
        }, 0, REFRESH_S, TimeUnit.SECONDS);
        // Stop polling once the window is closed (removed from the scene graph).
        root.sceneProperty().addListener((o, was, now) -> { if (was != null && now == null) poll.shutdownNow(); });
        return root;
    }

    private static JsonNode fetch(String endpoint, List<String> errors) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(HEXFLOW + endpoint))
                    .timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) { errors.add(endpoint + " http " + resp.statusCode()); return null; }
            return JSON.readTree(resp.body());
        } catch (Exception e) {
            errors.add(endpoint + " " + e.getClass().getSimpleName());
            return null;
        }
    }

    static String decisionRow(JsonNode d, double total) {
        StringBuilder veto = new StringBuilder();
        for (JsonNode v : d.path("vetoed")) veto.append(veto.length() == 0 ? "" : ",").append(v.asText());
        return String.format("%s  %s  %4.1f/%-4.1f  %s%s",
                d.path("t").asText(), d.path("passed").asBoolean() ? "PASS" : "FAIL",
                d.path("approve_weight").asDouble(), total, d.path("module").asText(),
                veto.length() == 0 ? "" : "  VETO " + veto);
    }

    static String ballotText(JsonNode d) {
        StringBuilder sb = new StringBuilder(d.path("module").asText()).append("\n");
        for (JsonNode b : d.path("ballots")) {
            sb.append(String.format("%s %-15s %3.1f  %s%n", b.path("approve").asBoolean() ? "yes" : " NO",
                    b.path("voter").asText(), b.path("weight").asDouble(), b.path("why").asText()));
        }
        return sb.toString();
    }

    static String cellsText(JsonNode h) {
        StringBuilder sb = new StringBuilder(String.format("total %d   healthy %d   failing %d   retired %d%nthrottle %s%n%n",
                h.path("total").asInt(), h.path("healthy").asInt(), h.path("failing").asInt(),
                h.path("retired").asInt(), h.path("throttle")));
        List<JsonNode> live = new ArrayList<>();
        for (JsonNode c : h.path("cells")) if (!c.path("retired").asBoolean()) live.add(c);
        sb.append("FAILING NOW\n");
        int bad = 0;
        for (JsonNode c : live) {
            if (!c.path("last_ok").asBoolean(true)) {
                bad++;
                sb.append(String.format("  %-24s t%d x%d  %s%n", c.path("cell").asText(), c.path("tier").asInt(),
                        c.path("consecutive_fails").asInt(), trim(c.path("last_error").asText(), 70)));
            }
        }
        if (bad == 0) sb.append("  none\n");
        live.sort((a, b) -> Double.compare(b.path("last_secs").asDouble(), a.path("last_secs").asDouble()));
        sb.append("\nSLOWEST LAST RUN\n");
        for (int i = 0; i < Math.min(12, live.size()); i++) {
            JsonNode c = live.get(i);
            sb.append(String.format("  %-24s %7.1f s  %s%n", c.path("cell").asText(),
                    c.path("last_secs").asDouble(), c.path("last_run").asText()));
        }
        return sb.toString();
    }

    static String jobsText(JsonNode m) {
        StringBuilder sb = new StringBuilder(String.format("results %d   ok %d   pushes %d%n%nMINER (newest last)%n",
                m.path("result_count").asInt(), m.path("success_count").asInt(), m.path("push_total").asInt()));
        for (JsonNode j : m.path("daemon_log")) {
            sb.append(String.format("  %s %-6s tier %d %s %6.0f s  %s%n", j.path("ok").asBoolean() ? "ok " : "ERR",
                    j.path("action").asText(), j.path("best_tier").asInt(),
                    j.path("promoted").asBoolean() ? "PROMOTED" : "        ",
                    j.path("secs").asDouble(), trim(j.path("task").asText(), 60)));
        }
        return sb.toString();
    }

    private static String micText(List<String> errors) {
        StringBuilder sb = new StringBuilder("HOLDING NOW\n");
        try {
            JsonNode mic = JSON.readTree(Files.readString(MIC_JSON));
            sb.append("  model ").append(mic.path("model").asText("(none)")).append("\n");
            for (JsonNode h : mic.path("holders")) {
                sb.append(String.format("  pid %-6d tier %d  %s%n", h.path("pid").asInt(), h.path("tier").asInt(),
                        h.path("reason").asText()));
            }
        } catch (Exception e) {
            // mic.json is rewritten in place; a read mid-write is normal, try again next tick.
            sb.append("  (mid-write, next tick)\n");
        }
        sb.append("\nLAST EVENTS\n");
        try (RandomAccessFile f = new RandomAccessFile(MIC_LOG.toFile(), "r")) {
            long start = Math.max(0, f.length() - 6000);
            byte[] buf = new byte[(int) (f.length() - start)];
            f.seek(start);
            f.readFully(buf);
            String[] lines = new String(buf, StandardCharsets.UTF_8).split("\n");
            for (int i = Math.max(1, lines.length - 14); i < lines.length; i++) {
                try {
                    JsonNode e = JSON.readTree(lines[i]);
                    sb.append(String.format("  %s %-18s %-22s %s%n", e.path("ts").asText().substring(11),
                            e.path("event").asText(), e.path("model").asText(), e.path("reason").asText()));
                } catch (Exception partial) { /* torn last line */ }
            }
        } catch (Exception e) {
            errors.add("mic log " + e.getClass().getSimpleName());
        }
        return sb.toString();
    }

    private static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 3) + "..."; }

    private static ListView<String> list() {
        ListView<String> v = new ListView<>();
        v.setStyle(BOX);
        return v;
    }

    private static TextArea area() {
        TextArea a = new TextArea();
        a.setEditable(false);
        a.setWrapText(false);
        a.setStyle(BOX);
        VBox.setVgrow(a, Priority.ALWAYS);
        return a;
    }

    private static Tab tab(String name, javafx.scene.Node body) {
        Tab t = new Tab(name, body);
        t.setClosable(false);
        return t;
    }
}
