package io.deskpilot.recorder;

import io.deskpilot.engine.DeskPilotSession;

import java.util.List;

public class VerificationInjector {

    public void injectTextVerifications(DeskPilotSession session, List<RecordedAction> actions) {
        // TODO: wire to the correct OCR/text API.
        // For now we do not inject anything to keep recorder compiling and runnable.
    }
}
