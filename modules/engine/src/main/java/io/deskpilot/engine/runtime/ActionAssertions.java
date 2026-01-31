package io.deskpilot.engine.runtime;

import io.deskpilot.engine.actions.ActionExpectation;
import io.deskpilot.engine.locators.LocatorResult;

public final class ActionAssertions {

    private ActionAssertions() {}

    public static String check(ActionExpectation[] expectations, LocatorResult r) {
    if (expectations == null || expectations.length == 0) return null;

    for (ActionExpectation e : expectations) {
        String fail = checkOne(e, r);
        if (fail != null) return fail;
    }
    return null;
}

private static String checkOne(ActionExpectation e, LocatorResult r) {
    switch (e) {
        case MUST_EXIST -> {
            if (r == null || !r.exists()) return "Expectation MUST_EXIST failed (status=" + (r==null ? "null" : r.status) + ")";
        }
        case MUST_HAVE_POINT -> {
            if (r == null || r.point == null) return "Expectation MUST_HAVE_POINT failed (point=null)";
        }
        case MUST_HAVE_BOUNDS -> {
            if (r == null || r.bounds == null) return "Expectation MUST_HAVE_BOUNDS failed (bounds=null)";
        }
    }
    return null;
}

}
