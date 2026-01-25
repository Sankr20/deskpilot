package io.deskpilot.engine;

import io.deskpilot.engine.targets.TemplateTarget;

/**
 * AUTO-GENERATED TEMPLATE TARGETS (ICON MATCHING)
 * Do not edit between AUTOGEN markers by hand.
 */
public final class UiTemplates {

    private UiTemplates() {}

    // --- DESKPILOT:AUTOGEN:START
    /** btn_template */
    public static final TemplateTarget BTNTEMPLATE =
            TemplateTarget.of("btn_template", "icons/btntemplate.png")
                    .withSearchAreaPct(new io.deskpilot.engine.targets.SearchAreaPct(0.025000, 0.044302, 0.118750, 0.109070))
                    .withMinScore(0.85);

    /** save_icon */
    public static final TemplateTarget SAVE_ICON =
            TemplateTarget.topLeftPct("save_icon", "icons/save_icon.png", 0.38, 0.29)
                    .withMinScore(0.85);

    /** search_result_area */
    public static final TemplateTarget SEARCHRESULTAREA =
            TemplateTarget.of("search_result_area", "icons/searchresultarea.png")
                    .withMinScore(0.85);

    /** search_status_region */
    public static final TemplateTarget SEARCHSTATUSREGION =
            TemplateTarget.of("search_status_region", "icons/searchstatusregion.png")
                    .withSearchAreaPct(new io.deskpilot.engine.targets.SearchAreaPct(0.000000, 0.010545, 0.103125, 0.098831))
                    .withMinScore(0.85);

    /** search_update_template */
    public static final TemplateTarget SEARCHUPDATETEMPLATE =
            TemplateTarget.of("search_update_template", "icons/searchupdatetemplate.png")
                    .withSearchAreaPct(new io.deskpilot.engine.targets.SearchAreaPct(0.000000, 0.010545, 0.102083, 0.099822))
                    .withMinScore(0.85);
    // --- DESKPILOT:AUTOGEN:END
}
