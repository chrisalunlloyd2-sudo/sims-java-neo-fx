package com.aigen.sims;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Hex chip verdict (roadmap steps 59-60): pure rules, no FX. */
class ViperChipTest {
    @Test
    void greenAmberRed() {
        assertEquals("green", ViperChip.verdict(1.0, 6000, 20, false, 3)[0]);
        assertEquals("amber", ViperChip.verdict(1.56, 6000, 20, false, 3)[0]);
        assertEquals("amber", ViperChip.verdict(1.0, 2500, 20, false, 3)[0]);
        assertEquals("amber", ViperChip.verdict(1.0, 6000, 20, false, 2)[0]);
        assertEquals("red", ViperChip.verdict(1.0, 1500, 20, false, 3)[0]);
        assertEquals("red", ViperChip.verdict(1.0, 6000, 900, false, 3)[0]);
        assertEquals("red", ViperChip.verdict(1.0, 6000, Double.NaN, false, 3)[0]);
        assertEquals("red", ViperChip.verdict(1.0, 6000, 20, true, 3)[0]);
        assertEquals("red", ViperChip.verdict(2.6, 6000, 20, false, 3)[0]);
        assertEquals("green", ViperChip.verdict(1.0, -1, 20, false, 3)[0]);       // unknown RAM never alarms
        String[] both = ViperChip.verdict(1.6, 1500, 20, false, 3);
        assertTrue(both[1].contains("RAM") && both[1].contains("throttle"));
    }

    @Test
    void topRamSumsByImage() {
        List<String> csv = Arrays.asList(
                "\"java.exe\",\"100\",\"Console\",\"1\",\"1,048,576 K\"",
                "\"java.exe\",\"101\",\"Console\",\"1\",\"1,048,576 K\"",
                "\"ollama.exe\",\"7\",\"Services\",\"0\",\"524,288 K\"",
                "\"tiny.exe\",\"8\",\"Services\",\"0\",\"1,024 K\"",
                "junk");
        List<String> top = ViperChip.topRam(csv, 2);
        assertEquals(Arrays.asList("java.exe 2.0 GB", "ollama.exe 0.5 GB"), top);
    }
}
