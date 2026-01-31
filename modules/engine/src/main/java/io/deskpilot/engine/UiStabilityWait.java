package io.deskpilot.engine;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public final class UiStabilityWait {

    private UiStabilityWait() {}

    /**
     * Waits until the client area becomes visually stable (diff stays below threshold for stableMs).
     *
     * This is our Playwright-style “auto-wait” primitive for desktop apps.
     *
     * @param driver DesktopDriver for screenshots
     * @param clientRectRobot client rect in ROBOT coordinates
     * @param timeoutMs total time to wait
     * @param stableMs how long it must remain stable continuously
     * @param diffThreshold01 max allowed diff between frames (0..1)
     * @param pollMs polling interval
     */
    public static void waitForStable(DesktopDriver driver,
                                     Rectangle clientRectRobot,
                                     long timeoutMs,
                                     long stableMs,
                                     double diffThreshold01,
                                     long pollMs) throws Exception {

        long end = System.currentTimeMillis() + timeoutMs;

        BufferedImage prev = driver.screenshot(clientRectRobot);

        long stableStart = -1;

        while (System.currentTimeMillis() < end) {
            Thread.sleep(pollMs);

            BufferedImage now = driver.screenshot(clientRectRobot);
            double diff = imageDiff01(prev, now);

            if (diff <= diffThreshold01) {
                if (stableStart < 0) stableStart = System.currentTimeMillis();

                long stableFor = System.currentTimeMillis() - stableStart;
                if (stableFor >= stableMs) {
                    System.out.println("UI stable. diff=" + diff + " stableMs=" + stableFor);
                    return;
                }
            } else {
                // UI changed -> reset stability timer
                stableStart = -1;
            }

            prev = now;
        }

        throw new RuntimeException("Timeout waiting for UI to become stable. timeoutMs=" + timeoutMs);
    }

    /**
     * Lightweight diff metric: samples pixels on a stride grid.
     */
    static double imageDiff01(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());

        long total = 0;
        long changed = 0;

        // Higher stride = faster. This is a global stability check, not precision comparison.
        int stride = 8;

        for (int y = 0; y < h; y += stride) {
            for (int x = 0; x < w; x += stride) {
                total++;
                if (a.getRGB(x, y) != b.getRGB(x, y)) changed++;
            }
        }

        return total == 0 ? 0 : (changed / (double) total);
    }
}
