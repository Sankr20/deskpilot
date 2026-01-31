package io.deskpilot.engine;

public final class StabilityOptions {
    public final boolean enabled;
    public final boolean bringToFront;
    public final long pollMs;
    public final long stableForMs;
    public final long timeoutMs;
    public final double diffThreshold;

    public StabilityOptions(
            boolean enabled,
            boolean bringToFront,
            long pollMs,
            long stableForMs,
            long timeoutMs,
            double diffThreshold
    ) {
        this.enabled = enabled;
        this.bringToFront = bringToFront;
        this.pollMs = pollMs;
        this.stableForMs = stableForMs;
        this.timeoutMs = timeoutMs;
        this.diffThreshold = diffThreshold;
    }

    public static StabilityOptions defaults() {
        return new StabilityOptions(
                true,
                true,
                120,
                450,
                6000,
                0.002
        );
    }
}
