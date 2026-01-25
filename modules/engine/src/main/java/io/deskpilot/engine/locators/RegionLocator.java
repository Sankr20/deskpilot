package io.deskpilot.engine.locators;

import io.deskpilot.engine.NormalizedRegion;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Map;

public final class RegionLocator implements Locator {

    private final String label;
    private final NormalizedRegion region;

    public NormalizedRegion region() { return region; }

    public RegionLocator(String label, NormalizedRegion region) {
        this.label = label;
        this.region = region;
    }

    @Override public LocatorKind kind() { return LocatorKind.REGION; }
    @Override public String label() { return label; }

    @Override
    public LocatorResult locate(LocatorSession session) throws Exception {

        Rectangle client = session.getClientRectWin32();

        // ✅ Hardening: reject invalid normalized values (don't silently clamp garbage).
        if (region.xPct < 0 || region.xPct > 1 || region.yPct < 0 || region.yPct > 1
                || region.wPct <= 0 || region.wPct > 1 || region.hPct <= 0 || region.hPct > 1) {
            return LocatorResult.notFound(kind(), label, Map.of(
                    "reason", "invalid_normalized_region",
                    "xPct", String.valueOf(region.xPct),
                    "yPct", String.valueOf(region.yPct),
                    "wPct", String.valueOf(region.wPct),
                    "hPct", String.valueOf(region.hPct),
                    "client", String.valueOf(client)
            ));
        }


        // ✅ Compute region in WIN32 space
        Rectangle r = region.toScreenRect(client);

        Rectangle clipped = r.intersection(client);
        if (clipped.isEmpty()) {
            return LocatorResult.notFound(kind(), label, Map.of(
                    "reason", "region_outside_client",
                    "region", String.valueOf(r),
                    "client", String.valueOf(client)
            ));
        }

        // ✅ Useful default point: center of region (WIN32)
        Point center = new Point(
                clipped.x + clipped.width / 2,
                clipped.y + clipped.height / 2
        );

        return LocatorResult.found(kind(), label, center, clipped, -1, Map.of(
                "region", String.valueOf(r),
                "clipped", String.valueOf(clipped)
        ));
    }



}