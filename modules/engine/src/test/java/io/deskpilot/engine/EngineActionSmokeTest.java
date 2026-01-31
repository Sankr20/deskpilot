package io.deskpilot.engine;

import io.deskpilot.engine.actions.Actions;
import io.deskpilot.engine.runtime.Stabilizers;
import org.junit.jupiter.api.Test;

class EngineActionSmokeTest {

    @Test
    void smoke_search_flow() throws Exception {

        try (DeskPilotSession s = DeskPilotSession.attachPickWindow("action-smoke")) {

            s.step("startup", s::before);

            Actions a = new Actions(s)
                    .withStabilizer(Stabilizers.refreshThenSleep(150));

            // ✅ Use your existing locators
            a.click(Locators.POINT_INPUT_SEARCH)                 // focus input
             .paste(Locators.POINT_INPUT_SEARCH, "deskpilot")    // enter text
             .click(Locators.SEARCH_BTN_POINT);                 // click search

            // 🚧 Parked for now (OCR region is tiny and still being tuned)
            // a.waitFor(Locators.STATUS_TEXT_SEARCHED);

            s.step("teardown", s::after);
        }
    }
}
