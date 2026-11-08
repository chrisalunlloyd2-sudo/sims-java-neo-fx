package com.aigen.sims;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * 2026-11-08 (roadmap phase 5, steps 48/49): the Updates panel. One card per pending swarm message
 * from notify.jsonl, newest first, tagged by source. Buttons: Open log, Ack, Snooze 1 h, Make contract.
 * Every button appends one line to the same log the Python bus reads; nothing is deleted.
 */
public final class ViperUpdatesPanel {
    private static final String TEXT = "-fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: 11px;";

    private ViperUpdatesPanel() {}

    static String levelColor(String level) {
        return "alarm".equals(level) ? "#f87171" : "act".equals(level) ? "#fbbf24" : "#38bdf8";
    }

    static VBox card(ViperNotify.Msg m, Runnable refresh) {
        Label head = new Label("[" + m.source + "] " + m.level.toUpperCase() + "   " + m.ts);
        head.setStyle("-fx-text-fill: " + levelColor(m.level) + "; -fx-font-family: monospace; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label title = new Label(m.title);
        title.setWrapText(true);
        title.setStyle(TEXT + " -fx-font-size: 12px;");
        VBox box = new VBox(4, head, title);
        if (!m.body.isEmpty()) {
            Label body = new Label(m.body.length() > 400 ? m.body.substring(0, 399) + "..." : m.body);
            body.setWrapText(true);
            body.setStyle(TEXT + " -fx-opacity: 0.8;");
            box.getChildren().add(body);
        }
        if (!m.tags.isEmpty()) {
            Label tags = new Label("tags: " + m.tags);
            tags.setStyle("-fx-text-fill: #a78bfa; -fx-font-family: monospace; -fx-font-size: 10px;");
            box.getChildren().add(tags);
        }
        Button open = btn("Open log", () -> ViperTray.open(ViperNotify.LOG.toString()), refresh, false);
        Button ack = btn("Ack", () -> ViperNotify.ack(m.id), refresh, true);
        Button snz = btn("Snooze 1 h", () -> ViperNotify.snooze(m.id, 60), refresh, true);
        Button con = btn("Make contract", () -> ViperNotify.makeContract(m.id), refresh, true);
        box.getChildren().add(new HBox(6, open, ack, snz, con));
        box.setStyle("-fx-padding: 8; -fx-background-color: #140d2b; -fx-border-color: " + levelColor(m.level) + "; -fx-border-width: 0 0 0 3;");
        return box;
    }

    private static Button btn(String text, Runnable action, Runnable refresh, boolean refreshAfter) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #2a1a4f; -fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: 10px;");
        b.setOnAction(e -> {
            action.run();
            if (refreshAfter) refresh.run();
        });
        return b;
    }

    public static VBox create() {
        Label count = new Label("...");
        count.setStyle("-fx-text-fill: #38bdf8; -fx-font-family: monospace; -fx-font-size: 12px;");
        Button ackAll = new Button("Ack all");
        ackAll.setStyle("-fx-background-color: #2a1a4f; -fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: 10px;");
        VBox cards = new VBox(6);
        ScrollPane sp = new ScrollPane(cards);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #0b0716; -fx-background-color: #0b0716;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        VBox root = new VBox(6, new HBox(10, count, ackAll), sp);
        root.setStyle("-fx-padding: 6;");

        Runnable[] self = new Runnable[1];
        self[0] = () -> {
            List<ViperNotify.Msg> p = ViperNotify.pending();
            count.setText(p.isEmpty() ? "No pending updates" : p.size() + " pending");
            cards.getChildren().clear();
            for (ViperNotify.Msg m : p) cards.getChildren().add(card(m, self[0]));
        };
        ackAll.setOnAction(e -> { ViperNotify.ackAll(); self[0].run(); });
        self[0].run();
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(3), e -> self[0].run()));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
        return root;
    }
}
