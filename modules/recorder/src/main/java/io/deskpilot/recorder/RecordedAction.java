package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;

import java.util.Objects;

public sealed interface RecordedAction
        permits RecordedAction.Click, RecordedAction.Fill, RecordedAction.WaitText {

    record Click(NormalizedRegion region) implements RecordedAction {
        public Click { Objects.requireNonNull(region, "region is null"); }
    }

    record Fill(NormalizedRegion region, String value) implements RecordedAction {
        public Fill {
            Objects.requireNonNull(region, "region is null");
            Objects.requireNonNull(value, "value is null");
        }
    }

    /**
     * Wait until OCR text contains the expected substring (case-insensitive recommended).
     */
    record WaitText(NormalizedRegion region, String expectedContains) implements RecordedAction {
        public WaitText {
            Objects.requireNonNull(region, "region is null");
            Objects.requireNonNull(expectedContains, "expectedContains is null");
        }
    }
}
