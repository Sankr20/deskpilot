package io.deskpilot.engine;

import java.awt.*;
import java.awt.geom.AffineTransform;

public final class RobotCoords {

    private RobotCoords() {}

    /** Find the GraphicsConfiguration whose bounds overlap this rect the most. */
    public static GraphicsConfiguration findConfigFor(Rectangle r) {
        GraphicsDevice best = null;
        double bestArea = -1;

        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle b = gc.getBounds();

            Rectangle inter = r.intersection(b);
            double area = Math.max(0, inter.getWidth()) * Math.max(0, inter.getHeight());

            if (area > bestArea) {
                bestArea = area;
                best = gd;
            }
        }
        return best == null ? GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                : best.getDefaultConfiguration();
    }

    /**
     * Converts a rect that came from Win32 screen coords into the coordinate space used by Java Robot
     * by applying the inverse of the monitor's default transform.
     */
    public static Rectangle toRobotRect(Rectangle win32Rect) {
        GraphicsConfiguration gc = findConfigFor(win32Rect);

        // This transform maps "user space" -> "device pixels" for that monitor.
        AffineTransform tx = gc.getDefaultTransform();

        // We need inverse: device pixels -> user space (Robot space)
        double sx = tx.getScaleX();
        double sy = tx.getScaleY();

        if (sx == 0 || sy == 0) return win32Rect;

        int x = (int) Math.round(win32Rect.x / sx);
        int y = (int) Math.round(win32Rect.y / sy);
        int w = (int) Math.round(win32Rect.width / sx);
        int h = (int) Math.round(win32Rect.height / sy);

        return new Rectangle(x, y, w, h);
    }

    public static Point toRobotPoint(Point win32Point) {
    Rectangle r = new Rectangle(win32Point.x, win32Point.y, 1, 1);
    GraphicsConfiguration gc = findConfigFor(r);

    AffineTransform tx = gc.getDefaultTransform();
    double sx = tx.getScaleX();
    double sy = tx.getScaleY();

    if (sx == 0 || sy == 0) return new Point(win32Point);

    int x = (int) Math.round(win32Point.x / sx);
    int y = (int) Math.round(win32Point.y / sy);

    return new Point(x, y);
}
public static Point toWin32Point(Point robotPoint, Rectangle referenceWin32Rect) {
    GraphicsConfiguration gc = findConfigFor(referenceWin32Rect);

    AffineTransform tx = gc.getDefaultTransform();
    double sx = tx.getScaleX();
    double sy = tx.getScaleY();

    if (sx == 0 || sy == 0) return new Point(robotPoint);

    int x = (int) Math.round(robotPoint.x * sx);
    int y = (int) Math.round(robotPoint.y * sy);

    return new Point(x, y);
}



}
