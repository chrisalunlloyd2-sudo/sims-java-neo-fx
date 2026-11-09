package com.aigen.sims;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * 2026-11-08 (roadmap phase 5, steps 48/49): the Updates panel. One card per pending swarm message
 * from notify.jsonl, newest first, tagged by source. Buttons: Open log, Ack, Snooze 1 h, Make contract.
 * Every button appends one line to the same log the Python bus reads; nothing is deleted.
 * Step 61: sizes come from ViperUi (large text + short lines), re-read on every refresh.
 */
public final class ViperUpdatesPanel {
    private ViperUpdatesPanel() {}

    static String levelColor(String level) {
        return "alarm".equals(level) ? "#f87171" : "act".equals(level) ? "#fbbf24" : "#38bdf8";
    }

    static String font(String color, double base, boolean bold) {
        return "-fx-text-fill: " + color + "; -fx-font-family: monospace; -fx-font-size: " + ViperUi.px(base) + "px;"
                + (bold ? " -fx-font-weight: bold;" : "");
    }

    private static Label label(String text, String color, double base, boolean bold) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(ViperUi.wrap(base));
        l.setStyle(font(color, base, bold));
        return l;
    }

    static VBox card(ViperNotify.Msg m, Runnable refresh) {
        VBox box = new VBox(4,
                label("[" + m.source + "] " + m.level.toUpperCase() + "   " + m.ts, levelColor(m.level), 11, true),
                label(m.title, "#e2e8f0", 12, false));
        if (!m.body.isEmpty())
            box.getChildren().add(label(m.body.length() > 400 ? m.body.substring(0, 399) + "..." : m.body, "#cbd5e1", 11, false));
        if (!m.tags.isEmpty()) box.getChildren().add(label("tags: " + m.tags, "#a78bfa", 10, false));
        FlowPane buttons = new FlowPane(6, 6,                     // wraps under each other in large mode
                btn("Open log", () -> ViperTray.open(ViperNotify.LOG.toString()), refresh, false),
                btn("Ack", () -> ViperNotify.ack(m.id), refresh, true),
                btn("Snooze 1 h", () -> ViperNotify.snooze(m.id, 60), refresh, true),
                btn("Make contract", () -> ViperNotify.makeContract(m.id), refresh, true));
        box.getChildren().add(buttons);
        box.setStyle("-fx-padding: 8; -fx-background-color: #140d2b; -fx-border-color: " + levelColor(m.level) + "; -fx-border-width: 0 0 0 3;");
        return box;
    }

    private static Button btn(String text, Runnable action, Runnable refresh, boolean refreshAfter) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #2a1a4f; -fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: " + ViperUi.px(10) + "px;");
        b.setOnAction(e -> {
            action.run();
            if (refreshAfter) refresh.run();
        });
        return b;
    }

    public static VBox create() {
        Label count = new Label("...");
        Button ackAll = new Button("Ack all");
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
            count.setStyle(font("#38bdf8", 12, false));
            ackAll.setStyle("-fx-background-color: #2a1a4f; -fx-text-fill: #e2e8f0; -fx-font-family: monospace; -fx-font-size: " + ViperUi.px(10) + "px;");
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
