package io.deskpilot.engine.locators;

import io.deskpilot.engine.NormalizedRegion;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * OCR polling locator:
 * - ONE OCR read per locate() call
 * - FOUND if normalized OCR text contains expected substring (case-insensitive)
 * - ActionStep handles retries/timeouts (the "wait")
 */
public final class OcrContainsLocator implements Locator {

    private final String label;
    private final NormalizedRegion region;
    private final String expectedNorm;

    public OcrContainsLocator(String label, NormalizedRegion region, String expected) {
        this.label = Objects.requireNonNull(label, "label is null");
        this.region = Objects.requireNonNull(region, "region is null");

        Objects.requireNonNull(expected, "expected is null");
        String t = expected.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("expected is blank");

        this.expectedNorm = normalize(t);
    }

    @Override public String label() { return label; }

    @Override public LocatorKind kind() { return LocatorKind.OCR; }

    @Override
    public LocatorResult locate(LocatorSession s) throws Exception {
        String actual = s.readTextNormalized(region); // already trims/cleans, but we normalize again for punctuation.
        String actualNorm = normalize(actual == null ? "" : actual);

        boolean ok = !actualNorm.isEmpty() && actualNorm.contains(expectedNorm);

        Map<String, String> diag = new LinkedHashMap<>();
        diag.put("expectedContains", expectedNorm);
        diag.put("actual", actual == null ? "" : actual);
        diag.put("actualNorm", actualNorm);
        diag.put("region", String.valueOf(region));

        if (ok) {
            return LocatorResult.found(kind(), label, null, null, -1, diag);
        }
        return LocatorResult.notFound(kind(), label, diag);
    }

    /**
     * Deterministic normalization for OCR comparisons:
     * - lowercase
     * - punctuation -> space
     * - collapse whitespace
     */
    private static String normalize(String s) {
        if (s == null) return "";
        String x = s.toLowerCase(Locale.ROOT);

        // replace punctuation-ish with spaces (keep letters/digits)
        x = x.replaceAll("[^a-z0-9]+", " ");

        // collapse whitespace
        x = x.replaceAll("\\s+", " ").trim();

        return x;
    }
}
