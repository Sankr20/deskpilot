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
 *
 * PRD 2.2: step numbering must not collide with 01-startup when it exists.
 */
public final class Artifacts {

    private final Path outDir;
    private final AtomicInteger step;

   public Artifacts(Path outDir) throws Exception {
    if (outDir == null) throw new IllegalArgumentException("outDir is null");
    this.outDir = outDir;

    // PRD 2.2: 01-startup is reserved, so first real step is 02-...
    int maxSeen = 1;

    try (var stream = Files.list(outDir)) {
        for (Path p : stream.toList()) {
            if (!Files.isDirectory(p)) continue;

            String name = p.getFileName().toString();

            // match "NN-..."
            if (name.length() >= 3
                    && Character.isDigit(name.charAt(0))
                    && Character.isDigit(name.charAt(1))
                    && name.charAt(2) == '-') {
                try {
                    int n = Integer.parseInt(name.substring(0, 2));
                    if (n > maxSeen) maxSeen = n;
                } catch (NumberFormatException ignore) {}
            }
        }
    }

    // step holds "last allocated step number"
    this.step = new AtomicInteger(maxSeen);
}



    public Path outDir() {
        return outDir;
    }

    /** Starts a step and returns the step folder path. */
public Path stepDir(String label) throws Exception {
    String safe = safeFile(label);
    if ("startup".equalsIgnoreCase(safe)) safe = "startup_step"; // avoid collision

    int n = step.incrementAndGet();
    if (n == 1) n = step.incrementAndGet(); // never allocate 01

    String dirName = String.format(Locale.US, "%02d-%s", n, safe);
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
