package io.deskpilot.common;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;

import static java.nio.file.StandardOpenOption.*;

public final class SafePaths {
    private SafePaths() {}

    public static Path root(Path dir) throws IOException {
        if (dir == null) throw new IllegalArgumentException("dir is null");
        Path abs = dir.toAbsolutePath().normalize();
        if (Files.exists(abs)) return abs.toRealPath().normalize();
        return abs;
    }

    public static void ensureDir(Path dir) throws IOException {
        if (dir == null) throw new IllegalArgumentException("dir is null");
        Files.createDirectories(dir);
    }

    public static void ensureDirEmptyOrForce(Path dir, boolean force) throws IOException {
        if (dir == null) throw new IllegalArgumentException("dir is null");
        if (!Files.exists(dir)) return;
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Target exists but is not a directory: " + dir);
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            if (ds.iterator().hasNext() && !force) {
                throw new IllegalArgumentException(
                        "Refusing to write into non-empty directory: " + dir + "\n" +
                                "Re-run with --force to overwrite."
                );
            }
        }
    }

    public static Path under(Path root, String... parts) {
        if (root == null) throw new IllegalArgumentException("root is null");
        Path out = root;
        for (String p : parts) out = out.resolve(p);
        out = out.toAbsolutePath().normalize();

        Path normRoot = root.toAbsolutePath().normalize();
        if (!out.startsWith(normRoot)) {
            throw new IllegalArgumentException("Refusing to write outside root. root=" + normRoot + " target=" + out);
        }
        return out;
    }

    /** Guardrail: ensure a target path is under the given root. */
    public static Path requireUnderRoot(Path root, Path target) {
        if (root == null) throw new IllegalArgumentException("root is null");
        if (target == null) throw new IllegalArgumentException("target is null");

        Path r = root.toAbsolutePath().normalize();
        Path t = target.toAbsolutePath().normalize();
        if (!t.startsWith(r)) {
            throw new IllegalArgumentException("Refusing to write outside projectDir. root=" + r + " target=" + t);
        }
        return t;
    }

    public static void writeString(Path file, String content, boolean force) throws IOException {
        if (file == null) throw new IllegalArgumentException("file is null");
        ensureDir(file.getParent());
        if (force) {
            Files.writeString(file, content, StandardCharsets.UTF_8, CREATE, TRUNCATE_EXISTING, WRITE);
        } else {
            Files.writeString(file, content, StandardCharsets.UTF_8, CREATE_NEW, WRITE);
        }
    }

    public static void validateJavaPackageOrThrow(String pkg) {
        if (pkg == null || pkg.isBlank()) return;
        String p = pkg.trim();
        if (!p.matches("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$")) {
            throw new IllegalArgumentException("Invalid package name: " + pkg);
        }
    }

    public static void rejectReservedWindowsName(String name) {
        if (name == null) return;

        String raw = name.trim();

        // Windows: disallow trailing dots/spaces in filenames
        while (raw.endsWith(".") || raw.endsWith(" ")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name: " + name);
        }

        String n = raw.toLowerCase(Locale.ROOT);

        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) n = n.substring(slash + 1);

        // reject "." and ".."
        if (n.equals(".") || n.equals("..")) {
            throw new IllegalArgumentException("Invalid file name: " + name);
        }

        // strip extension for reserved-name check
        int dot = n.indexOf('.');
        if (dot > 0) n = n.substring(0, dot);

        if (n.equals("con") || n.equals("prn") || n.equals("aux") || n.equals("nul")) {
            throw new IllegalArgumentException("Reserved Windows name: " + name);
        }
        if (n.matches("^com[1-9]$") || n.matches("^lpt[1-9]$")) {
            throw new IllegalArgumentException("Reserved Windows name: " + name);
        }
    }
}
