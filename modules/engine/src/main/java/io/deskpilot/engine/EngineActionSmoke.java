package io.deskpilot.engine;

import io.deskpilot.engine.actions.Actions;
import io.deskpilot.engine.runtime.Stabilizers;

public final class EngineActionSmoke {

    public static void main(String[] args) throws Exception {

        System.out.println("=== EngineActionSmoke starting ===");

        try (DeskPilotSession s = DeskPilotSession.attachPickWindow("action-smoke")) {

            Actions a = new Actions(s)
                    .withStabilizer(Stabilizers.refreshThenSleep(150));

            a.click(Locators.POINT_INPUT_SEARCH)
             .paste(Locators.POINT_INPUT_SEARCH, "deskpilot")
             .click(Locators.SEARCH_BTN_POINT);

            // ✅ no s.after() here
            // ✅ no teardown step here (close() handles it)
        }

        System.out.println("=== EngineActionSmoke finished OK ===");
    }

    private EngineActionSmoke() {}
}
