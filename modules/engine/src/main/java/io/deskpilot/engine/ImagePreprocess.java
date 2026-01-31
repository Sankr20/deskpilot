package io.deskpilot.engine;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class ImagePreprocess {

    private ImagePreprocess() {}

    public static BufferedImage toGray(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    public static BufferedImage scale(BufferedImage src, double factor) {
        int w = Math.max(1, (int) Math.round(src.getWidth() * factor));
        int h = Math.max(1, (int) Math.round(src.getHeight() * factor));

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();

        return out;
    }

    public static BufferedImage threshold(BufferedImage img, int t) {
    if (img == null) throw new IllegalArgumentException("img is null");
    if (t < 0 || t > 255) throw new IllegalArgumentException("threshold must be 0..255");

    int w = img.getWidth();
    int h = img.getHeight();

    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            int rgb = img.getRGB(x, y);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = (rgb) & 0xFF;

            // Convert to luminance (works even if already gray)
            int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);

            int v = (lum >= t) ? 255 : 0;
            int grayRgb = (v << 16) | (v << 8) | v;
            out.setRGB(x, y, 0xFF000000 | grayRgb);
        }
    }
    return out;
}

}
