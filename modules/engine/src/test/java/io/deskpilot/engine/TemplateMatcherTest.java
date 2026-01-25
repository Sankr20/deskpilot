package io.deskpilot.engine;


import org.junit.jupiter.api.Test;

import io.deskpilot.engine.image.MatchResult;
import io.deskpilot.engine.image.TemplateMatcher;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class TemplateMatcherTest {

    @Test
    void findBest_returnsLocationAndScore() {
        BufferedImage haystack = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = haystack.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 40, 40);
        g.setColor(Color.BLACK);
        g.fillRect(10, 12, 8, 6);
        g.dispose();

        BufferedImage needle = new BufferedImage(8, 6, BufferedImage.TYPE_INT_RGB);
        Graphics2D ng = needle.createGraphics();
        ng.setColor(Color.BLACK);
        ng.fillRect(0, 0, 8, 6);
        ng.dispose();

        MatchResult best = TemplateMatcher.findBest(haystack, needle);
        assertNotNull(best);
        assertTrue(best.score() > 0.9);
        assertEquals(new Point(10, 12), best.location());
    }

   @Test
void find_respectsThreshold() {
    // Haystack: all WHITE
    BufferedImage haystack = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = haystack.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 20, 20);
    g.dispose();

    // Needle: all BLACK
    BufferedImage needle = new BufferedImage(5, 5, BufferedImage.TYPE_INT_RGB);
    Graphics2D ng = needle.createGraphics();
    ng.setColor(Color.BLACK);
    ng.fillRect(0, 0, 5, 5);
    ng.dispose();

    // Whatever "best" does, find() must respect threshold and return null here
    assertNull(TemplateMatcher.find(haystack, needle, 0.95));
}


}
