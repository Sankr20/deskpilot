package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;
import java.util.Objects;

public sealed interface RecordedAction permits RecordedAction.Click, RecordedAction.Fill {

    record Click(NormalizedRegion region) implements RecordedAction {
        public Click { Objects.requireNonNull(region, "region is null"); }
    }

    record Fill(NormalizedRegion region, String value) implements RecordedAction {
        public Fill {
            Objects.requireNonNull(region, "region is null");
            Objects.requireNonNull(value, "value is null");
        }
    }
}
