package com.aigen.sims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Notify bus read side: same pending rules as swarm_notify.py (ack/snooze hide, newest first, toast once). */
class ViperNotifyTest {
    private static final String A = "{\"event\":\"msg\",\"id\":\"n1\",\"source\":\"heal\",\"level\":\"act\",\"title\":\"restored\",\"deliver\":true}";
    private static final String B = "{\"event\":\"msg\",\"id\":\"n2\",\"source\":\"disk\",\"level\":\"alarm\",\"title\":\"low space\",\"deliver\":true}";
    private static final String C = "{\"event\":\"msg\",\"id\":\"n3\",\"source\":\"hive\",\"level\":\"info\",\"title\":\"fyi\",\"deliver\":false}";

    @Test
    void pendingHidesAckedAndSnoozedNewestFirst() {
        long now = 1_000_000L;
        List<String> lines = Arrays.asList(A, B, C, "garbage {",
                "{\"event\":\"ack\",\"id\":\"n1\"}",
                "{\"event\":\"snooze\",\"id\":\"n2\",\"until\":" + (now / 1000.0 + 600) + "}");
        List<ViperNotify.Msg> p = ViperNotify.pendingFrom(lines, now);
        assertEquals(1, p.size());
        assertEquals("n3", p.get(0).id);
        assertEquals(2, ViperNotify.pendingFrom(Arrays.asList(A, B, "{\"event\":\"x\"}"), now).size());
        assertEquals("n2", ViperNotify.pendingFrom(Arrays.asList(A, B), now).get(0).id);
    }

    @Test
    void cardFieldsCarryBodyTagsAndTs() {
        String m = "{\"event\":\"msg\",\"id\":\"n9\",\"source\":\"heal\",\"level\":\"act\",\"title\":\"t\",\"body\":\"why\","
                + "\"tags\":[\"heal\",\"gui\"],\"ts\":\"2026-11-08T10:00:00\",\"deliver\":true}";
        ViperNotify.Msg g = ViperNotify.pendingFrom(Arrays.asList(m), 0).get(0);
        assertEquals("why", g.body);
        assertEquals("heal,gui", g.tags);
        assertEquals("2026-11-08T10:00:00", g.ts);
        assertEquals("#f87171", ViperUpdatesPanel.levelColor("alarm"));
    }

    @Test
    void lineAndToastedIds() {
        assertEquals("Notifications: none pending", ViperNotify.lineFrom(ViperNotify.pendingFrom(Arrays.asList(), 0)));
        assertTrue(ViperNotify.lineFrom(ViperNotify.pendingFrom(Arrays.asList(A, B), 0)).startsWith("Notifications: 2 pending - low space"));
        assertTrue(ViperNotify.toastedIds(Arrays.asList(A, "{\"event\":\"toasted\",\"id\":\"n1\"}")).contains("n1"));
    }
}
