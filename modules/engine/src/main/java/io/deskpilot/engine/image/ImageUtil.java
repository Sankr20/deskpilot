package io.deskpilot.engine.image;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class ImageUtil {

    // existing crop(...) stays untouched

    public static BufferedImage loadResource(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Image path is null or empty");
        }

        try (InputStream is = ImageUtil.class
                .getClassLoader()
                .getResourceAsStream(path)) {

            if (is == null) {
                throw new RuntimeException("Image resource not found on classpath: " + path);
            }

            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                throw new RuntimeException("Failed to decode image: " + path);
            }
            return img;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load image resource: " + path, e);
        }
    }

    public static void savePng(java.awt.image.BufferedImage img, String resourceLikePath) throws Exception {
    // resourceLikePath: "icons/save_icon.png"
    java.nio.file.Path out = java.nio.file.Paths.get("modules", "engine", "src", "main", "resources", resourceLikePath);
    java.nio.file.Files.createDirectories(out.getParent());
    javax.imageio.ImageIO.write(img, "png", out.toFile());
}
public static BufferedImage crop(BufferedImage src, Rectangle r) {
    if (src == null) throw new IllegalArgumentException("src is null");
    if (r == null) throw new IllegalArgumentException("r is null");

    Rectangle bounds = new Rectangle(0, 0, src.getWidth(), src.getHeight());
    Rectangle c = r.intersection(bounds);

    if (c.isEmpty() || c.width <= 0 || c.height <= 0) {
        throw new IllegalArgumentException("crop rect out of bounds: " + r + " src=" + bounds);
    }

    BufferedImage out = new BufferedImage(c.width, c.height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = out.createGraphics();
    g.drawImage(src, 0, 0, c.width, c.height, c.x, c.y, c.x + c.width, c.y + c.height, null);
    g.dispose();
    return out;
}


}
