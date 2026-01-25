package io.deskpilot.engine.locators;

import io.deskpilot.engine.UiTarget;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

public final class PointLocator implements Locator {

    private final String label;
    private final UiTarget target;

    public PointLocator(String label, UiTarget target) {
        this.label = label;
        this.target = target;
    }

    @Override public LocatorKind kind() { return LocatorKind.POINT; }
    @Override public String label() { return label; }

    @Override
    public LocatorResult locate(LocatorSession session) throws Exception {
        // ✅ WIN32 point for clicking
        Point p = session.resolvePointWin32(target);

        Rectangle client = session.getClientRectWin32();
        if (!client.contains(p)) {
            return LocatorResult.notFound(kind(), label, Map.of(
                    "reason", "point_outside_client",
                    "point", String.valueOf(p),
                    "client", String.valueOf(client)
            ));
        }

        return LocatorResult.found(kind(), label, p, null, -1, Map.of(
                "point", String.valueOf(p)
        ));
        
    }

 

}
