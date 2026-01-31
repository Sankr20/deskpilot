package io.deskpilot.engine;

import java.awt.image.BufferedImage;

public final class ImageDiff {
    private ImageDiff() {}

    /** Returns [0..1] ratio of pixels differing beyond tolerance. */
    public static double diffRatio(BufferedImage a, BufferedImage b) {
        if (a == null || b == null) return 1.0;
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        if (w == 0 || h == 0) return 1.0;

        final int tol = 10;
        long diff = 0;
        long total = (long) w * (long) h;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pa = a.getRGB(x, y);
                int pb = b.getRGB(x, y);

                int ra = (pa >> 16) & 0xFF, ga = (pa >> 8) & 0xFF, ba = pa & 0xFF;
                int rb = (pb >> 16) & 0xFF, gb = (pb >> 8) & 0xFF, bb = pb & 0xFF;

                if (Math.abs(ra - rb) > tol ||
                    Math.abs(ga - gb) > tol ||
                    Math.abs(ba - bb) > tol) {
                    diff++;
                }
            }
        }
        return (double) diff / (double) total;
    }
}
