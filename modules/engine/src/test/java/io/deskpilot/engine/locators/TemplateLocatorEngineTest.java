package io.deskpilot.engine.locators;

import io.deskpilot.engine.image.ImageUtil;
import io.deskpilot.engine.targets.TemplateTarget;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class TemplateLocatorEngineTest {

    @Test
    void templateFound_returnsFoundWithPointAndScore() throws Exception {
        BufferedImage client = ImageUtil.loadResource("test-templates/client40.png");

        TemplateTarget t = new TemplateTarget(
                "black8",
                new String[]{ "test-templates/black8.png" },
                0.80,
                null,
                null
        );

        var r = TemplateLocatorEngine.locate(
                client,
                new Rectangle(100, 200, client.getWidth(), client.getHeight()),
                t,
                "black8"
        );

        assertEquals(LocateStatus.FOUND, r.status);
        assertNotNull(r.point);
        assertTrue(r.score >= 0.80, "score=" + r.score);
        assertNotNull(r.bounds);
    }

    @Test
    void templateNearMiss_returnsNearMiss() throws Exception {
        BufferedImage client = ImageUtil.loadResource("test-templates/client40.png");

        // set minScore too high so it can't reach it, but still should find a best score and return NEAR_MISS
        TemplateTarget t = new TemplateTarget(
                "black8",
                // Make sure anchors still match; the mismatch is on a non-anchor pixel.
                new String[]{ "test-templates/black8_nonanchorwhite.png" },
                0.999,
                null,
                null
        );

        var r = TemplateLocatorEngine.locate(
                client,
                new Rectangle(0, 0, client.getWidth(), client.getHeight()),
                t,
                "black8"
        );

        assertEquals(LocateStatus.NEAR_MISS, r.status);
        assertTrue(r.score >= 0.0, "near score should be recorded");
        assertNotNull(r.bounds);
    }
}