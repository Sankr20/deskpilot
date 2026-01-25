package io.deskpilot.engine.targets;

import java.awt.Rectangle;

public class SearchAreaPct {

    public final double xPct;
    public final double yPct;
    public final double wPct;
    public final double hPct;

    public SearchAreaPct(double xPct, double yPct, double wPct, double hPct) {
        if (!isFinite(xPct) || !isFinite(yPct) || !isFinite(wPct) || !isFinite(hPct)) {
            throw new IllegalArgumentException("pct values must be finite");
        }
        if (wPct <= 0 || hPct <= 0) throw new IllegalArgumentException("wPct/hPct must be > 0");

        // We allow slight out-of-range and clamp later, but huge values are almost always a bug.
        if (wPct > 2.0 || hPct > 2.0) throw new IllegalArgumentException("wPct/hPct too large: " + wPct + "/" + hPct);

        this.xPct = xPct;
        this.yPct = yPct;
        this.wPct = wPct;
        this.hPct = hPct;
    }

    /** Convert to client-local pixel Rectangle with clamping. Throws if result is empty. */
    public Rectangle toClientPixels(int clientW, int clientH) {
        if (clientW <= 0 || clientH <= 0) throw new IllegalArgumentException("clientW/clientH must be > 0");

        // Clamp pct to [0..1] for origin
        double x0 = clamp01(xPct);
        double y0 = clamp01(yPct);

        // Clamp width/height so x+w/y+h doesn't exceed 1
        double w0 = clamp01(wPct);
        double h0 = clamp01(hPct);

        // If origin is already near edge, cap the width/height accordingly
        if (x0 + w0 > 1.0) w0 = 1.0 - x0;
        if (y0 + h0 > 1.0) h0 = 1.0 - y0;

        int x = (int) Math.round(x0 * clientW);
        int y = (int) Math.round(y0 * clientH);
        int w = (int) Math.round(w0 * clientW);
        int h = (int) Math.round(h0 * clientH);

        // Make sure we don't go out of bounds due to rounding
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > clientW - 1) x = clientW - 1;
        if (y > clientH - 1) y = clientH - 1;

        if (x + w > clientW) w = clientW - x;
        if (y + h > clientH) h = clientH - y;

        // Fail fast instead of silently returning 0x0 (causes confusing template-not-found)
        if (w <= 0 || h <= 0) {
            throw new IllegalArgumentException(
                    "SearchAreaPct resolves to empty pixels. " +
                    "pct=(" + xPct + "," + yPct + "," + wPct + "," + hPct + ") " +
                    "client=(" + clientW + "x" + clientH + ") " +
                    "resolved=(" + x + "," + y + "," + w + "," + h + ")"
            );
        }

        return new Rectangle(x, y, w, h);
    }

    // -------------------------
    // ergonomic presets
    // -------------------------

    public static SearchAreaPct topLeft(double wPct, double hPct) {
        return new SearchAreaPct(0, 0, wPct, hPct);
    }

    public static SearchAreaPct topBar(double hPct) {
        return new SearchAreaPct(0, 0, 1.0, hPct);
    }

    public static SearchAreaPct leftNav(double wPct) {
        return new SearchAreaPct(0, 0, wPct, 1.0);
    }

    public static SearchAreaPct statusBar(double yPct, double hPct) {
        return new SearchAreaPct(0, yPct, 1.0, hPct);
    }

    /** General “box” builder (nice for future recorder): values are pct of client. */
    public static SearchAreaPct box(double xPct, double yPct, double wPct, double hPct) {
        return new SearchAreaPct(xPct, yPct, wPct, hPct);
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    private static boolean isFinite(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v);
    }
}
