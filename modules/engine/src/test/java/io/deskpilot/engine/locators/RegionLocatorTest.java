package io.deskpilot.engine.locators;

import io.deskpilot.engine.NormalizedRegion;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.*;

class RegionLocatorTest {

    static final class FakeSession implements LocatorSession {
        private final Rectangle client;

        FakeSession(Rectangle client) {
            this.client = client;
        }

        @Override public Rectangle getClientRectWin32() { return client; }

        @Override public java.awt.Point resolvePointWin32(io.deskpilot.engine.UiTarget target) {
            throw new UnsupportedOperationException("not needed");
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
    void regionOutsideClient_returnsNotFound() throws Exception {
        // Region in percentages: x=2.0 => far outside
        NormalizedRegion r = new NormalizedRegion(2.0, 2.0, 0.1, 0.1);
        RegionLocator loc = new RegionLocator("status_region", r);

        LocatorResult out = loc.locate(new FakeSession(new Rectangle(0, 0, 100, 100)));
        assertEquals(LocateStatus.NOT_FOUND, out.status);
        assertTrue(out.diag.getOrDefault("reason", "").contains("invalid"), String.valueOf(out.diag));
    }

    @Test
    void regionClampsInsideClient_returnsFound() throws Exception {
        // A region partially outside should be intersected with client and still found if non-empty.
        NormalizedRegion r = new NormalizedRegion(0.9, 0.9, 0.3, 0.3);
        RegionLocator loc = new RegionLocator("status_region", r);

        LocatorResult out = loc.locate(new FakeSession(new Rectangle(0, 0, 100, 100)));
        assertEquals(LocateStatus.FOUND, out.status);
        assertNotNull(out.bounds);
        assertFalse(out.bounds.isEmpty());
    }
}