package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 2026-11-08 (roadmap phase 5, steps 44, 50, 51): the swarm notification bus, read side.
 *
 * Python's swarm_notify.py appends to C:\Viper\databases\notify\notify.jsonl: "msg" lines, then
 * "ack" / "snooze" / "toasted" lines that refer to a msg by id. Nothing is ever deleted. This reads
 * the same file, so the tray shows "Notifications (N)" and toasts each deliverable message once.
 * Ack and toasted are appended here in the same shape Python writes, so both sides agree.
 *
 * Read-mostly: the only writes are single appended lines. Never throws into the tray.
 */
public final class ViperNotify {
    static final Path LOG = Paths.get("C:\\Viper\\databases\\notify\\notify.jsonl");
    private static final ObjectMapper M = new ObjectMapper();

    private ViperNotify() {}

    /** One pending message. */
    public static final class Msg {
        public final String id, source, level, title, body, tags, ts;
        public final boolean deliver;
        Msg(String id, String source, String level, String title, boolean deliver, String body, String tags, String ts) {
            this.id = id; this.source = source; this.level = level; this.title = title; this.deliver = deliver;
            this.body = body; this.tags = tags; this.ts = ts;
        }
    }

    /** Pure: pending (not acked, not snoozed) messages, newest first. Testable without a file. */
    static List<Msg> pendingFrom(List<String> lines, long nowMs) {
        List<JsonNode> recs = new ArrayList<>();
        for (String ln : lines) {
            try { recs.add(M.readTree(ln)); } catch (IOException | RuntimeException e) { /* skip junk line */ }
        }
        Set<String> acked = new HashSet<>();
        Map<String, Double> snoozed = new HashMap<>();
        for (JsonNode r : recs) {
            String ev = r.path("event").asText();
            if ("ack".equals(ev)) acked.add(r.path("id").asText());
            if ("snooze".equals(ev)) snoozed.merge(r.path("id").asText(), r.path("until").asDouble(0), Math::max);
        }
        List<Msg> out = new ArrayList<>();
        for (int i = recs.size() - 1; i >= 0; i--) {
            JsonNode r = recs.get(i);
            if (!"msg".equals(r.path("event").asText())) continue;
            String id = r.path("id").asText();
            if (acked.contains(id) || snoozed.getOrDefault(id, 0.0) * 1000.0 > nowMs) continue;
            StringBuilder tg = new StringBuilder();
            for (JsonNode t : r.path("tags")) tg.append(tg.length() == 0 ? "" : ",").append(t.asText());
            out.add(new Msg(id, r.path("source").asText(), r.path("level").asText(), r.path("title").asText(),
                    r.path("deliver").asBoolean(false), r.path("body").asText(), tg.toString(), r.path("ts").asText()));
        }
        return out;
    }

    static Set<String> toastedIds(List<String> lines) {
        Set<String> s = new HashSet<>();
        for (String ln : lines) {
            try {
                JsonNode r = M.readTree(ln);
                if ("toasted".equals(r.path("event").asText())) s.add(r.path("id").asText());
            } catch (IOException | RuntimeException e) { /* skip */ }
        }
        return s;
    }

    /** Menu text: "Notifications: 3 pending - <newest title>". */
    static String lineFrom(List<Msg> pending) {
        if (pending.isEmpty()) return "Notifications: none pending";
        String t = pending.get(0).title;
        if (t.length() > 48) t = t.substring(0, 47) + "...";
        return "Notifications: " + pending.size() + " pending - " + t;
    }

    static List<String> readLines() {
        try { return Files.readAllLines(LOG, StandardCharsets.UTF_8); } catch (IOException e) { return new ArrayList<>(); }
    }

    public static String line() { return lineFrom(pendingFrom(readLines(), System.currentTimeMillis())); }

    private static void append(String json) {
        try {
            Files.createDirectories(LOG.getParent());
            Files.write(LOG, (json + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { /* a notifier never breaks the tray */ }
    }

    /** Tray menu action: acknowledge everything pending (appends ack lines). */
    public static void ackAll() {
        for (Msg m : pendingFrom(readLines(), System.currentTimeMillis()))
            append("{\"event\":\"ack\",\"id\":\"" + m.id + "\",\"epoch\":" + (System.currentTimeMillis() / 1000.0) + "}");
    }

    /** Pending messages for the Updates panel. */
    public static List<Msg> pending() { return pendingFrom(readLines(), System.currentTimeMillis()); }

    /** Card button: acknowledge one message. */
    public static void ack(String id) {
        append("{\"event\":\"ack\",\"id\":\"" + id + "\",\"epoch\":" + (System.currentTimeMillis() / 1000.0) + "}");
    }

    /** Card button: hide one message for the given minutes (same shape as swarm_notify.snooze). */
    public static void snooze(String id, int minutes) {
        append("{\"event\":\"snooze\",\"id\":\"" + id + "\",\"until\":" + (System.currentTimeMillis() / 1000.0 + 60.0 * minutes) + "}");
    }

    /** Card button: file the message as a board contract (python does the real work), then it is acked. */
    public static void makeContract(String id) {
        new Thread(() -> {
            try {
                new ProcessBuilder("C:\\Users\\viper\\AppData\\Local\\Programs\\Python\\Python311\\python.exe",
                        "C:\\Viper\\scripts\\swarm_notify.py", "contract", id)
                        .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start().waitFor();
            } catch (Exception e) { /* card stays pending, user can retry */ }
        }, "notify-contract").start();
    }

    /** Toast each pending, deliverable, not-yet-toasted message once. Called by the tray's status thread. */
    public static void toastNew() {
        try {
            List<String> lines = readLines();
            Set<String> done = toastedIds(lines);
            List<Msg> p = pendingFrom(lines, System.currentTimeMillis());
            for (int i = p.size() - 1; i >= 0; i--) {                 // oldest first
                Msg m = p.get(i);
                if (!m.deliver || done.contains(m.id)) continue;
                ViperTray.toast("[" + m.source + "] " + m.level, m.title);
                append("{\"event\":\"toasted\",\"id\":\"" + m.id + "\",\"by\":\"godhand\"}");
            }
        } catch (RuntimeException e) { /* never */ }
    }
}
