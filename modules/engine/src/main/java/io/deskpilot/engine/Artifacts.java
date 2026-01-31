package io.deskpilot.engine;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Milestone 12: unified artifact layout.
 *
 * Each step gets its own folder:
 *   runs/<runName>/<NN>-<label>/...
 */
public final class Artifacts {

    private final Path outDir;
    private final AtomicInteger step = new AtomicInteger(0);

    public Artifacts(Path outDir) {
        if (outDir == null) throw new IllegalArgumentException("outDir is null");
        this.outDir = outDir;
    }

    public Path outDir() {
        return outDir;
    }

    /** Starts a step and returns the step folder path. */
    public Path stepDir(String label) throws Exception {
        int n = step.incrementAndGet();
        String dirName = String.format(Locale.US, "%02d-%s", n, safeFile(label));
        Path dir = outDir.resolve(dirName);
        Files.createDirectories(dir);
        return dir;
    }

    public Path savePng(Path stepDir, String fileName, BufferedImage img) throws Exception {
        if (stepDir == null) throw new IllegalArgumentException("stepDir is null");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName blank");
        if (img == null) throw new IllegalArgumentException("img is null");

        String n = safeFile(fileName);
        if (!n.toLowerCase(Locale.US).endsWith(".png")) n = n + ".png";
        Path file = stepDir.resolve(n);
        javax.imageio.ImageIO.write(img, "png", file.toFile());
        return file;
    }

    public Path saveText(Path stepDir, String fileName, String text) throws Exception {
        if (stepDir == null) throw new IllegalArgumentException("stepDir is null");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName blank");
        if (text == null) text = "";
        String n = safeFile(fileName);
        Path file = stepDir.resolve(n);
        Files.writeString(file, text);
        return file;
    }

    static String safeFile(String s) {
        if (s == null) return "";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "-");
    }
}
