package io.deskpilot.engine;

import io.deskpilot.engine.actions.ActionExpectation;
import io.deskpilot.engine.locators.LocatorKind;
import io.deskpilot.engine.locators.LocatorResult;
import io.deskpilot.engine.runtime.ActionAssertions;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ActionAssertionsTest {

    @Test
    void mustExist_passes_whenFound() {
        LocatorResult r = LocatorResult.found(LocatorKind.OCR, "x", null, null, 1.0, Map.of());
        String fail = ActionAssertions.check(new ActionExpectation[]{ActionExpectation.MUST_EXIST}, r);
        assertNull(fail);
    }

    @Test
    void mustExist_fails_whenNotFound() {
        LocatorResult r = LocatorResult.notFound(LocatorKind.OCR, "x", Map.of());
        String fail = ActionAssertions.check(new ActionExpectation[]{ActionExpectation.MUST_EXIST}, r);
        assertNotNull(fail);
        assertTrue(fail.contains("MUST_EXIST"));
    }

    @Test
    void mustHavePoint_passes_whenPointPresent() {
        LocatorResult r = LocatorResult.found(LocatorKind.POINT, "x", new Point(10, 10), null, 1.0, Map.of());
        String fail = ActionAssertions.check(new ActionExpectation[]{ActionExpectation.MUST_HAVE_POINT}, r);
        assertNull(fail);
    }

    @Test
    void mustHavePoint_fails_whenPointMissing() {
        LocatorResult r = LocatorResult.found(LocatorKind.POINT, "x", null, null, 1.0, Map.of());
        String fail = ActionAssertions.check(new ActionExpectation[]{ActionExpectation.MUST_HAVE_POINT}, r);
        assertNotNull(fail);
        assertTrue(fail.contains("MUST_HAVE_POINT"));
    }

    @Test
    void expectations_areCheckedInOrder_firstFailureReturned() {
        LocatorResult r = LocatorResult.notFound(LocatorKind.OCR, "x", Map.of());
        String fail = ActionAssertions.check(
                new ActionExpectation[]{ActionExpectation.MUST_EXIST, ActionExpectation.MUST_HAVE_POINT},
                r
        );
        assertNotNull(fail);
        assertTrue(fail.contains("MUST_EXIST"));
    }
}
