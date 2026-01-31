package io.deskpilot.recorder;

import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.NormalizedRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecorderManager {

    private final List<RecordedAction> actions = new ArrayList<>();
    @SuppressWarnings("unused")
    private final DeskPilotSession session;

    private int clickCount = 0;
    private int fillCount = 0;
    private int waitCount = 0;

    public RecorderManager(DeskPilotSession session) {
        this.session = Objects.requireNonNull(session, "session is null");
    }

    public void recordClick(NormalizedRegion region) {
        Objects.requireNonNull(region, "region is null");
        actions.add(new RecordedAction.Click(region));
        clickCount++;
    }

    public void recordFill(NormalizedRegion region, String text) {
        Objects.requireNonNull(region, "region is null");
        Objects.requireNonNull(text, "text is null");
        actions.add(new RecordedAction.Fill(region, text));
        fillCount++;
    }

    public void recordWaitText(NormalizedRegion region, String expectedContains) {
        Objects.requireNonNull(region, "region is null");
        Objects.requireNonNull(expectedContains, "expectedContains is null");
        actions.add(new RecordedAction.WaitText(region, expectedContains));
        waitCount++;
    }

    public List<RecordedAction> getActions() {
        return List.copyOf(actions);
    }

    public String summary() {
        return "actions=" + actions.size() +
                " (fill=" + fillCount + ", click=" + clickCount + ", wait=" + waitCount + ")";
    }

}
