package com.aigen.sims;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.scene.effect.Bloom;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Tabbed GodHand Controller - All GUIs in one window with tabs.
 *
 * Hardened 2026-11-06: all fake/placeholder data replaced with live API calls.
 * - Models tab: polls Ollama /api/tags every 15s for real model list
 * - Queue: polls hexflow /api/mining for real task counts
 * - Chat: calls Ollama /api/chat for real model responses
 * - Adapters: honest "no LoRA adapters loaded" (none are)
 * - Players: replaced with live hive cell summary from hexflow
 * - Facilities: labels reflect actual Viper subsystems with real port status
 */
public class TabbedGodHandController {
    private static final Logger logger = LoggerFactory.getLogger(TabbedGodHandController.class);

    private static final String OLLAMA = "http://127.0.0.1:11434";
    private static final String HEXFLOW = "http://127.0.0.1:7766/api";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int REFRESH_S = 15;
    private static final String CONFIG_DIR = "C:/Viper/databases/godhand";
    
    @FXML private Label statusLabel;
    @FXML private TabPane mainTabPane;
    @FXML private VBox modelPanel, adapterPanel, queuePanel;
    @FXML private GridPane simpleGrid;
    @FXML private HBox skylineBox;
    @FXML private VBox playerPanel, chatPanel, chatBox, facilitiesPanel;
    @FXML private ScrollPane chatScroll;
    @FXML private TextField chatInput;
    @FXML private ComboBox<String> chatModelBox;   // wired 2026-11-09 (was controller-only, no FXML)
    @FXML private Button chatSendButton;

    private final ScheduledExecutorService poll = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "godhand-live-poll");
        t.setDaemon(true);
        return t;
    });
    // Chat gets its OWN executor: a slow response (box mic churn can take 200 s)
    // must never stall the 15 s live polling that drives the other tabs.
    private final ScheduledExecutorService chatExec = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "godhand-chat");
        t.setDaemon(true);
        return t;
    });
    
    // Simple 10x10 grid
    private static final int GRID_SIZE = 10;
    
    @FXML
    public void initialize() {
        logger.info("Tabbed GodHand initialized - 3 tabs (live data mode)");
        
        // Tab 1: GodHand
        initGodHandTab();
        
        // Tab 2: Player Grid → Hive Cell Grid
        initPlayerGridTab();
        
        // Tab 3: Facilities
        initFacilitiesTab();
        
        logger.info("All tabs initialized - starting live polling");
        
        // Start live data polling
        startLivePolling();
    }
    
    private void initGodHandTab() {
        logger.info("Initializing GodHand tab (live mode)...");
        
        // Placeholder until first poll arrives
        Label loading = new Label("Loading models from Ollama...");
        loading.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 13px;");
        modelPanel.getChildren().add(loading);

        Label adapterInfo = new Label("No LoRA adapters loaded");
        adapterInfo.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 12px;");
        adapterPanel.getChildren().add(adapterInfo);

        Label queueLoading = new Label("Loading queue stats from hexflow...");
        queueLoading.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 13px;");
        queuePanel.getChildren().add(queueLoading);
    }
    
    private void initPlayerGridTab() {
        logger.info("Initializing Hive Cell Grid tab...");
        
        // Placeholder until first poll arrives
        Label loading = new Label("Loading hive cells from hexflow...");
        loading.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 14px;");
        playerPanel.getChildren().add(loading);
    }
    
    // Old buildSimpleGrid/buildSkyline/addPlayers removed - replaced by live hive cell polling
    
    private void initFacilitiesTab() {
        logger.info("Initializing Facilities tab (live mode)...");
        Label loading = new Label("Loading Viper subsystem status...");
        loading.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 13px;");
        facilitiesPanel.getChildren().add(loading);
    }
    
    @FXML
    private void sendChat() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;

        final String model = (chatModelBox.getValue() == null || chatModelBox.getValue().isBlank())
                ? "tinyllama:1.1b" : chatModelBox.getValue();
        final String msg = message;

        addChatMessage("You", message);
        chatInput.clear();
        chatSendButton.setDisable(true);
        addChatMessage("System",
                String.format("thinking (model=%s, may take a while under mic load)…", model));

        // Own executor: slow chat never stalls live tab polling
        CompletableFuture.runAsync(() -> {
            try {
                String response = callOllamaChat(msg, model);
                Platform.runLater(() -> {
                    removeLastSystemLine();
                    addChatMessage("ollama", response);
                    chatSendButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    removeLastSystemLine();
                    addChatMessage("System", "Ollama error: " + e.getMessage());
                    chatSendButton.setDisable(false);
                });
            }
        }, chatExec);
    }

    /** Dedupe: drop the newest "System:" line (the thinking… notice) once answered. */
    private void removeLastSystemLine() {
        var children = chatBox.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            if (children.get(i) instanceof Label l
                    && l.getText() != null && l.getText().startsWith("[System:")) {
                // keep history for context but mark it answered so it reads cleanly
                l.setText(l.getText() + "  ✓ answered");
                break;
            }
        }
    }
    
    private String callOllamaChat(String userMessage, String model) {
        try {
            int timeoutMs = 240_000;   // mic churn can legitimately take 200 s+
            String body = JSON.writeValueAsString(Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", userMessage)),
                "stream", false,
                "options", Map.of("num_gpu", 0, "num_predict", 100)
            ));

            HttpURLConnection conn = (HttpURLConnection) new URL(OLLAMA + "/api/chat").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(timeoutMs);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != 200) {
                return "[HTTP " + conn.getResponseCode() + "]";
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JsonNode resp = JSON.readTree(sb.toString());
                String content = resp.path("message").path("content").asText("(no response)");
                if (content.length() > 300) content = content.substring(0, 297) + "...";
                return content.trim();
            }
        } catch (Exception e) {
            return "[error: " + e.getClass().getSimpleName() + "]";
        }
    }
    
    private void simulateModelChat() {
        // Periodically show live hive events instead of fake chat
        javafx.animation.AnimationTimer timer = new javafx.animation.AnimationTimer() {
            private long lastChat = 0;
            private String lastStatus = "";   // de-spam: only post when the summary CHANGES

            @Override
            public void handle(long now) {
                if (now - lastChat > 15_000_000_000L) {
                    try {
                        JsonNode hive = fetchJson(HEXFLOW + "/hive");
                        if (hive != null) {
                            int total = hive.path("total").asInt(0);
                            int healthy = hive.path("healthy").asInt(0);
                            int failing = hive.path("failing").asInt(0);
                            String s = String.format("status: %d cells, %d healthy, %d failing", total, healthy, failing);
                            if (!s.equals(lastStatus)) {
                                addChatMessage("hive", s);
                                lastStatus = s;
                            }
                        } else if (!"DOWN".equals(lastStatus)) {
                            addChatMessage("System", "hexflow unreachable");
                            lastStatus = "DOWN";
                        }
                        // unchanged -> silence (was: re-posted every 15 s)
                    } catch (Exception e) {
                        // silently skip
                    }
                    lastChat = now;
                }
            }
        };
        timer.start();
    }
    
    private void addChatMessage(String sender, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        Label msgLabel = new Label("[" + timestamp + "] " + sender + ": " + message);
        msgLabel.setStyle(getSenderStyle(sender));
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(280);
        
        chatBox.getChildren().add(msgLabel);
        chatScroll.setVvalue(1.0);
        
        logger.debug("Chat: {} - {}", sender, message);
    }
    
    private String getSenderStyle(String sender) {
        return switch (sender) {
            case "System" -> "-fx-text-fill: #a0a0a0; -fx-font-size: 11px;";
            case "You" -> "-fx-text-fill: #00ff88; -fx-font-size: 11px; -fx-font-weight: bold;";
            case "ollama" -> "-fx-text-fill: #00d9ff; -fx-font-size: 11px;";
            case "hive" -> "-fx-text-fill: #c77dff; -fx-font-size: 11px;";
            default -> "-fx-text-fill: #ffffff; -fx-font-size: 11px;";
        };
    }

    // ── Live polling engine ────────────────────────────────────────────────

    private void startLivePolling() {
        poll.scheduleWithFixedDelay(() -> {
            try {
                refreshModelsTab();
                refreshQueueTab();
                refreshHiveCells();
                refreshFacilities();
                Platform.runLater(() -> {
                    if (statusLabel != null) {
                        statusLabel.setText("Live data updated " + LocalTime.now().withNano(0) + "  (every " + REFRESH_S + "s)");
                    }
                });
            } catch (Exception e) {
                logger.error("Polling error", e);
            }
        }, 2, REFRESH_S, TimeUnit.SECONDS);

        chatModelBox.setOnAction(e -> persistChatModelChoice());
        loadChatModelChoice();
        refreshChatModelList();
        poll.scheduleWithFixedDelay(
                () -> Platform.runLater(this::refreshChatModelList), 5, 60, TimeUnit.SECONDS);
    }

    private void refreshModelsTab() {
        try {
            JsonNode tags = fetchJson(OLLAMA + "/api/tags");
            if (tags == null) {
                Platform.runLater(() -> {
                    modelPanel.getChildren().clear();
                    Label err = new Label("Ollama unreachable at " + OLLAMA);
                    err.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    modelPanel.getChildren().add(err);
                });
                return;
            }

            List<String> modelNames = new ArrayList<>();
            for (JsonNode m : tags.path("models")) {
                String name = m.path("name").asText("(unknown)");
                long sizeBytes = m.path("size").asLong(0);
                double sizeGb = sizeBytes / 1073741824.0;
                String sizeStr = sizeGb >= 1.0 ? String.format("%.1f GB", sizeGb) : String.format("%d MB", sizeBytes / 1048576);
                modelNames.add(String.format("%s  (%s)", name, sizeStr));
            }

            Platform.runLater(() -> {
                modelPanel.getChildren().clear();
                if (modelNames.isEmpty()) {
                    Label none = new Label("No models installed");
                    none.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 13px;");
                    modelPanel.getChildren().add(none);
                } else {
                    for (String name : modelNames) {
                        Label lbl = new Label("🟢 " + name);
                        lbl.setStyle("-fx-text-fill: #00ff88; -fx-font-size: 13px;");
                        modelPanel.getChildren().add(lbl);
                    }
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to refresh models: {}", e.getMessage());
        }
    }

    private void refreshQueueTab() {
        try {
            JsonNode mining = fetchJson(HEXFLOW + "/mining");
            if (mining == null) {
                Platform.runLater(() -> {
                    queuePanel.getChildren().clear();
                    Label err = new Label("Hexflow mining API unreachable");
                    err.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    queuePanel.getChildren().add(err);
                });
                return;
            }

            int results = mining.path("result_count").asInt(0);
            int ok = mining.path("success_count").asInt(0);
            int pushes = mining.path("push_total").asInt(0);
            int activeTasks = 0;
            for (JsonNode j : mining.path("daemon_log")) {
                if (j.path("ok").asBoolean(false)) activeTasks++;
            }

            JsonNode m = mining;
            int finalActive = activeTasks;
            Platform.runLater(() -> {
                queuePanel.getChildren().clear();
                Label stats = new Label(String.format("Results: %d | OK: %d | Pushes: %d | Active miners: %d", results, ok, pushes, finalActive));
                stats.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px;");
                queuePanel.getChildren().add(stats);

                Label recentHdr = new Label("Recent mining jobs:");
                recentHdr.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
                queuePanel.getChildren().add(recentHdr);

                try {
                    int shown = 0;
                    for (JsonNode j : m.path("daemon_log")) {
                        if (shown >= 8) break;
                        String action = j.path("action").asText("?");
                        int tier = j.path("best_tier").asInt(0);
                        double secs = j.path("secs").asDouble(0);
                        boolean promoted = j.path("promoted").asBoolean(false);
                        String task = j.path("task").asText("");
                        if (task.length() > 50) task = task.substring(0, 47) + "...";

                        Label job = new Label(String.format("  %s tier%d %s %5.0fs  %s",
                                action, tier, promoted ? "PROMOTED" : "", secs, task));
                        job.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 11px; -fx-font-family: monospace;");
                        queuePanel.getChildren().add(job);
                        shown++;
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            logger.warn("Failed to refresh queue: {}", e.getMessage());
        }
    }

    private void refreshHiveCells() {
        try {
            JsonNode hive = fetchJson(HEXFLOW + "/hive");
            if (hive == null) {
                Platform.runLater(() -> {
                    playerPanel.getChildren().clear();
                    Label err = new Label("Hexflow hive API unreachable");
                    err.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                    playerPanel.getChildren().add(err);
                });
                return;
            }

            int total = hive.path("total").asInt(0);
            int healthy = hive.path("healthy").asInt(0);
            int failing = hive.path("failing").asInt(0);
            int retired = hive.path("retired").asInt(0);

            List<JsonNode> live = new ArrayList<>();
            for (JsonNode c : hive.path("cells")) {
                if (!c.path("retired").asBoolean(false)) live.add(c);
            }
            live.sort((a, b) -> {
                int t = Integer.compare(a.path("tier").asInt(99), b.path("tier").asInt(99));
                if (t != 0) return t;
                return a.path("cell").asText().compareTo(b.path("cell").asText());
            });

            Platform.runLater(() -> {
                playerPanel.getChildren().clear();
                Label summary = new Label(String.format("Hive: %d total, %d healthy, %d failing, %d retired", total, healthy, failing, retired));
                summary.setStyle("-fx-text-fill: #00d9ff; -fx-font-size: 14px; -fx-font-weight: bold;");
                playerPanel.getChildren().add(summary);

                JsonNode throttle = hive.path("throttle");
                if (!throttle.isMissingNode()) {
                    double factor = throttle.path("factor").asDouble(1.0);
                    String reason = throttle.path("reason").asText("");
                    String thrText = factor > 1.0 ?
                            String.format("THROTTLE: x%.1f (%s)", factor, reason.substring(0, Math.min(40, reason.length()))) :
                            "Throttle: nominal";
                    Label thrLabel = new Label(thrText);
                    thrLabel.setStyle(factor > 3.0 ?
                            "-fx-text-fill: #ff6b6b; -fx-font-size: 11px;" :
                            "-fx-text-fill: #446644; -fx-font-size: 11px;");
                    playerPanel.getChildren().add(thrLabel);
                }

                int shown = 0;
                int currentTier = -1;
                for (JsonNode c : live) {
                    if (shown >= 30) break;
                    int tier = c.path("tier").asInt(0);
                    if (tier != currentTier) {
                        currentTier = tier;
                        Label tierHdr = new Label("-- Tier " + tier + " --");
                        tierHdr.setStyle("-fx-text-fill: #c77dff; -fx-font-size: 11px; -fx-font-weight: bold;");
                        playerPanel.getChildren().add(tierHdr);
                    }

                    String name = c.path("cell").asText("?");
                    boolean ok = c.path("last_ok").asBoolean(true);
                    int fails = c.path("consecutive_fails").asInt(0);
                    double secs = c.path("last_secs").asDouble(0);

                    String icon = ok ? "OK" : "!!";
                    String detail = String.format("%s %-24s %5.0fs  fails=%d", icon, name, secs, fails);
                    Label cellLabel = new Label(detail);
                    cellLabel.setStyle(ok ?
                            "-fx-text-fill: #c0c0c0; -fx-font-size: 11px; -fx-font-family: monospace;" :
                            "-fx-text-fill: #ff6b6b; -fx-font-size: 11px; -fx-font-family: monospace;");
                    playerPanel.getChildren().add(cellLabel);
                    shown++;
                }

                if (live.size() > 30) {
                    Label more = new Label("... and " + (live.size() - 30) + " more cells");
                    more.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");
                    playerPanel.getChildren().add(more);
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to refresh hive cells: {}", e.getMessage());
        }
    }

    private void refreshFacilities() {
        String[][] subsystems = {
            {"hexflow", "7766", "Hive dashboard & workflow API"},
            {"moe_server", "8765", "Main inference / chat backend"},
            {"wave_server", "8788", "Learning inference (WAVE layer)"},
            {"ollama", "11434", "Local model inference engine"},
            {"gateway", "8800", "API gateway"},
            {"aegis_web", "5005", "MOA web UI"},
        };

        List<String[]> results = new ArrayList<>();
        for (String[] sub : subsystems) {
            boolean alive = portAlive(Integer.parseInt(sub[1]));
            results.add(new String[]{sub[0], alive ? "UP" : "DOWN", sub[1], sub[2]});
        }

        Platform.runLater(() -> {
            facilitiesPanel.getChildren().clear();

            for (String[] r : results) {
                String name = r[0];
                boolean up = "UP".equals(r[1]);
                String port = r[2];
                String desc = r[3];

                VBox card = new VBox(6);
                card.setStyle("-fx-background-color: #16213e; -fx-padding: 12; -fx-background-radius: 8; -fx-min-width: 200;");

                Label nameLbl = new Label((up ? "ON " : "OFF ") + name + " (:" + port + ")");
                nameLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + (up ? "#00ff88" : "#ff6b6b") + ";");

                Label descLbl = new Label(desc);
                descLbl.setStyle("-fx-text-fill: #a0a0a0; -fx-font-size: 11px;");

                card.getChildren().addAll(nameLbl, descLbl);
                facilitiesPanel.getChildren().add(card);
            }

            Label note = new Label("Ports probed every " + REFRESH_S + "s. Read-only.");
            note.setStyle("-fx-text-fill: #606060; -fx-font-size: 10px;");
            facilitiesPanel.getChildren().add(note);
        });
    }

    // ── Chat: model picker helpers (added 2026-11-09) ─────────────────────

    /** Live model list from Ollama, tinyllama sorted first, persisted choice kept. */
    private void refreshChatModelList() {
        try {
            JsonNode tags = fetchJson(OLLAMA + "/api/tags");
            if (tags == null) return;
            List<String> names = new ArrayList<>();
            for (JsonNode m : tags.path("models")) names.add(m.path("name").asText(""));
            names.removeIf(String::isBlank);
            names.sort(Comparator.comparing(
                    (String n) -> n.contains("tinyllama") ? 0
                            : n.matches(".*\\b[0-9.]+b\\b.*") && n.contains("0.5b") ? 1 : 2)
                    .thenComparing(Comparator.naturalOrder()));
            String chosen = chatModelBox.getValue() != null ? chatModelBox.getValue() : loadChatModelChoice();
            Platform.runLater(() -> {
                String cur = chatModelBox.getValue();
                chatModelBox.getItems().setAll(names);
                // keep the user's pick across refreshes; fall back to saved choice / tinyllama
                if (cur != null && names.contains(cur)) chatModelBox.setValue(cur);
                else chatModelBox.setValue(chosen != null && names.contains(chosen) ? chosen
                        : names.stream().filter(n -> n.contains("tinyllama")).findFirst().orElse(names.isEmpty() ? null : names.get(0)));
            });
        } catch (Exception e) {
            logger.debug("chat model list refresh failed: {}", e.getMessage());
        }
    }

    private void persistChatModelChoice() {
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(CONFIG_DIR));
            java.nio.file.Files.writeString(java.nio.file.Path.of(CONFIG_DIR, "chat_model.txt"),
                    String.valueOf(chatModelBox.getValue()));
        } catch (Exception e) {
            logger.debug("chat model choice not persisted: {}", e.getMessage());
        }
    }

    private String loadChatModelChoice() {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of(CONFIG_DIR, "chat_model.txt")).trim();
        } catch (Exception e) {
            return null;
        }
    }

    // ── HTTP utilities ─────────────────────────────────────────────────────

    private static JsonNode fetchJson(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() != 200) return null;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return JSON.readTree(sb.toString());
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean portAlive(int port) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            return code > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
