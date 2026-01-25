package io.deskpilot.engine.runtime;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.actions.ActionExpectation;
import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.locators.LocatorResult;
import io.deskpilot.engine.locators.LocatorSession;

import java.time.Duration;
import java.time.Instant;

public final class ActionStep {

    @FunctionalInterface
    public interface Stabilizer {
        void stabilize(DeskPilotSession s) throws Exception;
    }

    @FunctionalInterface
    public interface Actor {
        void act(DeskPilotSession s, LocatorResult r) throws Exception;
    }

    @FunctionalInterface
    public interface Verifier {
        void verify(DeskPilotSession s, LocatorResult r) throws Exception;
    }

public static void run(
        DeskPilotSession s,
        String stepName,
        Locator locator,
        ActionOptions options,
        Stabilizer stabilize,
        Actor act,
        Verifier verify,
        ActionExpectation... expectations
) throws Exception {



        if (s == null) throw new IllegalArgumentException("session is null");
        if (stepName == null || stepName.isBlank()) throw new IllegalArgumentException("stepName is blank");
        if (locator == null) throw new IllegalArgumentException("locator is null");

        final ActionOptions opt = (options == null) ? ActionOptions.defaults() : options;

        s.step(stepName, () -> {

            Instant deadline = Instant.now().plus(opt.timeout());
            int attempts = 0;

            LocatorResult last = null;
            Exception lastErr = null;
            String lastExpectationFailure = null;

            while (Instant.now().isBefore(deadline)) {
                attempts++;

                try {
                    // ✅ refresh visual state
                 // ✅ stabilize + capture (minimal default)
if (stabilize != null) stabilize.stabilize(s);
else s.stabilize();



                    // ✅ build a LocatorSession from DeskPilotSession
                    LocatorSession ls = LocatorSession.from(s);

                    // ✅ locate (must return NOT_FOUND rather than throw for not found)
                    LocatorResult r = locator.locate(ls);

                    last = r;
                    lastExpectationFailure = ActionAssertions.check(expectations, r);

                    if (lastExpectationFailure == null) {
                        if (act != null) act.act(s, r);
                        if (verify != null) verify.verify(s, r);
                        return;
                    }

                    sleep(opt.pollInterval());
                } catch (Exception e) {
                    lastErr = e;
                    sleep(opt.pollInterval());
                }
            }

           String msg = buildFailureMessage(stepName, locator, expectations, attempts, last, lastExpectationFailure, lastErr);

           try {
    // always persist the final diagnostic message
    s.saveStepText("failure.txt", msg);
} catch (Exception ignore) {
    // don’t mask the real failure
}

         throw new ActionFailedException(stepName, locator, expectations, attempts, last, msg, lastErr);

        });
    }

public static void run(
    DeskPilotSession s,
    String stepName,
    Locator locator,
    Actor act,
    ActionExpectation... expectations
) throws Exception {
    run(
        s,
        stepName,
        locator,
        ActionOptions.defaults(),
        null,
        act,
        null,
        expectations
    );
}

    private static void sleep(Duration d) {
        if (d == null) return;
        long ms = d.toMillis();
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

 private static String buildFailureMessage(
        String stepName,
        Locator locator,
        ActionExpectation[] exps,
        int attempts,
        LocatorResult last,
        String expFail,
        Exception lastErr
)
 {
        String label = locator == null ? "<null>" : locator.label();

        StringBuilder sb = new StringBuilder();
        sb.append("Action step failed: ").append(stepName).append("\n")
          .append("locator=").append(label).append("\n")
          .append("kind=").append(locator.kind()).append("\n")
         .append("expectations=").append(java.util.Arrays.toString(exps)).append("\n")
          .append("attempts=").append(attempts).append("\n");

        if (expFail != null) sb.append("reason=").append(expFail).append("\n");

        if (last != null && last.diag != null && !last.diag.isEmpty()) {
    sb.append("diag:\n");
    for (var e : last.diag.entrySet()) {
        sb.append("  - ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
    }
}
        if (last != null) sb.append("lastResult=").append(last.toString()).append("\n");
        else sb.append("lastResult=<null>\n");

        if (lastErr != null) {
            sb.append("lastError=").append(lastErr.getClass().getSimpleName())
              .append(": ").append(lastErr.getMessage());
        }

        return sb.toString();
    }
}
