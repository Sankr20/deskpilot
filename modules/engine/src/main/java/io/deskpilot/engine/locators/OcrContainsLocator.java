package io.deskpilot.engine.locators;

import io.deskpilot.engine.NormalizedRegion;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class OcrContainsLocator implements Locator {

    private final String label;
    private final NormalizedRegion region;
    private final String expectedContainsNorm;

    public OcrContainsLocator(String label, NormalizedRegion region, String expectedContains) {
        this.label = Objects.requireNonNull(label, "label is null");
        this.region = Objects.requireNonNull(region, "region is null");
        this.expectedContainsNorm = normalize(Objects.requireNonNull(expectedContains, "expectedContains is null"));
    }

    @Override
    public LocatorKind kind() {
        return LocatorKind.OCR;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public LocatorResult locate(LocatorSession session) throws Exception {
        Objects.requireNonNull(session, "session is null");

        Rectangle bounds = region.toScreenRect(session.getClientRectWin32());
        Point center = new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);

        String gotNorm = session.readTextNormalized(region);
        boolean ok = gotNorm != null && gotNorm.contains(expectedContainsNorm);

        Map<String, String> diag = new LinkedHashMap<>();
        diag.put("expectedContains", expectedContainsNorm);
        diag.put("ocr", gotNorm == null ? "" : gotNorm);

        if (ok) {
            return LocatorResult.found(kind(), label, center, bounds, 1.0, diag);
        }
        return LocatorResult.notFound(kind(), label, diag);
    }

    private static String normalize(String s) {
        return s.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
