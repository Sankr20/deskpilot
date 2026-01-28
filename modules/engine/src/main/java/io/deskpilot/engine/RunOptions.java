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

    private RunOptions(Builder b) {
        this.runsDir = Objects.requireNonNull(b.runsDir, "runsDir is null");
        this.runName = Objects.requireNonNull(b.runName, "runName is null").trim();
        if (this.runName.isEmpty()) throw new IllegalArgumentException("runName is blank");

        this.stepScreenshots = b.stepScreenshots;
        this.bringToFrontOnAttach = b.bringToFrontOnAttach;
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
    }
}
