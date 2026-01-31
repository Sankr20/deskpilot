package io.deskpilot.engine;

import java.util.Locale;

public final class UiNaming {

    private UiNaming() {}

    /** What kind of UI artifact is being recorded */
    public enum Kind {
        POINT,
        REGION,
        TEMPLATE
    }

    /**
     * Normalize a raw name entered by the user into a stable label.
     *
     * Examples:
     *   "save icon"     -> "save_icon"
     *   "Save-Icon"     -> "save_icon"
     *   "SAVE ICON"    -> "save_icon"
     */
    public static String normalizeLabel(String raw, Kind kind) {
        if (raw == null) return "";

        String s = raw.trim()
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        if (s.isEmpty()) {
            throw new IllegalArgumentException("Invalid name after normalization: '" + raw + "'");
        }

        return s;
    }

    /**
     * Convert a normalized label into a Java constant name.
     *
     * Examples:
     *   "save_icon"     -> "SAVE_ICON"
     *   "client_name"  -> "CLIENT_NAME"
     */
    public static String toConst(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Label is empty");
        }
        return label.toUpperCase(Locale.US);
    }
}
