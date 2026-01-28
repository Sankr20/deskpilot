package io.deskpilot.engine;

/**
 * PRD: single blessed public entrypoint.
 *
 * Tests and users should call DeskPilot.attachPickWindow(RunOptions) rather than using internal engine classes.
 */
public final class DeskPilot {

    private DeskPilot() {}

    public static DeskPilotSession attachPickWindow(RunOptions options) throws Exception {
        return DeskPilotSession.attachPickWindow(options);
    }
}
