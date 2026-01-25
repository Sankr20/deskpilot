package io.deskpilot.engine.locators;

import io.deskpilot.engine.targets.TemplateTarget;


public final class TemplateLocator implements Locator {

    private final String label;
    private final TemplateTarget templateTarget;

    public TemplateTarget templateTarget() { return templateTarget; }

    public TemplateLocator(String label, TemplateTarget templateTarget) {
        this.label = label;
        this.templateTarget = templateTarget;
    }

    @Override public LocatorKind kind() { return LocatorKind.TEMPLATE; }
    @Override public String label() { return label; }

    @Override
    public LocatorResult locate(LocatorSession session) throws Exception {
        return session.locateTemplate(templateTarget, label);
    }
}
