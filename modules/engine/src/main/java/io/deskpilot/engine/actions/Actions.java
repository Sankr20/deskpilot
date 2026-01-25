package io.deskpilot.engine.actions;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.runtime.ActionOptions;
import io.deskpilot.engine.runtime.ActionStep;

import java.time.Duration;
import java.util.Objects;

public final class Actions {

    private final DeskPilotSession s;
    private final ActionOptions options;
    private final ActionStep.Stabilizer stabilizer;

    public Actions(DeskPilotSession s) {
        this(s, ActionOptions.defaults(), null);
    }

    private Actions(DeskPilotSession s, ActionOptions options, ActionStep.Stabilizer stabilizer) {
        this.s = Objects.requireNonNull(s, "session is null");
        this.options = Objects.requireNonNull(options, "options is null");
        this.stabilizer = stabilizer; // can be null
    }

    // --- fluent config -------------------------------------------------------

    public Actions withTimeout(Duration timeout) {
        return new Actions(s, options.withTimeout(timeout), stabilizer);
    }

    public Actions withPoll(Duration poll) {
        return new Actions(s, options.withPollInterval(poll), stabilizer);
    }

    /** Optional: run before locate each attempt (refresh, small sleeps, etc.) */
    public Actions withStabilizer(ActionStep.Stabilizer stabilizer) {
        return new Actions(s, options, stabilizer);
    }

    // --- primitives ----------------------------------------------------------

    public Actions click(Locator locator) throws Exception {
        Objects.requireNonNull(locator, "locator is null");

        ActionStep.run(
                s,
                StepNamer.click(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> ss.clickWin32(r.point),
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /** Paste text into a point-like locator: click + paste (clipboard + Ctrl+V). */
    public Actions paste(Locator locator, String text) throws Exception {
        Objects.requireNonNull(locator, "locator is null");
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                StepNamer.paste(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.clickWin32(r.point);
                    Thread.sleep(120);  // focus settle
                    ss.paste(text);
                    Thread.sleep(80);   // allow UI update
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /**
     * Canonical desktop "fill":
     * click -> Ctrl+A -> paste
     * This is far more reliable than typing chars.
     */
    public Actions fill(Locator locator, String text) throws Exception {
        Objects.requireNonNull(locator, "locator is null");
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                StepNamer.fill(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.clickWin32(r.point);
                    Thread.sleep(120);
                    ss.selectAll();
                    Thread.sleep(60);
                    ss.paste(text);
                    Thread.sleep(80);
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /**
     * Type text into a point-like locator (fallback).
     * Prefer fill() unless you specifically need key-by-key behavior.
     */
    public Actions type(Locator locator, String text) throws Exception {
        Objects.requireNonNull(locator, "locator is null");
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                StepNamer.type(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.clickWin32(r.point);
                    Thread.sleep(120);
                    ss.typeText(text);
                    Thread.sleep(80);
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /**
     * Wait until a locator is FOUND (no click).
     * For OCR locators, "FOUND" should mean the text condition is met.
     */
    public Actions waitFor(Locator locator) throws Exception {
        Objects.requireNonNull(locator, "locator is null");

        ActionStep.run(
                s,
                StepNamer.waitText(locator),
                locator,
                options,
                stabilizer,
                null,
                null,
                ActionExpectation.MUST_EXIST
        );
        return this;
    }

    public Actions clickAndWait(Locator clickTarget, Locator waitTarget) throws Exception {
    Objects.requireNonNull(clickTarget, "clickTarget is null");
    Objects.requireNonNull(waitTarget, "waitTarget is null");

    click(clickTarget);
    waitFor(waitTarget);

    return this;
}
public Actions fillAndWait(Locator fieldPoint, String text, Locator waitTarget) throws Exception {
    Objects.requireNonNull(fieldPoint, "fieldPoint is null");
    Objects.requireNonNull(waitTarget, "waitTarget is null");

    fill(fieldPoint, text);
    waitFor(waitTarget);

    return this;
}

public Actions fillClickAndWait(Locator fieldPoint, String text, Locator clickTarget, Locator waitTarget) throws Exception {
    Objects.requireNonNull(fieldPoint, "fieldPoint is null");
    Objects.requireNonNull(text, "text is null");
    Objects.requireNonNull(clickTarget, "clickTarget is null");
    Objects.requireNonNull(waitTarget, "waitTarget is null");

    fill(fieldPoint, text);
    clickAndWait(clickTarget, waitTarget);

    return this;
}



private static void shortSettle(long ms) {
    try { Thread.sleep(ms); }
    catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
}

}
