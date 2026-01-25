package io.deskpilot.engine.actions;

import io.deskpilot.engine.locators.Locator;

final class StepNamer {
    private StepNamer() {}

    static String click(Locator l) { return "click-" + l.label(); }
    static String type(Locator l) { return "type-" + l.label(); }
    static String paste(Locator l) { return "paste-" + l.label(); }
    static String fill(Locator l) { return "fill-" + l.label(); }
    static String waitText(Locator l) { return "wait_text-" + l.label(); }
}






