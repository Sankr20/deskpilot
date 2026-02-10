package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;
import java.util.Objects;

public sealed interface RecordedAction
        permits RecordedAction.Click, RecordedAction.Fill, RecordedAction.WaitText,
                RecordedAction.Hotkey, RecordedAction.Press, RecordedAction.TypeText {

    record Click(NormalizedRegion region) implements RecordedAction {
        public Click { Objects.requireNonNull(region, "region is null"); }
    }

    record Fill(NormalizedRegion region, String value) implements RecordedAction {
        public Fill {
            Objects.requireNonNull(region, "region is null");
            Objects.requireNonNull(value, "value is null");
        }
    }

    record WaitText(NormalizedRegion region, String expectedContains) implements RecordedAction {
        public WaitText {
            Objects.requireNonNull(region, "region is null");
            Objects.requireNonNull(expectedContains, "expectedContains is null");
        }
    }

    record Hotkey(String chord) implements RecordedAction {
        public Hotkey { Objects.requireNonNull(chord, "chord is null"); }
    }

    record Press(String key) implements RecordedAction {
        public Press { Objects.requireNonNull(key, "key is null"); }
    }

    record TypeText(String text) implements RecordedAction {
        public TypeText { Objects.requireNonNull(text, "text is null"); }
    }
}
