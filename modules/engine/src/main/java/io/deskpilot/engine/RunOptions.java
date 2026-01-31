package io.deskpilot.engine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * PRD: RunOptions controls how a DeskPilot run is created (where artifacts go, run name, etc).
 *
 * Keep this minimal and stable: tests should prefer supplying RunOptions over touching internals.
 */
public final class RunOptions {

    public static final Path DEFAULT_RUNS_DIR = Paths.get("runs");

    private final Path runsDir;
    private final String runName;

    // Optional toggles (kept intentionally small; we can extend without breaking callers)
    private final boolean stepScreenshots;
    private final boolean bringToFrontOnAttach;
    private final long attachTimeoutMs;


    private RunOptions(Builder b) {
        this.bringToFrontOnAttach = b.bringToFrontOnAttach;
        this.runsDir = Objects.requireNonNull(b.runsDir, "runsDir is null");
        this.runName = Objects.requireNonNull(b.runName, "runName is null").trim();
        if (this.runName.isEmpty()) throw new IllegalArgumentException("runName is blank");
        this.attachTimeoutMs = b.attachTimeoutMs;
if (this.attachTimeoutMs <= 0) throw new IllegalArgumentException("attachTimeoutMs must be > 0");
        this.stepScreenshots = b.stepScreenshots;
    }

    public Path runsDir() { return runsDir; }
    public String runName() { return runName; }

    public boolean stepScreenshots() { return stepScreenshots; }
    public boolean bringToFrontOnAttach() { return bringToFrontOnAttach; }

    public Path runDir() {
        return runsDir.normalize().resolve(runName);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Path runsDir = DEFAULT_RUNS_DIR;
        private String runName = "run";

        private boolean stepScreenshots = Boolean.getBoolean("deskpilot.stepScreenshots");
        private boolean bringToFrontOnAttach = true;
        private long attachTimeoutMs = Long.getLong("deskpilot.attachTimeoutMs", 30_000L);


        private Builder() {}

        public Builder runsDir(Path runsDir) {
            this.runsDir = Objects.requireNonNull(runsDir, "runsDir is null");
            return this;
        }

        public Builder runName(String runName) {
            this.runName = Objects.requireNonNull(runName, "runName is null");
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

        public RunOptions build() {
            return new RunOptions(this);
        }

        public Builder attachTimeoutMs(long ms) {
    this.attachTimeoutMs = ms;
    return this;
}

    }

 public static java.nio.file.Path prepareRunFolder(RunOptions opts) throws java.io.IOException {
    if (opts == null) throw new IllegalArgumentException("opts is null");

    java.nio.file.Path base = opts.runsDir() != null ? opts.runsDir() : DEFAULT_RUNS_DIR;
    String name = (opts.runName() != null && !opts.runName().isBlank()) ? opts.runName() : "run";
    java.nio.file.Path runFolder = base.resolve(name);
    java.nio.file.Files.createDirectories(runFolder);

    // ✅ PRD: reserve 01-startup
    java.nio.file.Path startup = runFolder.resolve("01-startup");
    java.nio.file.Files.createDirectories(startup);

    String json = "{\n" +
            "  \"runName\": " + quote(name) + ",\n" +
            "  \"runsDir\": " + quote(base.toAbsolutePath().toString()) + ",\n" +
            "  \"createdAt\": " + quote(java.time.OffsetDateTime.now().toString()) + "\n" +
            "}\n";

    java.nio.file.Files.writeString(
            startup.resolve("metadata.json"),
            json,
            java.nio.charset.StandardCharsets.UTF_8
    );

    return runFolder;
}


private static String quote(String s) {
    if (s == null) return "null";
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
}
public long attachTimeoutMs() { return attachTimeoutMs; }

public StabilityOptions stability() {
    StabilityOptions d = StabilityOptions.defaults();
    return new StabilityOptions(
            d.enabled,
            this.bringToFrontOnAttach, // reuse existing toggle
            d.pollMs,
            d.stableForMs,
            d.timeoutMs,
            d.diffThreshold
    );
}


}
