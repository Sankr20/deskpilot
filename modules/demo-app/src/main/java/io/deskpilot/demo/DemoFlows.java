package io.deskpilot.demo;

import io.deskpilot.engine.Locators;
import io.deskpilot.engine.actions.Actions;

public final class DemoFlows {

    public static void search(Actions a, String text) throws Exception {
        a.click(Locators.SEARCHINPUT)
         .paste(Locators.SEARCHINPUT, text)
         .click(Locators.SEARCH_BTN)
         .waitFor(Locators.STATUS_TEXT_SEARCHED);
    }

    private DemoFlows() {}
}
