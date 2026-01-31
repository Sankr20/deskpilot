package io.deskpilot.engine.runtime;

import io.deskpilot.engine.actions.ActionExpectation;
import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.locators.LocatorResult;

public final class ActionFailedException extends RuntimeException {

    private final String stepName;
    private final String locatorLabel;
    private final ActionExpectation[] expectations;
    private final int attempts;
    private final LocatorResult lastResult;

    public ActionFailedException(
            String stepName,
            Locator locator,
            ActionExpectation[] expectations,
            int attempts,
            LocatorResult last,
            String message,
            Exception cause
    ) {
        super(message, cause);
        this.stepName = stepName;
        this.locatorLabel = (locator == null ? "<null>" : locator.label());
        this.expectations = (expectations == null ? new ActionExpectation[0] : expectations);
        this.attempts = attempts;
        this.lastResult = last;
    }

    // Backwards-compatible overload (optional, but handy)
    public ActionFailedException(
            String stepName,
            Locator locator,
            ActionExpectation expectation,
            int attempts,
            LocatorResult last,
            String message,
            Exception cause
    ) {
        this(stepName, locator, new ActionExpectation[]{expectation}, attempts, last, message, cause);
    }

    public String stepName() { return stepName; }
    public String locatorLabel() { return locatorLabel; }
    public ActionExpectation[] expectations() { return expectations; }
    public int attempts() { return attempts; }
    public LocatorResult lastResult() { return lastResult; }
}
