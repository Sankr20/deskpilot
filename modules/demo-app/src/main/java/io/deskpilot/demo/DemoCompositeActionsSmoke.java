package io.deskpilot.demo;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.Locators;
import io.deskpilot.engine.actions.Actions;

public final class DemoCompositeActionsSmoke {

    public static void main(String[] args) throws Exception {

        System.err.println("🔥 SMOKE STARTED");
        System.err.println("👉 When prompted, CLICK the DeskPilot Demo App window (NOT the terminal).");

        DeskPilotSession s = DeskPilotSession.attachPickWindow("composite-actions-smoke");
Actions a = new Actions(s);

s.step("demo-search-flow", () -> {
    DemoFlows.search(a, "DeskPilot");
});


        s.step("demo-search-flow", () -> {
            a.click(Locators.SEARCHINPUT)
             .type(Locators.SEARCHINPUT, "DeskPilot")
             .click(Locators.SEARCH_BTN);
        });

        System.err.println("✅ SMOKE DONE");
    }

    private DemoCompositeActionsSmoke() {}
}
