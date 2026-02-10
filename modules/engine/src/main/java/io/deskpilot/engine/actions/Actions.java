package io.deskpilot.engine.actions;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.locators.LocatorKind;
import io.deskpilot.engine.locators.LocatorResult;
import io.deskpilot.engine.locators.LocatorSession;
import io.deskpilot.engine.runtime.ActionOptions;
import io.deskpilot.engine.runtime.ActionStep;

import java.awt.Point;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * High-level desktop actions.
 *
 * Rules:
 * - Everything goes through ActionStep.run(...) for retries + step artifacts.
 * - Keyboard-only steps still go through ActionStep.run(...) using a safe NOOP locator.
 */
public final class Actions {

    private final DeskPilotSession s;
    private final ActionOptions options;
    private final ActionStep.Stabilizer stabilizer;

    private static final Locator NOOP_LOCATOR = new Locator() {
        @Override public LocatorKind kind() { return LocatorKind.POINT; }
        @Override public String label() { return "noop_keyboard_step"; }

        @Override
        public LocatorResult locate(LocatorSession session) {
            return LocatorResult.found(
                    LocatorKind.POINT,
                    label(),
                    new Point(0, 0),
                    null,
                    -1,
                    Map.of("noop", "true")
            );
        }
    };

    public Actions(DeskPilotSession s) {
        this(s, ActionOptions.defaults(), DeskPilotSession::stabilizeAttempt);
    }

    private Actions(DeskPilotSession s, ActionOptions options, ActionStep.Stabilizer stabilizer) {
        this.s = Objects.requireNonNull(s, "session is null");
        this.options = Objects.requireNonNull(options, "options is null");
        this.stabilizer = stabilizer; // can be null
    }

    // ---- fluent config ----

    public Actions withTimeout(Duration timeout) {
        return new Actions(s, options.withTimeout(timeout), stabilizer);
    }

    public Actions withPoll(Duration poll) {
        return new Actions(s, options.withPollInterval(poll), stabilizer);
    }

    public Actions withStabilizer(ActionStep.Stabilizer stabilizer) {
        return new Actions(s, options, stabilizer);
    }

    // ---- helpers ----

    private static String lbl(Locator locator) {
        try {
            String l = locator == null ? null : locator.label();
            return (l == null || l.isBlank()) ? "<unlabeled>" : l;
        } catch (Exception e) {
            return "<label_error>";
        }
    }

    // ---- primitives ----

    public Actions click(Locator locator) throws Exception {
        Objects.requireNonNull(locator, "locator is null");

        ActionStep.run(
                s,
                "click:" + lbl(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-click");
                    ss.clickWin32(r.point);
                    ss.stabilizeInStep("after-click");
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    public Actions paste(Locator locator, String text) throws Exception {
        Objects.requireNonNull(locator, "locator is null");
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                "paste:" + lbl(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-paste");
                    ss.clickWin32(r.point);
                    ss.paste(text);
                    ss.stabilizeInStep("after-paste");
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /** Canonical: click -> selectAll -> paste */
    public Actions fill(Locator locator, String text) throws Exception {
        Objects.requireNonNull(locator, "locator is null");
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                "fill:" + lbl(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-fill");
                    ss.clickWin32(r.point);
                    ss.selectAll();
                    ss.paste(text);
                    ss.stabilizeInStep("after-fill");
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /** Fallback: click -> typeText */
    public Actions type(Locator locator, String text) throws Exception {
        Objects.requireNonNull(locator, "locator is null");
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                "type:" + lbl(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-type");
                    ss.clickWin32(r.point);
                    ss.typeText(text);
                    ss.stabilizeInStep("after-type");
                },
                null,
                ActionExpectation.MUST_EXIST,
                ActionExpectation.MUST_HAVE_POINT
        );
        return this;
    }

    /** Wait until a locator is FOUND. */
    public Actions waitFor(Locator locator) throws Exception {
        Objects.requireNonNull(locator, "locator is null");

        ActionStep.run(
                s,
                "waitFor:" + lbl(locator),
                locator,
                options,
                stabilizer,
                (ss, r) -> ss.stabilizeInStep("before-waitFor"),
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

    // ---- keyboard-only steps ----

    public Actions hotkey(String chord) throws Exception {
        Objects.requireNonNull(chord, "chord is null");

        ActionStep.run(
                s,
                "hotkey:" + chord,
                NOOP_LOCATOR,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-hotkey");
                    ss.hotkey(chord);
                    ss.stabilizeInStep("after-hotkey");
                },
                null
        );
        return this;
    }

    public Actions press(String key) throws Exception {
        Objects.requireNonNull(key, "key is null");

        ActionStep.run(
                s,
                "press:" + key,
                NOOP_LOCATOR,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-press");
                    ss.press(key);
                    ss.stabilizeInStep("after-press");
                },
                null
        );
        return this;
    }

    public Actions typeText(String text) throws Exception {
        Objects.requireNonNull(text, "text is null");

        ActionStep.run(
                s,
                "typeText",
                NOOP_LOCATOR,
                options,
                stabilizer,
                (ss, r) -> {
                    ss.stabilizeInStep("before-typeText");
                    ss.typeText(text);
                    ss.stabilizeInStep("after-typeText");
                },
                null
        );
        return this;
    }
}
