package io.deskpilot.engine.recorder;

/**
 * A minimal representation of a recorded action/assertion that can be rendered into
 * a runnable JUnit test using Actions.
 */
public sealed interface RecordedStep
permits RecordedStep.Click,
        RecordedStep.Fill,
        RecordedStep.WaitForFound,
        RecordedStep.Macro
 {

    /** Click a locator (must resolve to a point). */
    record Click(String locatorConst) implements RecordedStep {
        public Click {
            if (locatorConst == null || locatorConst.isBlank())
                throw new IllegalArgumentException("locatorConst is blank");
        }
    }

    /** Fill a field locator with text (click + clear + paste). */
    record Fill(String fieldLocatorConst, String text) implements RecordedStep {
        public Fill {
            if (fieldLocatorConst == null || fieldLocatorConst.isBlank())
                throw new IllegalArgumentException("fieldLocatorConst is blank");
            if (text == null)
                throw new IllegalArgumentException("text is null");
        }
    }

    /** Wait until OCR text within the given region locator contains expected substring. */
/** Wait until an OCR locator is FOUND (it encapsulates the text rule). */
record WaitForFound(String ocrLocatorConst, long timeoutMs) implements RecordedStep {
    public WaitForFound {
        if (ocrLocatorConst == null || ocrLocatorConst.isBlank())
            throw new IllegalArgumentException("ocrLocatorConst is blank");
        if (timeoutMs <= 0)
            throw new IllegalArgumentException("timeoutMs must be > 0");
    }
}


    /**
     * A macro step that already contains a rendered Java line.
     * Used internally by the recorder/writer to collapse flows.
     */
    record Macro(String renderedJavaLine) implements RecordedStep {
        public Macro {
            if (renderedJavaLine == null || renderedJavaLine.isBlank())
                throw new IllegalArgumentException("renderedJavaLine is blank");
        }
    }
}
