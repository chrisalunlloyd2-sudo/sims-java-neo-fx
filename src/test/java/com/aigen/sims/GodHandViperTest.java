package com.aigen.sims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2026-11-05: guards for the GodHand <-> Viper wiring. No network, no models, no GUI:
 * everything here runs headless in a second or two. Live checks are in C:\Viper\scripts\godhand_check.py.
 */
class GodHandViperTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    // ---- box mic reader (OllamaRouter yields to the Viper mic) ----

    @Test
    void boxMicReportsModelWhileHeld(@TempDir Path dir) throws Exception {
        Path mic = dir.resolve("mic.json");
        Files.writeString(mic, "{\"model\":\"qwen2.5-coder:1.5b\",\"holders\":[{\"pid\":1,\"tier\":1,\"reason\":\"house\"}]}");
        assertEquals("qwen2.5-coder:1.5b", OllamaRouter.boxMicModel(mic));
    }

    @Test
    void boxMicIsFreeWithNoHolders(@TempDir Path dir) throws Exception {
        Path mic = dir.resolve("mic.json");
        Files.writeString(mic, "{\"model\":\"granite3.1-moe:1b\",\"holders\":[],\"last_model\":\"granite3.1-moe:1b\"}");
        assertNull(OllamaRouter.boxMicModel(mic));
    }

    @Test
    void boxMicTornWriteOrMissingFileReadsAsFree(@TempDir Path dir) throws Exception {
        Path torn = dir.resolve("torn.json");
        Files.writeString(torn, "{\"model\":\"qwen");
        assertNull(OllamaRouter.boxMicModel(torn));
        assertNull(OllamaRouter.boxMicModel(dir.resolve("absent.json")));
    }

    // ---- physics kernel driven by real CPU ----

    private static double settle(StrainRatePhysicsKernel k, double cpu) {
        double dt = 0.02;
        for (int i = 0; i < 8; i++) k.updateStrainRate(cpu * 4.0 * dt, dt);
        return k.getStress();
    }

    @Test
    void stressStaysUnderThresholdAtHalfCpu() {
        assertTrue(settle(new StrainRatePhysicsKernel(), 0.5) <= 1.5);
    }

    @Test
    void stressCrossesThresholdAtHighCpu() {
        StrainRatePhysicsKernel k = new StrainRatePhysicsKernel();
        PrintStream old = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            for (int i = 0; i < 200; i++) k.updateStrainRate(0.9 * 4.0 * 0.02, 0.02);
        } finally {
            System.setOut(old);
        }
        String log = out.toString();
        int count = log.split("High Stress Detected", -1).length - 1;
        // The bug this guards: one line per frame (32,910 lines in half an hour). Entering high stress logs at most a few times.
        assertTrue(count >= 1, "high CPU should register as high stress");
        assertTrue(count <= 10, "logged " + count + " times in 200 frames; should log on transitions only");
    }

    // ---- vitals ----

    @Test
    void vitalsAreInRange() {
        double cpu = ViperVitals.cpu();
        assertTrue(cpu >= 0.0 && cpu <= 1.0, "cpu " + cpu);
        assertTrue(ViperVitals.totalRamGb() > 0.5);
        assertTrue(ViperVitals.freeRamGb() <= ViperVitals.totalRamGb());
    }

    // ---- Viper Hive panel formatting (hexflow JSON shapes as of 2026-11-05) ----

    @Test
    void quorumRowShowsVerdictWeightAndVeto() throws Exception {
        JsonNode d = JSON.readTree("{\"t\":\"2026-11-05 10:00:00\",\"passed\":false,\"approve_weight\":9.0,"
            + "\"module\":\"foo_bot\",\"vetoed\":[\"parses\"],\"ballots\":[]}");
        String row = ViperHivePanel.decisionRow(d, 18.0);
        assertTrue(row.contains("FAIL"));
        assertTrue(row.contains("9.0/18.0"));
        assertTrue(row.contains("foo_bot"));
        assertTrue(row.contains("VETO parses"));
    }

    @Test
    void ballotsListEveryVoterWithReason() throws Exception {
        JsonNode d = JSON.readTree("{\"module\":\"m\",\"ballots\":["
            + "{\"approve\":true,\"voter\":\"parses\",\"weight\":3.0,\"why\":\"python ast\"},"
            + "{\"approve\":false,\"voter\":\"not_duplicate\",\"weight\":2.0,\"why\":\"identical body already sealed\"}]}");
        String text = ViperHivePanel.ballotText(d);
        assertTrue(text.contains("yes parses"));
        assertTrue(text.contains(" NO not_duplicate"));
        assertTrue(text.contains("identical body already sealed"));
    }

    @Test
    void cellsListFailingButNotRetired() throws Exception {
        JsonNode h = JSON.readTree("{\"total\":3,\"healthy\":1,\"failing\":1,\"retired\":1,\"throttle\":{},\"cells\":["
            + "{\"cell\":\"good\",\"last_ok\":true,\"retired\":false,\"tier\":1,\"last_secs\":1.0},"
            + "{\"cell\":\"broken\",\"last_ok\":false,\"retired\":false,\"tier\":2,\"consecutive_fails\":3,\"last_error\":\"boom\",\"last_secs\":2.0},"
            + "{\"cell\":\"old\",\"last_ok\":false,\"retired\":true,\"tier\":3,\"last_secs\":9.0}]}");
        String text = ViperHivePanel.cellsText(h);
        String failing = text.substring(text.indexOf("FAILING NOW"), text.indexOf("SLOWEST"));
        assertTrue(failing.contains("broken"));
        assertTrue(failing.contains("boom"));
        assertFalse(failing.contains("old"), "retired cells are not failures");
        assertFalse(text.contains("old "), "retired cells are left out of the slow list too");
    }

    @Test
    void jobsShowPromotedAndErrors() throws Exception {
        JsonNode m = JSON.readTree("{\"result_count\":2,\"success_count\":1,\"push_total\":0,\"daemon_log\":["
            + "{\"ok\":true,\"action\":\"mined\",\"best_tier\":3,\"promoted\":true,\"secs\":12.0,\"task\":\"Write a.py\"},"
            + "{\"ok\":false,\"action\":\"mined\",\"best_tier\":0,\"promoted\":false,\"secs\":5.0,\"task\":\"Write b.py\"}]}");
        String text = ViperHivePanel.jobsText(m);
        assertTrue(text.contains("PROMOTED"));
        assertTrue(text.contains("ERR"));
        assertTrue(text.contains("results 2"));
    }

    // ---- tray doorbell (2026-11-06): a relaunch shows the running copy ----

    @Test
    void doorbellSendsShowToTheRunningCopy() throws Exception {
        try (java.net.ServerSocket fake = new java.net.ServerSocket(
                0, 1, java.net.InetAddress.getLoopbackAddress())) {   // free port: a live GodHand owns 7787
            fake.setSoTimeout(5000);
            java.util.concurrent.CompletableFuture<String> got =
                    java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try (java.net.Socket c = fake.accept()) {
                            return new java.io.BufferedReader(new java.io.InputStreamReader(
                                    c.getInputStream())).readLine();
                        } catch (Exception e) { return "ERR " + e; }
                    });
            assertTrue(ViperTray.ringExisting(fake.getLocalPort()));
            assertEquals("SHOW", got.get(6, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @Test
    void doorbellUnansweredMeansStartFresh() throws Exception {
        int port;
        try (java.net.ServerSocket probe = new java.net.ServerSocket(0)) { port = probe.getLocalPort(); }
        assertFalse(ViperTray.ringExisting(port));   // a port nobody holds: no copy answers
    }

    // ---- live feed (2026-11-07): dashboard grid / stability / KQML from real sources ----

    @Test
    void quorumGridTakesPassedFlagsInOrderCappedAt64() throws Exception {
        StringBuilder sb = new StringBuilder("{\"decisions\":[");
        for (int i = 0; i < 70; i++) sb.append(i == 0 ? "" : ",").append("{\"passed\":").append(i % 3 == 0).append("}");
        boolean[] v = ViperLiveFeed.votes(JSON.readTree(sb.append("]}").toString()), 64);
        assertEquals(64, v.length);
        assertTrue(v[0]);
        assertFalse(v[1]);
        assertEquals(0, ViperLiveFeed.votes(JSON.readTree("{}"), 64).length, "no decisions = no lit cells");
    }

    @Test
    void stabilityIsHealthyShareOfLiveCells() throws Exception {
        JsonNode h = JSON.readTree("{\"cells\":["
            + "{\"consecutive_fails\":0,\"retired\":false},{\"consecutive_fails\":2,\"retired\":false},"
            + "{\"consecutive_fails\":0,\"retired\":false},{\"consecutive_fails\":0,\"retired\":false},"
            + "{\"consecutive_fails\":9,\"retired\":true}]}");
        assertEquals(0.75, ViperLiveFeed.stability(h), 1e-9);
        assertTrue(Double.isNaN(ViperLiveFeed.stability(JSON.readTree("{\"cells\":[]}"))));
    }

    @Test
    void kqmlViewShowsRealMessagesOrSaysEmpty(@TempDir Path dir) throws Exception {
        for (String q : ViperLiveFeed.QUEUES) Files.createDirectories(dir.resolve(q));
        String empty = ViperLiveFeed.kqmlText(dir, 4);
        assertTrue(empty.contains("[queue empty]"));
        assertFalse(empty.contains("StabilityDaemon"), "no invented traffic");
        Files.writeString(dir.resolve("to_kai").resolve("moe_1.json"), "{\"acl\":\"(ask\\n  :sender moe\\n  :receiver kai)\"}");
        String one = ViperLiveFeed.kqmlText(dir, 4);
        assertTrue(one.contains("to_kai  1 message(s)"));
        assertTrue(one.contains(":sender moe"));
        assertFalse(one.contains("[queue empty]"));
    }

    @Test
    void swarmTabReportsRealStateAndSaysWhatIsUnreachable() throws Exception {
        JsonNode ps = JSON.readTree("{\"models\":[{\"name\":\"qwen2.5:1.5b\",\"size\":1048576000,\"size_vram\":0}]}");
        JsonNode hive = JSON.readTree("{\"cells\":[{\"cell\":\"a\",\"consecutive_fails\":0},{\"cell\":\"miner\",\"consecutive_fails\":3},"
            + "{\"cell\":\"old\",\"consecutive_fails\":5,\"retired\":true}]}");
        JsonNode voting = JSON.readTree("{\"decisions\":[{\"t\":\"2026-11-07 08:00\",\"passed\":true,\"module\":\"foo_bot\"}]}");
        String s = ViperLiveFeed.swarmText(ps, hive, voting, "qwen2.5:1.5b");
        assertTrue(s.contains("held by qwen2.5:1.5b"));
        assertTrue(s.contains("qwen2.5:1.5b") && s.contains("1000 MB"));
        assertTrue(s.contains("2 live, 1 healthy, 1 failing"));
        assertTrue(s.contains("failing: miner") && !s.contains("old"));
        assertTrue(s.contains("PASS  foo_bot"));
        String down = ViperLiveFeed.swarmText(null, null, null, null);
        assertTrue(down.contains("mic: free"));
        assertTrue(down.contains("[ollama :11434 not reached]") && down.contains("[hexflow :7766 not reached]"));
        assertFalse(down.contains("Agent Alpha"), "no invented registry");
    }

    @Test
    void trayMenusKeepGroupOrderAndStatusLinesReadTheSnapshot() {
        ViperTray.clearMenus();
        ViperTray.window("Open", "Viper Hive", () -> {});
        ViperTray.action("Logs", "GodHand log", () -> {});
        ViperTray.status("Status", () -> "x");
        ViperTray.window("Open", "Notes", () -> {});
        assertEquals("Open: Viper Hive; Notes;\nLogs: GodHand log;\nStatus: [status];\n", ViperTray.outline());
        ViperTray.clearMenus();
        assertEquals("Quorum: 2 passed / 3 latest", ViperLiveFeed.quorumLine(new boolean[]{true, false, true}));
        assertEquals("Quorum: hexflow not reached", ViperLiveFeed.quorumLine(null));
        assertEquals("Hive: 75% of live cells healthy", ViperLiveFeed.hiveLine(0.75));
        assertEquals("Hive: not reached", ViperLiveFeed.hiveLine(Double.NaN));
    }

    @Test
    void archivalLineReadsTheNewestLog(@TempDir Path dir) throws Exception {
        assertEquals("Archive: never run", ViperLiveFeed.archivalLine(dir));
        Files.writeString(dir.resolve("daily-archival-20261106.log"), "[2026-11-06T01:06:35] === DONE ===\n");
        Files.writeString(dir.resolve("daily-archival-20261107.log"),
            "[2026-11-07T01:00:39] step2: fleet -> OK\n[2026-11-07T01:06:35] === DONE ===\n");
        assertEquals("Archive: 2026-11-07 01:06 done", ViperLiveFeed.archivalLine(dir));
        Files.writeString(dir.resolve("daily-archival-20261108.log"), "[2026-11-08T09:40:02] step2: viper-scripts -> OK\n");
        assertTrue(ViperLiveFeed.archivalLine(dir).startsWith("Archive: 2026-11-08 09:40 running: step2"));
    }

    @Test
    void trayIconIsDrawnWithoutResources() {
        java.awt.image.BufferedImage img = ViperTray.image();
        assertEquals(32, img.getWidth());
        assertNotEquals(0, img.getRGB(16, 16) >>> 24, "centre must be opaque");
        assertEquals(0, img.getRGB(0, 0) >>> 24, "corner must be clear");
    }
}
