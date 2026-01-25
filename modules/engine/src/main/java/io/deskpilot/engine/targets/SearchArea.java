package io.deskpilot.engine.targets;

import java.awt.Rectangle;

public class SearchArea {

    public final double xPct;
    public final double yPct;
    public final double wPct;
    public final double hPct;

    public SearchArea(double xPct, double yPct, double wPct, double hPct) {
        this.xPct = xPct;
        this.yPct = yPct;
        this.wPct = wPct;
        this.hPct = hPct;
    }

    public Rectangle toClientPixels(int clientW, int clientH) {
        int x = (int) Math.round(xPct * clientW);
        int y = (int) Math.round(yPct * clientH);
        int w = (int) Math.round(wPct * clientW);
        int h = (int) Math.round(hPct * clientH);

        // clamp
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x + w > clientW) w = clientW - x;
        if (y + h > clientH) h = clientH - y;

        return new Rectangle(x, y, Math.max(0, w), Math.max(0, h));
    }

    public static SearchArea topLeft(double wPct, double hPct) {
        return new SearchArea(0, 0, wPct, hPct);
    }
}
