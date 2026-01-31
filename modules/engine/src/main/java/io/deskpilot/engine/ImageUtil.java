package io.deskpilot.engine;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public final class ImageUtil {
    private ImageUtil() {}

    public static BufferedImage crop(BufferedImage img, Rectangle r) {
        if (img == null) throw new IllegalArgumentException("img is null");
        if (r == null) throw new IllegalArgumentException("rect is null");

        int x = clamp(r.x, 0, img.getWidth() - 1);
        int y = clamp(r.y, 0, img.getHeight() - 1);

        int w = Math.max(1, Math.min(r.width, img.getWidth() - x));
        int h = Math.max(1, Math.min(r.height, img.getHeight() - y));

        return img.getSubimage(x, y, w, h);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
    public static BufferedImage loadResource(String path) {
    if (path == null || path.trim().isEmpty()) {
        throw new IllegalArgumentException("Image resource path is null/empty");
    }

    String p = path.startsWith("/") ? path.substring(1) : path;

    try (java.io.InputStream is =
                 ImageUtil.class.getClassLoader().getResourceAsStream(p)) {

        if (is == null) {
            throw new RuntimeException("Image resource not found on classpath: " + p);
        }

        BufferedImage img = javax.imageio.ImageIO.read(is);
        if (img == null) {
            throw new RuntimeException("Failed to decode image resource: " + p);
        }

        return img;

    } catch (Exception e) {
        throw new RuntimeException("Failed to load image resource: " + p, e);
    }
}

}
