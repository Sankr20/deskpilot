package io.deskpilot.engine.runtime;

import java.time.Duration;

public final class ActionOptions {

    private final Duration timeout;
    private final Duration pollInterval;

    private ActionOptions(Duration timeout, Duration pollInterval) {
        this.timeout = timeout;
        this.pollInterval = pollInterval;
    }

    public static ActionOptions defaults() {
        return new ActionOptions(Duration.ofSeconds(8), Duration.ofMillis(250));
    }

    public ActionOptions withTimeout(Duration timeout) {
        return new ActionOptions(timeout, this.pollInterval);
    }

    public ActionOptions withPollInterval(Duration pollInterval) {
        return new ActionOptions(this.timeout, pollInterval);
    }

    public Duration timeout() {
        return timeout;
    }

    public Duration pollInterval() {
        return pollInterval;
    }
}
