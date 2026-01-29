package io.deskpilot.recorder;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.NormalizedRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecorderManager {

    private final List<RecordedAction> actions = new ArrayList<>();
    private final DeskPilotSession session;

    public RecorderManager(DeskPilotSession session) {
        this.session = Objects.requireNonNull(session, "session is null");
    }

    public void recordClick(NormalizedRegion region) {
        Objects.requireNonNull(region, "region is null");
        actions.add(new RecordedAction.Click(region));
        // (verification injection comes later, after we have a stable strategy)
    }

    public void recordFill(NormalizedRegion region, String text) {
        Objects.requireNonNull(region, "region is null");
        Objects.requireNonNull(text, "text is null");
        actions.add(new RecordedAction.Fill(region, text));
    }

    public List<RecordedAction> getActions() {
        return List.copyOf(actions);
    }
}
