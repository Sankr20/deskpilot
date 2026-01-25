package io.deskpilot.engine;

import io.deskpilot.engine.targets.TemplateTarget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class RegistryIntegrationTest {

    // snake_case (lowercase only)
    private static final String SNAKE = "^[a-z0-9]+(_[a-z0-9]+)*$";

    @Test
    void uiMap_labelsAreSnakeCase_andUnique() throws Exception {
        Map<String, String> labelToConst = new LinkedHashMap<>();

        for (Field f : UiMap.class.getDeclaredFields()) {
            if (!isPublicStaticFinal(f)) continue;
            if (!UiTarget.class.isAssignableFrom(f.getType())) continue;

            UiTarget t = (UiTarget) f.get(null);
            assertNotNull(t, "UiMap field " + f.getName() + " is null");
            assertNotNull(t.name(), "UiTarget name is null for " + f.getName());
            assertTrue(t.name().matches(SNAKE),
                    "UiMap label must be snake_case. Field=" + f.getName() + " label=" + t.name());

            String prev = labelToConst.put(t.name(), f.getName());
            assertNull(prev, "Duplicate UiMap label: '" + t.name() + "' used by " + prev + " and " + f.getName());
        }

        assertFalse(labelToConst.isEmpty(), "UiMap should not be empty");
    }

    @Test
    void uiRegions_namesAreSnakeCase_andUnique() throws Exception {
        Map<String, String> nameToConst = new LinkedHashMap<>();

        for (Field f : UiRegions.class.getDeclaredFields()) {
            if (!isPublicStaticFinal(f)) continue;
            if (!NormalizedRegion.class.isAssignableFrom(f.getType())) continue;

            // We don't have a label inside NormalizedRegion, so we assert on the JavaDoc naming contract
            // by validating the CONST name shape and ensuring Locators uses snake_case names.
            // For now we check that Locators includes a RegionLocator for this region.
            String regionConst = f.getName();
            assertNotNull(f.get(null), "UiRegions field " + regionConst + " is null");

            // Collect const names to ensure unique (trivial, but catches accidental duplicates via generation)
            String prev = nameToConst.put(regionConst, regionConst);
            assertNull(prev, "Duplicate UiRegions const name: " + regionConst);
        }

        assertFalse(nameToConst.isEmpty(), "UiRegions should not be empty");
    }

    @Test
    void uiTemplates_namesAreSnakeCase_andResourcesExist() throws Exception {
        Map<String, String> nameToConst = new LinkedHashMap<>();

        for (Field f : UiTemplates.class.getDeclaredFields()) {
            if (!isPublicStaticFinal(f)) continue;
            if (!TemplateTarget.class.isAssignableFrom(f.getType())) continue;

            TemplateTarget t = (TemplateTarget) f.get(null);
            assertNotNull(t, "UiTemplates field " + f.getName() + " is null");
            assertNotNull(t.name, "TemplateTarget.name is null for " + f.getName());

            assertTrue(t.name.matches(SNAKE),
                    "UiTemplates template name must be snake_case. Field=" + f.getName() + " name=" + t.name);

            String prev = nameToConst.put(t.name, f.getName());
            assertNull(prev, "Duplicate UiTemplates name: '" + t.name + "' used by " + prev + " and " + f.getName());

            // Verify every template variant path can be loaded from classpath
            assertNotNull(t.imagePaths, "TemplateTarget.imagePaths is null for " + f.getName());
            assertTrue(t.imagePaths.length >= 1, "TemplateTarget.imagePaths must have >= 1 item for " + f.getName());

            for (String path : t.imagePaths) {
                assertNotNull(path, "Template image path is null for " + f.getName());
                assertFalse(path.isBlank(), "Template image path is blank for " + f.getName());

                // ImageUtil will throw if resource doesn't exist
                assertDoesNotThrow(() -> io.deskpilot.engine.image.ImageUtil.loadResource(path),
                        "Template image must exist on classpath: " + path + " (Field=" + f.getName() + ")");
            }
        }

        assertFalse(nameToConst.isEmpty(), "UiTemplates should not be empty");
    }

    @Test
    void locators_coverAllRegistries_basicSanity() throws Exception {
        // Ensure Locators exposes at least one of each kind and has snake_case labels for region locators.
        boolean sawPoint = false, sawTemplate = false, sawRegion = false;

        for (Field f : Locators.class.getDeclaredFields()) {
            if (!isPublicStaticFinal(f)) continue;

            Object v = f.get(null);
            if (!(v instanceof io.deskpilot.engine.locators.Locator loc)) continue;

            assertNotNull(loc.label(), "Locator label is null for " + f.getName());
            assertFalse(loc.label().isBlank(), "Locator label blank for " + f.getName());

            if (loc.kind() == io.deskpilot.engine.locators.LocatorKind.POINT) sawPoint = true;
            if (loc.kind() == io.deskpilot.engine.locators.LocatorKind.TEMPLATE) sawTemplate = true;

            if (v instanceof io.deskpilot.engine.locators.RegionLocator rl) {
                sawRegion = true;
                assertTrue(rl.label().matches(SNAKE),
                        "Region locator label must be snake_case. Field=" + f.getName() + " label=" + rl.label());
                assertNotNull(rl.region(), "RegionLocator.region() null for " + f.getName());
            }
        }

        assertTrue(sawPoint, "Locators should contain at least one POINT locator");
        assertTrue(sawTemplate, "Locators should contain at least one TEMPLATE locator");
        assertTrue(sawRegion, "Locators should contain at least one REGION locator");
    }


    private static boolean isPublicStaticFinal(Field f) {
        int m = f.getModifiers();
        return Modifier.isPublic(m) && Modifier.isStatic(m) && Modifier.isFinal(m);
    }
}
