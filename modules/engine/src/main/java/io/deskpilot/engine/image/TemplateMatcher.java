package io.deskpilot.engine.image;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TemplateMatcher {

    public static MatchResult find(BufferedImage haystack, BufferedImage needle, double minScore) {
        MatchResult best = findBest(haystack, needle);
        if (best == null) return null;
        return best.score() >= minScore ? best : null;
    }

    /**
     * Finds the best match location and score, even if the score is below the caller's threshold.
     *
     * Returns null only when matching is impossible (e.g., needle larger than haystack).
     */
    public static MatchResult findBest(BufferedImage haystack, BufferedImage needle) {
        int hw = haystack.getWidth();
        int hh = haystack.getHeight();
        int nw = needle.getWidth();
        int nh = needle.getHeight();

        if (nw <= 0 || nh <= 0) throw new IllegalArgumentException("needle is empty");
        if (nw > hw || nh > hh) return null;

        // scan stride (skip positions)
        int stride = chooseStride(hw, hh);

        double bestScore = 0;
        Point bestPoint = null;

        // a few anchor pixels for fast reject
        Anchor[] anchors = anchorsFor(needle);

        for (int y = 0; y <= hh - nh; y += stride) {
            for (int x = 0; x <= hw - nw; x += stride) {

                if (!anchorsMatch(haystack, needle, x, y, anchors)) continue;

                double score = compareSampled(haystack, needle, x, y);
                if (score > bestScore) {
                    bestScore = score;
                    bestPoint = new Point(x, y);

                    if (bestScore >= 0.999) break; // perfect match, bail early
                }
            }
            if (bestScore >= 0.999) break;
        }

        return bestPoint == null ? null : new MatchResult(bestPoint, bestScore);
    }

    private static int chooseStride(int w, int h) {
        long area = (long) w * (long) h;
        if (area <= 300_000) return 1;
        if (area <= 1_000_000) return 2;
        return 3;
    }

    /** Sampled compare inside the template (faster than full compare). */
    private static double compareSampled(BufferedImage haystack, BufferedImage needle, int ox, int oy) {
        int nw = needle.getWidth();
        int nh = needle.getHeight();

        // small icons: full compare; larger: sample every 2 pixels
        int innerStride = (nw * nh <= 900) ? 1 : 2;

        long match = 0;
        long total = 0;

        for (int y = 0; y < nh; y += innerStride) {
            for (int x = 0; x < nw; x += innerStride) {
                int p1 = haystack.getRGB(ox + x, oy + y);
                int p2 = needle.getRGB(x, y);
                if (closeRgb(p1, p2)) match++;
                total++;
            }
        }

        return total == 0 ? 0 : (double) match / total;
    }

    private static boolean closeRgb(int rgb1, int rgb2) {
        int r1 = (rgb1 >> 16) & 0xFF, g1 = (rgb1 >> 8) & 0xFF, b1 = rgb1 & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF, g2 = (rgb2 >> 8) & 0xFF, b2 = rgb2 & 0xFF;

        int dr = Math.abs(r1 - r2);
        int dg = Math.abs(g1 - g2);
        int db = Math.abs(b1 - b2);

        int tol = 18; // adjust if needed
        return dr <= tol && dg <= tol && db <= tol;
    }

    // -------- anchors (fast reject) --------

    private record Anchor(int x, int y) {}

    private static Anchor[] anchorsFor(BufferedImage needle) {
        int w = needle.getWidth();
        int h = needle.getHeight();
        return new Anchor[]{
                new Anchor(0, 0),
                new Anchor(w - 1, 0),
                new Anchor(0, h - 1),
                new Anchor(w - 1, h - 1),
                new Anchor(w / 2, h / 2)
        };
    }

    private static boolean anchorsMatch(BufferedImage haystack, BufferedImage needle, int ox, int oy, Anchor[] anchors) {
        for (Anchor a : anchors) {
            int p1 = haystack.getRGB(ox + a.x(), oy + a.y());
            int p2 = needle.getRGB(a.x(), a.y());
            if (!closeRgb(p1, p2)) return false;
        }
        return true;
    }
}
