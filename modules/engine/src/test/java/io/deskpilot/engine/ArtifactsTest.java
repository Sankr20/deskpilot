package io.deskpilot.engine;


import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactsTest {

    @Test
    void stepDir_isStableAndCreatesDirectory() throws Exception {
        Path tmp = Files.createTempDirectory("deskpilot-artifacts");
        Artifacts a = new Artifacts(tmp);

        Path d1 = a.stepDir("click Search Button");
        Path d2 = a.stepDir("waitForTextContains");

        assertTrue(Files.isDirectory(d1));
        assertTrue(Files.isDirectory(d2));
        assertNotEquals(d1, d2);
        assertTrue(d1.getFileName().toString().startsWith("01-"));
        assertTrue(d2.getFileName().toString().startsWith("02-"));
    }

    @Test
    void savePng_writesFile() throws Exception {
        Path tmp = Files.createTempDirectory("deskpilot-artifacts");
        Artifacts a = new Artifacts(tmp);
        Path step = a.stepDir("demo");

        BufferedImage img = new BufferedImage(20, 10, BufferedImage.TYPE_INT_ARGB);
        Path f = a.savePng(step, "client.png", img);
        assertTrue(Files.exists(f));
        assertTrue(f.getFileName().toString().endsWith(".png"));
    }
}
