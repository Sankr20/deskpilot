package io.deskpilot.engine.locators;

import io.deskpilot.engine.NormalizedRegion;

import java.util.Locale;
import java.util.Objects;

/**
 * OCR polling locator:
 * - ONE OCR read per locate() call
 * - returns FOUND if normalized OCR text contains expected substring (case-insensitive)
 * - ActionStep handles retries/timeouts (the "wait")
 */
public final class OcrContainsLocator implements Locator {

    private final String label;
    private final NormalizedRegion region;
    private final String expectedLower;

    public OcrContainsLocator(String label, NormalizedRegion region, String expected) {
        this.label = Objects.requireNonNull(label, "label is null");
        this.region = Objects.requireNonNull(region, "region is null");

        Objects.requireNonNull(expected, "expected is null");
        String t = expected.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("expected is blank");

        this.expectedLower = t.toLowerCase(Locale.ROOT);
    }

    @Override public String label() { return label; }

    @Override public LocatorKind kind() { return LocatorKind.OCR; }

@Override
public LocatorResult locate(LocatorSession s) throws Exception {
    String actual = s.readTextNormalized(region);
    String actualLower = (actual == null ? "" : actual.toLowerCase(Locale.ROOT));

    boolean ok = !actualLower.isEmpty() && actualLower.contains(expectedLower);

    java.util.Map<String,String> diag = new java.util.LinkedHashMap<>();
    diag.put("expectedContains", expectedLower);
    diag.put("actual", actual == null ? "" : actual);
    diag.put("region", String.valueOf(region));

    if (ok) {
        return LocatorResult.found(kind(), label, null, null, -1, diag);
    }
    return LocatorResult.notFound(kind(), label, diag);
}

}
