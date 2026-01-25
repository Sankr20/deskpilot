package io.deskpilot.engine;


import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UiFileWriterNormalizationTest {

    @Test
    void normalizeUiMap_normalizesLabelButNotConstName() {
        List<String> in = List.of(
                "package io.deskpilot.engine;",
                "public final class UiMap {",
                "    " + "// --- DESKPILOT:AUTOGEN:START",
                "",
                "    /** Search Button Point */",
                "    public static final UiTarget SEARCH_BUTTON_POINT =",
                "            new UiTarget(\"Search Button Point\", 0.500000, 0.100000);",
                "    " + "// --- DESKPILOT:AUTOGEN:END",
                "}"
        );

        List<String> out = UiFileWriter.normalizeAutogenFile("UiMap.java", in);
        String joined = String.join("\n", out);

        assertTrue(joined.contains("public static final UiTarget SEARCH_BUTTON_POINT"));
        assertTrue(joined.contains("new UiTarget(\"search_button_point\""), joined);
        assertTrue(joined.contains("/** search_button_point */"), joined);
    }

    @Test
    void normalizeLocators_normalizesRegionLabelArgument() {
        List<String> in = List.of(
                "package io.deskpilot.engine;",
                "public final class Locators {",
                "    " + "// --- DESKPILOT:AUTOGEN:START",
                "",
                "    /** Status Region */",
                "    public static final Locator STATUS_REGION =",
                "            region(\"Status Region\", UiRegions.STATUS_REGION);",
                "    " + "// --- DESKPILOT:AUTOGEN:END",
                "}"
        );

        List<String> out = UiFileWriter.normalizeAutogenFile("Locators.java", in);
        String joined = String.join("\n", out);

        assertTrue(joined.contains("region(\"status_region\""), joined);
        assertTrue(joined.contains("/** status_region */"), joined);
    }
}
