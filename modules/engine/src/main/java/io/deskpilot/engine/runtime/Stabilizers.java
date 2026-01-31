package io.deskpilot.engine.runtime;

import io.deskpilot.engine.DeskPilotSession;

public final class Stabilizers {

    private Stabilizers() {}

    public static ActionStep.Stabilizer refresh() {
        return DeskPilotSession::before;
    }

    public static ActionStep.Stabilizer sleep(long ms) {
        return s -> {
            if (ms > 0) Thread.sleep(ms);
        };
    }

    public static ActionStep.Stabilizer refreshThenSleep(long ms) {
        return s -> {
            s.before();
            if (ms > 0) Thread.sleep(ms);
        };
    }

    
}
