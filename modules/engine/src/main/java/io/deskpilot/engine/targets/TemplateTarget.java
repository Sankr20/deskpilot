package io.deskpilot.engine.targets;

import java.awt.Rectangle;
import java.util.Arrays;

public class TemplateTarget {

    public final String name;

    /** Primary (first) image path for backward compatibility. */
    public final String imagePath;

    /** All template image paths (variants). Never null, length >= 1. */
    public final String[] imagePaths;

    public final double minScore;

    /** Client-local pixel search area (optional). */
    public final Rectangle searchArea;

    /** Percent-based search area (optional). */
    public final SearchAreaPct searchAreaPct;

    // Full constructor (new)
    public TemplateTarget(
            String name,
            String[] imagePaths,
            double minScore,
            Rectangle searchArea,
            SearchAreaPct searchAreaPct) {

        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is null/empty");
        if (imagePaths == null || imagePaths.length == 0) throw new IllegalArgumentException("imagePaths is null/empty");

        for (String p : imagePaths) {
            if (p == null || p.isBlank()) throw new IllegalArgumentException("imagePath is null/empty");
        }

        if (minScore <= 0 || minScore > 1.0) throw new IllegalArgumentException("minScore must be in (0,1]");

        this.name = name;
        this.imagePaths = Arrays.copyOf(imagePaths, imagePaths.length);
        this.imagePath = this.imagePaths[0]; // keep old field semantics
        this.minScore = minScore;
        this.searchArea = searchArea;
        this.searchAreaPct = searchAreaPct;

        if (searchArea != null && searchAreaPct != null) {
            throw new IllegalArgumentException("Provide either searchArea OR searchAreaPct, not both");
        }
    }

    // -------------------------
    // Backward-compatible constructors
    // -------------------------

    public TemplateTarget(String name, String imagePath, double minScore, Rectangle searchArea, SearchAreaPct searchAreaPct) {
        this(name, new String[] { imagePath }, minScore, searchArea, searchAreaPct);
    }

    public TemplateTarget(String name, String imagePath, double minScore, Rectangle searchArea) {
        this(name, new String[] { imagePath }, minScore, searchArea, null);
    }

    // -------------------------
    // Factory API
    // -------------------------

    public static TemplateTarget of(String name, String imagePath) {
        return new TemplateTarget(name, new String[] { imagePath }, 0.85, null, null);
    }

    /** New: multiple variants (hover/dark/disabled etc). */
    public static TemplateTarget of(String name, String... imagePaths) {
        return new TemplateTarget(name, imagePaths, 0.85, null, null);
    }

    // -------------------------
    // Ergonomic pixel-area presets
    // -------------------------

    public static TemplateTarget toolbarLeftPx(String name, String imagePath) {
        return new TemplateTarget(name, new String[] { imagePath }, 0.85, new Rectangle(0, 0, 520, 180), null);
    }

    public static TemplateTarget topBarPx(String name, String imagePath) {
        return new TemplateTarget(name, new String[] { imagePath }, 0.85, new Rectangle(0, 0, 2000, 220), null);
    }

    // -------------------------
    // Ergonomic percent-area presets
    // -------------------------

    public static TemplateTarget topLeftPct(String name, String imagePath, double wPct, double hPct) {
        return new TemplateTarget(name, new String[] { imagePath }, 0.85, null, SearchAreaPct.topLeft(wPct, hPct));
    }

    public static TemplateTarget topBarPct(String name, String imagePath, double hPct) {
        return new TemplateTarget(name, new String[] { imagePath }, 0.85, null, SearchAreaPct.topBar(hPct));
    }

    public static TemplateTarget leftNavPct(String name, String imagePath, double wPct) {
        return new TemplateTarget(name, new String[] { imagePath }, 0.85, null, SearchAreaPct.leftNav(wPct));
    }

    // -------------------------
    // Builder-ish helpers (preserve variants)
    // -------------------------

    public TemplateTarget withMinScore(double newMinScore) {
        return new TemplateTarget(this.name, this.imagePaths, newMinScore, this.searchArea, this.searchAreaPct);
    }

    public TemplateTarget withSearchArea(Rectangle area) {
        return new TemplateTarget(this.name, this.imagePaths, this.minScore, area, null);
    }

    public TemplateTarget withSearchAreaPct(SearchAreaPct areaPct) {
        return new TemplateTarget(this.name, this.imagePaths, this.minScore, null, areaPct);
    }
}
