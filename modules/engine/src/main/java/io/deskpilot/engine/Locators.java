package io.deskpilot.engine;

import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.locators.PointLocator;
import io.deskpilot.engine.locators.RegionLocator;
import io.deskpilot.engine.locators.TemplateLocator;
import io.deskpilot.engine.targets.TemplateTarget;
import io.deskpilot.engine.locators.OcrContainsLocator;
import java.util.Objects;

/**
 * Single registry + factories for DeskPilot locators.
 *
 * NOTE: The runtime locator model lives in {@code io.deskpilot.engine.locators.*}.
 * This class is only a convenient registry (including AUTOGEN constants).
 */
public final class Locators {

    private Locators() {}

    // -------------------------
    // Factory helpers
    // -------------------------


    /** Point (UiTarget) */
    public static Locator point(UiTarget t) {
        Objects.requireNonNull(t, "UiTarget is null");
        return new PointLocator(t.name(), t);
    }

    /** Template (TemplateTarget) */
    public static Locator template(TemplateTarget t) {
        Objects.requireNonNull(t, "TemplateTarget is null");
        return new TemplateLocator(t.name, t);
    }

    /** Region (NormalizedRegion) */
    public static Locator region(String label, NormalizedRegion r) {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("label blank");
        Objects.requireNonNull(r, "NormalizedRegion is null");
        return new RegionLocator(label, r);
    }

    // Sugar
    public static Locator point(String name, double xPct, double yPct) {
        return point(new UiTarget(name, xPct, yPct));
    }
public static Locator ocrContains(String label, NormalizedRegion region, String expectedContains) {
    if (label == null || label.isBlank()) throw new IllegalArgumentException("label blank");
    Objects.requireNonNull(region, "region is null");
    Objects.requireNonNull(expectedContains, "expectedContains is null");
    return new OcrContainsLocator(label, region, expectedContains);
}

// --- DESKPILOT:AUTOGEN:START
/** btn_point */
public static final Locator BTNPOINT =
        point(UiMap.BTNPOINT);
    /** btn_region */
    public static final Locator BTNREGION =
            region("btn_region", UiRegions.BTNREGION);
    /** btn_template */
    public static final Locator BTNTEMPLATE =
            template(UiTemplates.BTNTEMPLATE);
    /** btn_search */
    public static final Locator BTN_SEARCH =
            point(UiMap.BTN_SEARCH);
    /** button_search */
    public static final Locator BUTTON_SEARCH =
            point(UiMap.BUTTON_SEARCH);
    /** inputpoint */
    public static final Locator INPUTPOINT =
            point(UiMap.INPUTPOINT);
    /** point_button_search */
    public static final Locator POINT_BUTTON_SEARCH =
            point(UiMap.POINT_BUTTON_SEARCH);
    /** point_input_search */
    public static final Locator POINT_INPUT_SEARCH =
            point(UiMap.POINT_INPUT_SEARCH);
    /** region_status_update */
    public static final Locator REGION_STATUS_UPDATE =
            region("region_status_update", UiRegions.REGION_STATUS_UPDATE);
    /** searchbutton */
    public static final Locator SEARCHBUTTON =
            region("searchbutton", UiRegions.SEARCHBUTTON);
    /** searchinput */
    public static final Locator SEARCHINPUT =
            point(UiMap.SEARCHINPUT);
    /** searchstatusupdateregion */
    public static final Locator SEARCHSTATUSUPDATEREGION =
            region("searchstatusupdateregion", UiRegions.SEARCHSTATUSUPDATEREGION);
    /** search_btn */
    public static final Locator SEARCH_BTN =
            point(UiMap.SEARCH_BTN);
    /** search_btn_point */
    public static final Locator SEARCH_BTN_POINT =
            point(UiMap.SEARCH_BTN_POINT);

    /** status_text_searched */
    public static final Locator STATUS_TEXT_SEARCHED =
            ocrContains("status_text_searched", UiRegions.SEARCHSTATUSUPDATEREGION, "searched");
    // --- DESKPILOT:AUTOGEN:END



}
