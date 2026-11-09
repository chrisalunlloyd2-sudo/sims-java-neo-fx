package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2026-11-08 (roadmap phase 7, steps 59-60): a tiny always-on-top hex "chip" above the tray.
 *
 * Shows the hive throttle (x1.0 ...) and free RAM, coloured green / amber / red:
 *   red   = free RAM < 2 GB, hive heartbeat older than 5 min, hive stuck, throttle >= 2.5, or tier ceiling <= 1
 *   amber = free RAM < 3 GB, throttle >= 1.5, or tier ceiling 2 (tier-3 cells shed)
 * When not green the tooltip names why, plus the top 3 RAM users. Left click = show GodHand,
 * drag = move, right click = menu (hide chip, hive log). Lives inside GodHand: no extra process.
 * Reads C:\Viper\databases\aegis\hive_running.json (the hive writes it); never writes anything.
 */
public final class ViperChip {
    static final Path HIVE = Paths.get("C:\\Viper\\databases\\aegis\\hive_running.json");
    private static final ObjectMapper M = new ObjectMapper();
    private static Stage chip;

    private ViperChip() {}

    /** Pure verdict. Returns {level, reason}. ageS = NaN when the heartbeat could not be read. */
    static String[] verdict(double throttle, long freeMb, double ageS, boolean stuck, int ceiling) {
        List<String> red = new ArrayList<>(), amber = new ArrayList<>();
        if (freeMb >= 0 && freeMb < 2048) red.add("RAM " + freeMb + " MB free (< 2 GB)");
        else if (freeMb >= 0 && freeMb < 3072) amber.add("RAM " + freeMb + " MB free (< 3 GB)");
        if (Double.isNaN(ageS)) red.add("hive heartbeat unreadable");
        else if (ageS > 300) red.add("hive heartbeat " + Math.round(ageS / 60) + " min old");
        if (stuck) red.add("hive cell stuck");
        if (throttle >= 2.5) red.add(String.format("throttle x%.2f", throttle));
        else if (throttle >= 1.5) amber.add(String.format("throttle x%.2f", throttle));
        if (ceiling > 0 && ceiling <= 1) red.add("tier ceiling " + ceiling + " (tiers 2-3 shed)");
        else if (ceiling == 2) amber.add("tier ceiling 2 (tier 3 shed)");
        if (!red.isEmpty()) { red.addAll(amber); return new String[]{"red", String.join("; ", red)}; }
        if (!amber.isEmpty()) return new String[]{"amber", String.join("; ", amber)};
        return new String[]{"green", "all clear"};
    }

    static Color color(String level) {
        return "red".equals(level) ? Color.web("#ef4444") : "amber".equals(level) ? Color.web("#f59e0b") : Color.web("#22c55e");
    }

    /** Free physical RAM in MB, -1 if unknown. */
    static long freeMb() {
        try {
            java.lang.management.OperatingSystemMXBean os = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            return ((com.sun.management.OperatingSystemMXBean) os).getFreeMemorySize() / 1048576L;
        } catch (Throwable t) { return -1; }
    }

    /** Pure: parse `tasklist /fo csv /nh` lines -> top n images by summed memory, "name 1.2 GB". */
    static List<String> topRam(List<String> csvLines, int n) {
        Map<String, Long> by = new HashMap<>();
        for (String ln : csvLines) {
            String[] f = ln.split("\",\"");
            if (f.length < 5) continue;
            String name = f[0].replace("\"", "");
            String digits = f[4].replaceAll("[^0-9]", "");
            if (digits.isEmpty()) continue;
            by.merge(name, Long.parseLong(digits), Long::sum);          // KB
        }
        List<String> out = new ArrayList<>();
        by.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(n)
                .forEach(e -> out.add(String.format("%s %.1f GB", e.getKey(), e.getValue() / 1048576.0)));
        return out;
    }

    static List<String> topRamNow() {
        List<String> lines = new ArrayList<>();
        try {
            Process p = new ProcessBuilder("tasklist", "/fo", "csv", "/nh").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String ln;
                while ((ln = r.readLine()) != null) lines.add(ln);
            }
        } catch (Exception e) { return lines; }
        return topRam(lines, 3);
    }

    /** {throttle, ageS, stuck(0/1), ceiling} from the hive heartbeat; ageS NaN when unreadable. */
    static double[] hive() {
        try {
            JsonNode h = M.readTree(Files.readAllBytes(HIVE));
            double age = java.time.Duration.between(LocalDateTime.parse(h.path("ts").asText()), LocalDateTime.now()).getSeconds();
            return new double[]{h.path("throttle_factor").asDouble(1.0), age, h.path("stuck").asBoolean(false) ? 1 : 0,
                    h.path("tier_ceiling").asInt(3)};
        } catch (Exception e) { return new double[]{1.0, Double.NaN, 0, 3}; }
    }

    private static void draw(Canvas c, String level, String top, String bottom) {
        GraphicsContext g = c.getGraphicsContext2D();
        double w = c.getWidth(), r = w / 2 - 2, cx = w / 2, cy = w / 2;
        g.clearRect(0, 0, w, w);
        double[] xs = new double[6], ys = new double[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3 * i + Math.PI / 6;
            xs[i] = cx + r * Math.cos(a);
            ys[i] = cy + r * Math.sin(a);
        }
        g.setFill(Color.web("#0b0716", 0.92));
        g.fillPolygon(xs, ys, 6);
        g.setStroke(color(level));
        g.setLineWidth(3);
        g.strokePolygon(xs, ys, 6);
        g.setFill(color(level));
        g.setTextAlign(TextAlignment.CENTER);
        double k = w / 64.0;                                   // text follows the chip size (step 61)
        g.setFont(Font.font("Consolas", FontWeight.BOLD, 13 * k));
        g.fillText(top, cx, cy - 1 * k);
        g.setFont(Font.font("Consolas", 10 * k));
        g.setFill(Color.web("#e2e8f0"));
        g.fillText(bottom, cx, cy + 13 * k);
    }

    /** Call once from start() on the FX thread. Never throws. */
    public static void install(Stage main) {
        try {
            Stage owner = new Stage(StageStyle.UTILITY);         // hidden owner = no taskbar button for the chip
            owner.setOpacity(0);
            owner.setWidth(1);
            owner.setHeight(1);
            owner.setX(-100);
            owner.setY(-100);
            owner.show();
            chip = new Stage(StageStyle.TRANSPARENT);
            chip.initOwner(owner);
            chip.setAlwaysOnTop(true);
            Canvas c = new Canvas(64, 64);
            StackPane root = new StackPane(c);
            root.setStyle("-fx-background-color: transparent;");
            Scene sc = new Scene(root, 64, 64, Color.TRANSPARENT);
            chip.setScene(sc);
            Rectangle2D vb = Screen.getPrimary().getVisualBounds();
            chip.setX(vb.getMaxX() - 76);
            chip.setY(vb.getMaxY() - 76);
            Tooltip tip = new Tooltip("...");
            tip.setShowDelay(Duration.millis(200));
            Tooltip.install(root, tip);
            ContextMenu menu = new ContextMenu();
            MenuItem show = new MenuItem("Show GodHand");
            show.setOnAction(e -> ViperTray.show(main));
            MenuItem log = new MenuItem("Open hive log");
            log.setOnAction(e -> ViperTray.open("C:\\Viper\\logs\\hive.log"));
            MenuItem hide = new MenuItem("Hide chip (until GodHand restarts)");
            hide.setOnAction(e -> chip.hide());
            menu.getItems().addAll(show, log, hide);
            double[] drag = new double[4];
            root.setOnMousePressed(e -> { drag[0] = e.getScreenX(); drag[1] = e.getScreenY(); drag[2] = chip.getX(); drag[3] = chip.getY(); });
            root.setOnMouseDragged(e -> { chip.setX(drag[2] + e.getScreenX() - drag[0]); chip.setY(drag[3] + e.getScreenY() - drag[1]); });
            root.setOnMouseClicked(e -> {
                boolean moved = Math.abs(e.getScreenX() - drag[0]) + Math.abs(e.getScreenY() - drag[1]) > 4;
                if (e.getButton() == MouseButton.SECONDARY) menu.show(root, e.getScreenX(), e.getScreenY());
                else if (!moved) ViperTray.show(main);
            });
            Runnable refresh = () -> new Thread(() -> {
                double[] h = hive();
                long free = freeMb();
                String[] v = verdict(h[0], free, h[1], h[2] > 0, (int) h[3]);
                String tipText = "Hive throttle x" + String.format("%.2f", h[0]) + ", tier ceiling " + (int) h[3]
                        + "\nFree RAM " + (free < 0 ? "?" : String.format("%.1f GB", free / 1024.0))
                        + "\n" + v[0].toUpperCase() + ": " + v[1];
                if (!"green".equals(v[0])) tipText += "\nTop RAM: " + String.join(", ", topRamNow());
                final String tt = tipText;
                final String bottom = free < 0 ? "" : String.format("%.1fG", free / 1024.0);
                javafx.application.Platform.runLater(() -> {
                    double size = Math.round(64 * ViperUi.scale());
                    if (size != c.getWidth()) {                     // large-text toggled: grow/shrink, keep the corner
                        double right = chip.getX() + c.getWidth(), lower = chip.getY() + c.getHeight();
                        c.setWidth(size); c.setHeight(size);
                        chip.setWidth(size); chip.setHeight(size);
                        chip.setX(right - size); chip.setY(lower - size);
                    }
                    tip.setStyle("-fx-font-size: " + ViperUi.px(12) + "px;");
                    draw(c, v[0], String.format("x%.1f", h[0]), bottom);
                    tip.setText(tt);
                });
            }, "viper-chip").start();
            refresh.run();
            Timeline t = new Timeline(new KeyFrame(Duration.seconds(10), e -> refresh.run()));
            t.setCycleCount(Timeline.INDEFINITE);
            t.play();
            chip.show();
            System.out.println("[CHIP] shown");
        } catch (Throwable e) {
            System.out.println("[CHIP] failed: " + e);
        }
    }
}
