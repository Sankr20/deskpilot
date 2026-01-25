package io.deskpilot.engine.locators;

import io.deskpilot.engine.UiTarget;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

class PointLocatorTest {

    static final class FakeSession implements LocatorSession {
        private final Rectangle client;
        private final Point p;

        FakeSession(Rectangle client, Point p) {
            this.client = client;
            this.p = p;
        }

        @Override public Rectangle getClientRectWin32() { return client; }

        @Override public Point resolvePointWin32(UiTarget target) {
            return p;
        }

        @Override
        public LocatorResult locateTemplate(io.deskpilot.engine.targets.TemplateTarget target, String label) {
            throw new UnsupportedOperationException("not needed");
        }

           @Override
public String readTextNormalized(io.deskpilot.engine.NormalizedRegion region) {
    return ""; // not needed for point locator tests
}
    }

    @Test
    void pointOutsideClient_returnsNotFound() throws Exception {
        UiTarget t = new UiTarget("p", 0.5, 0.5);
        PointLocator loc = new PointLocator("p", t);

        FakeSession s = new FakeSession(new Rectangle(100, 100, 50, 50), new Point(10, 10));
        LocatorResult r = loc.locate(s);

        assertEquals(LocateStatus.NOT_FOUND, r.status);
        assertTrue(r.diag.getOrDefault("reason", "").contains("outside"), String.valueOf(r.diag));
    }

    @Test
    void pointInsideClient_returnsFound() throws Exception {
        UiTarget t = new UiTarget("p", 0.5, 0.5);
        PointLocator loc = new PointLocator("p", t);

        FakeSession s = new FakeSession(new Rectangle(0, 0, 200, 200), new Point(10, 10));
        LocatorResult r = loc.locate(s);

        assertEquals(LocateStatus.FOUND, r.status);
        assertEquals(new Point(10, 10), r.point);
    }
    
}
