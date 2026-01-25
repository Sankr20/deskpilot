package io.deskpilot.engine.locators;

public interface Locator {
    LocatorKind kind();
    String label();

    /**
     * Resolve locator into a runtime result.
     * MUST NOT throw for "not found" (return NOT_FOUND instead).
     * CAN throw for invalid definitions (e.g., missing template resource).
     */
    LocatorResult locate(LocatorSession session) throws Exception;
}
