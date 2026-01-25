package io.deskpilot.engine.locators;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.UiTarget;
import io.deskpilot.engine.targets.TemplateTarget;
import io.deskpilot.engine.NormalizedRegion;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Minimal session surface required for locator resolution.
 * DeskPilotSession implements this; tests can use fakes.
 */
public interface LocatorSession {

   Rectangle getClientRectWin32();

String readTextNormalized(NormalizedRegion region) throws Exception;



    Point resolvePointWin32(UiTarget target) throws Exception;

    /**
     * Locate a template target against the current client screenshot.
     * MUST NOT throw for "not found" (return NOT_FOUND / NEAR_MISS instead).
     * CAN throw for invalid definitions (e.g., missing template resource).
     */
    LocatorResult locateTemplate(TemplateTarget target, String label) throws Exception;

    public static LocatorSession from(DeskPilotSession s) {
    if (s == null) throw new IllegalArgumentException("session is null");

    return new LocatorSession() {
        @Override
        public Rectangle getClientRectWin32() {
            return s.getClientRectWin32();
        }

        @Override
        public Point resolvePointWin32(UiTarget target) throws Exception {
            return s.resolvePointWin32(target);
        }

        @Override
        public LocatorResult locateTemplate(TemplateTarget target, String label) throws Exception {
            return s.locateTemplate(target, label);
        }

        @Override
public String readTextNormalized(NormalizedRegion region) throws Exception {
    return s.readTextNormalized(region);
}

    };
}


}
