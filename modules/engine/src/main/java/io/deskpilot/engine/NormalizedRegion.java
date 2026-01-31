package io.deskpilot.engine;

import java.awt.Point;
import java.awt.Rectangle;

public class NormalizedRegion {

    public final double xPct;
    public final double yPct;
    public final double wPct;
    public final double hPct;

    public NormalizedRegion(double xPct, double yPct, double wPct, double hPct) {
        this.xPct = xPct;
        this.yPct = yPct;
        this.wPct = wPct;
        this.hPct = hPct;
    }

    /** Convert normalized point (xPct,yPct) inside CLIENT -> absolute WIN32 screen Point */
    public static Point toScreenPoint(double xPct, double yPct, Rectangle clientRectOnScreen) {
        if (clientRectOnScreen == null) throw new IllegalArgumentException("clientRectOnScreen is null");
        if (clientRectOnScreen.width <= 0 || clientRectOnScreen.height <= 0) {
            throw new IllegalArgumentException("Invalid clientRectOnScreen size: " + clientRectOnScreen);
        }

        double cx = clamp01(xPct);
        double cy = clamp01(yPct);

        // -1 keeps 1.0 landing on the last pixel inside the client rect
        int x = clientRectOnScreen.x + (int) Math.round(cx * (clientRectOnScreen.width - 1));
        int y = clientRectOnScreen.y + (int) Math.round(cy * (clientRectOnScreen.height - 1));

        return new Point(x, y);
    }

    /** Convert this normalized region -> absolute WIN32 screen rectangle based on current client rect */
    public Rectangle toScreenRect(Rectangle clientRectOnScreen) {
        if (clientRectOnScreen == null) throw new IllegalArgumentException("clientRectOnScreen is null");
        if (clientRectOnScreen.width <= 0 || clientRectOnScreen.height <= 0) {
            throw new IllegalArgumentException("Invalid clientRectOnScreen size: " + clientRectOnScreen);
        }

        double nx = clamp01(xPct);
        double ny = clamp01(yPct);
        double nw = clamp01(wPct);
        double nh = clamp01(hPct);

        int x = clientRectOnScreen.x + (int) Math.round(clientRectOnScreen.width * nx);
        int y = clientRectOnScreen.y + (int) Math.round(clientRectOnScreen.height * ny);
        int w = (int) Math.round(clientRectOnScreen.width * nw);
        int h = (int) Math.round(clientRectOnScreen.height * nh);

        // Ensure OCR/cropping never gets a 0-sized rect
        w = Math.max(1, w);
        h = Math.max(1, h);

        Rectangle screenRect = new Rectangle(x, y, w, h);
        return clampToClient(screenRect, clientRectOnScreen, true);
    }

    /**
     * Create normalized region from a chosen screen pixel rect inside the client rect.
     * We clamp the requested rect into the client bounds so Record Mode can be sloppy
     * without producing negative/overflow percentages.
     */
    public static NormalizedRegion fromScreenRect(Rectangle screenRect, Rectangle clientRectOnScreen) {
        if (screenRect == null) throw new IllegalArgumentException("screenRect is null");
        if (clientRectOnScreen == null) throw new IllegalArgumentException("clientRectOnScreen is null");
        if (clientRectOnScreen.width <= 0 || clientRectOnScreen.height <= 0) {
            throw new IllegalArgumentException("Invalid clientRectOnScreen size: " + clientRectOnScreen);
        }

        Rectangle r = clampToClient(screenRect, clientRectOnScreen, false);
        if (r.width <= 0 || r.height <= 0) {
            throw new IllegalArgumentException(
                    "screenRect produced empty region after clamp. screenRect=" + screenRect +
                    " clientRectOnScreen=" + clientRectOnScreen
            );
        }

        double xPct = (r.x - clientRectOnScreen.x) / (double) clientRectOnScreen.width;
        double yPct = (r.y - clientRectOnScreen.y) / (double) clientRectOnScreen.height;
        double wPct = r.width / (double) clientRectOnScreen.width;
        double hPct = r.height / (double) clientRectOnScreen.height;

        // Final clamp to keep constants safe
        xPct = clamp01(xPct);
        yPct = clamp01(yPct);
        wPct = clamp01(wPct);
        hPct = clamp01(hPct);

        return new NormalizedRegion(xPct, yPct, wPct, hPct);
    }

    /** Clamp a screen rect to be fully within client bounds. */
    private static Rectangle clampToClient(Rectangle r, Rectangle client, boolean ensureNonZeroSize) {
        int x1 = Math.max(r.x, client.x);
        int y1 = Math.max(r.y, client.y);
        int x2 = Math.min(r.x + r.width, client.x + client.width);
        int y2 = Math.min(r.y + r.height, client.y + client.height);

        int w = Math.max(0, x2 - x1);
        int h = Math.max(0, y2 - y1);

        if (ensureNonZeroSize) {
            w = Math.max(1, w);
            h = Math.max(1, h);

            // If clamping collapsed to an edge, pull it back inside by 1 pixel
            if (x1 == client.x + client.width) x1 = client.x + client.width - 1;
            if (y1 == client.y + client.height) y1 = client.y + client.height - 1;
        }

        return new Rectangle(x1, y1, w, h);
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    @Override
    public String toString() {
        return String.format(
                "NormalizedRegion{xPct=%.6f,yPct=%.6f,wPct=%.6f,hPct=%.6f}",
                xPct, yPct, wPct, hPct
        );
    }
}
