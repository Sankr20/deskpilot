package io.deskpilot.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * PRD: RunOptions controls how a DeskPilot run is created.
 *
 * Design goals:
 * - Deterministic grouping by <class>/<method>
 * - Unique run instance per execution (no artifact mixing)
 * - Minimal, stable public surface
 */
public final class RunOptions {

    public static final Path DEFAULT_RUNS_DIR = Paths.get("runs");

    private final Path runsDir;
    private final String runName;   // e.g. ExampleTest/attach_smoke
    private final String runId;     // e.g. run-20260203_203112

    // Toggles (kept intentionally small)
    private final boolean stepScreenshots;
    private final boolean bringToFrontOnAttach;
    private final long attachTimeoutMs;

    private RunOptions(Builder b) {
        this.runsDir = Objects.requireNonNull(b.runsDir, "runsDir is null");
        this.runName = requireNonBlank(b.runName, "runName");
        this.runId = (b.runId == null || b.runId.isBlank())
                ? defaultRunId()
                : safeName(b.runId);

        this.stepScreenshots = b.stepScreenshots;
        this.bringToFrontOnAttach = b.bringToFrontOnAttach;

        this.attachTimeoutMs = b.attachTimeoutMs;
        if (this.attachTimeoutMs <= 0) {
            throw new IllegalArgumentException("attachTimeoutMs must be > 0");
        }
    }

    /* -------------------- accessors -------------------- */

    public Path runsDir() {
        return runsDir;
    }

    public String runName() {
        return runName;
    }

    public String runId() {
        return runId;
    }

    /** Full run directory: runs/<runName>/<runId> */
    public Path runDir() {
        return runsDir.normalize()
                .resolve(runName)
                .resolve(runId);
    }

    public boolean stepScreenshots() {
        return stepScreenshots;
    }

    public boolean bringToFrontOnAttach() {
        return bringToFrontOnAttach;
    }

    public long attachTimeoutMs() {
        return attachTimeoutMs;
    }

    public StabilityOptions stability() {
        StabilityOptions d = StabilityOptions.defaults();
        return new StabilityOptions(
                d.enabled,
                this.bringToFrontOnAttach,
                d.pollMs,
                d.stableForMs,
                d.timeoutMs,
                d.diffThreshold
        );
    }

    /* -------------------- builder -------------------- */

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path runsDir = DEFAULT_RUNS_DIR;
        private String runName = "run";
        private String runId = null; // null = auto-generate

        private boolean stepScreenshots = Boolean.getBoolean("deskpilot.stepScreenshots");
        private boolean bringToFrontOnAttach = true;
        private long attachTimeoutMs =
                Long.getLong("deskpilot.attachTimeoutMs", 30_000L);

        private Builder() {}

        public Builder runsDir(Path runsDir) {
            this.runsDir = Objects.requireNonNull(runsDir, "runsDir is null");
            return this;
        }

        /** Logical grouping name (usually <class>/<method>) */
        public Builder runName(String runName) {
            this.runName = requireNonBlank(runName, "runName");
            return this;
        }

        /** Optional explicit run id (otherwise auto-generated) */
        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public Builder stepScreenshots(boolean enabled) {
            this.stepScreenshots = enabled;
            return this;
        }

        public Builder bringToFrontOnAttach(boolean enabled) {
            this.bringToFrontOnAttach = enabled;
            return this;
        }

        public Builder attachTimeoutMs(long ms) {
            this.attachTimeoutMs = ms;
            return this;
        }

        public RunOptions build() {
            return new RunOptions(this);
        }
    }

    /* -------------------- filesystem -------------------- */

    /**
     * Creates and returns the run folder:
     *
     * runs/<runName>/<runId>/
     *   01-startup/
     *     metadata.json
     */
    public static Path prepareRunFolder(RunOptions opts) throws java.io.IOException {
        if (opts == null) throw new IllegalArgumentException("opts is null");

        Path runFolder = opts.runDir();
        Files.createDirectories(runFolder);

        // PRD: reserve startup folder
        Path startup = runFolder.resolve("01-startup");
        Files.createDirectories(startup);

        String json = "{\n" +
                "  \"runName\": " + quote(opts.runName) + ",\n" +
                "  \"runId\": " + quote(opts.runId) + ",\n" +
                "  \"runsDir\": " + quote(opts.runsDir.toAbsolutePath().toString()) + ",\n" +
                "  \"createdAt\": " + quote(OffsetDateTime.now().toString()) + "\n" +
                "}\n";

        Files.writeString(startup.resolve("metadata.json"), json);

        return runFolder;
    }

    /* -------------------- helpers -------------------- */

    private static String defaultRunId() {
        // Windows-safe, readable, sortable
        return "run-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private static String requireNonBlank(String s, String label) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is blank");
        }
        return s.trim();
    }

    private static String safeName(String s) {
        return s.replaceAll("[^a-zA-Z0-9._/-]+", "_");
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
