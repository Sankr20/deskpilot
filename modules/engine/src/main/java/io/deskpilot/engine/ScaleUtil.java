package io.deskpilot.engine;

import java.awt.*;

public final class ScaleUtil {

    private ScaleUtil() {}

    /**
     * Returns an approximate scale factor for the monitor that contains the given rect.
     * Works well for mixed-DPI setups (laptop 125%, external 100%).
     */
    public static double getScaleForRect(Rectangle targetRect) {

        GraphicsDevice bestDevice = null;
        double bestArea = -1;

        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle b = gd.getDefaultConfiguration().getBounds();

            Rectangle inter = targetRect.intersection(b);
            double area = Math.max(0, inter.getWidth()) * Math.max(0, inter.getHeight());

            if (area > bestArea) {
                bestArea = area;
                bestDevice = gd;
            }
        }

        if (bestDevice == null) return 1.0;

        // Physical pixel width for this monitor
        int physicalW = bestDevice.getDisplayMode().getWidth();

        // "Logical" width from the monitor bounds in the JVM coordinate space
        Rectangle logicalBounds = bestDevice.getDefaultConfiguration().getBounds();
        double logicalW = logicalBounds.getWidth();

        if (logicalW <= 0) return 1.0;

        double scale = physicalW / logicalW;

        // clamp
        if (scale < 0.5 || scale > 4.0) return 1.0;

        return scale;
    }

    public static Rectangle scaleRect(Rectangle r, double s) {
        return new Rectangle(
                (int) Math.round(r.x * s),
                (int) Math.round(r.y * s),
                (int) Math.round(r.width * s),
                (int) Math.round(r.height * s)
        );
    }
}
