package io.deskpilot.engine;

import io.deskpilot.engine.image.ImageUtil;
import io.deskpilot.engine.image.MatchResult;
import io.deskpilot.engine.image.TemplateMatcher;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class TemplateResourceSmokeTest {

    @Test
    void canLoadOneTemplate_andMatcherRuns() {
        // Pick a guaranteed template in your UiTemplates (SAVE_ICON exists in your pasted file)
        BufferedImage needle = ImageUtil.loadResource("icons/save_icon.png");

        // Create a simple haystack: embed the needle into the haystack so matcher has a clear best match
        BufferedImage hay = new BufferedImage(needle.getWidth() + 50, needle.getHeight() + 50, BufferedImage.TYPE_INT_ARGB);
        hay.getGraphics().drawImage(needle, 20, 20, null);

        MatchResult best = TemplateMatcher.findBest(hay, needle);
        assertNotNull(best);
        assertTrue(best.score() > 0.7, "Expected a decent score, got: " + best.score());
    }
}
