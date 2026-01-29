package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;

public class LocatorSuggester {

    public String suggestNameForClick(NormalizedRegion region) {
        // Engine NormalizedRegion does not expose nearby-text helpers.
        // Keep suggestion deterministic for now.
        return normalize("click");
    }

    public String suggestNameForFill(NormalizedRegion region, String text) {
        // Prefer using provided text hint, not OCR.
        String base = "fill";
        if (text != null && !text.isBlank()) {
            base = "fill_" + text.toLowerCase().replaceAll("\\W+", "_");
        }
        return normalize(base);
    }

    private String normalize(String name) {
        if (name == null || name.isBlank()) name = "step";
        if (!name.matches("^[a-z].*")) {
            name = "step_" + name;
        }
        return name;
    }
}
