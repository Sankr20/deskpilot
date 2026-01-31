package io.deskpilot.engine.recorder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Writes a runnable JUnit 5 test into:
 * modules/engine/src/test/java/io/deskpilot/tests/generated
 *
 * This writer only generates code. It does not execute anything.
 */
public final class RecordedTestWriter {

    private static final String PACKAGE = "io.deskpilot.tests.generated";

    // relative to repo root
    private static final Path GENERATED_TEST_DIR = Path.of("modules", "engine", "src", "test", "java",
            "io", "deskpilot", "tests", "generated");

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private RecordedTestWriter() {
    }

    public static Path writeTest(
            String className,
            String testMethodName,
            String pickWindowLabel,
            List<RecordedStep> steps) throws IOException {

        if (className == null || className.isBlank())
            throw new IllegalArgumentException("className is blank");
        if (testMethodName == null || testMethodName.isBlank())
            throw new IllegalArgumentException("testMethodName is blank");
        if (pickWindowLabel == null || pickWindowLabel.isBlank())
            throw new IllegalArgumentException("pickWindowLabel is blank");

        Objects.requireNonNull(steps, "steps is null");

        boolean anyReal = steps.stream().anyMatch(s ->
        s instanceof RecordedStep.Click
     || s instanceof RecordedStep.Fill
     || s instanceof RecordedStep.WaitForFound
);

if (!anyReal) {
    throw new IllegalArgumentException("Refusing to write empty recording (no Click/Fill/Wait steps).");
}

        Files.createDirectories(GENERATED_TEST_DIR);

        String src = render(className, testMethodName, pickWindowLabel, steps);

        Path out = GENERATED_TEST_DIR.resolve(className + ".java");
        Files.writeString(out, src, StandardCharsets.UTF_8);

        return out;
    }

    public static String defaultClassName() {
        // ✅ ends with Test so surefire runs it by default patterns
        return "RecordedTest_" + TS.format(LocalDateTime.now()) + "Test";
    }

    public static String defaultMethodName() {
        return "recorded_flow";
    }

    private static String render(
            String className,
            String testMethodName,
            String pickWindowLabel,
            List<RecordedStep> steps) {
        StringBuilder b = new StringBuilder(4096);

        b.append("package ").append(PACKAGE).append(";\n\n")
                .append("import io.deskpilot.engine.DeskPilotSession;\n")
                .append("import io.deskpilot.engine.Locators;\n")
                .append("import io.deskpilot.engine.actions.Actions;\n")
                .append("import org.junit.jupiter.api.Test;\n")
                .append("import java.time.Duration;\n\n")
                .append("public class ").append(className).append(" {\n\n")
                .append("    @Test\n")
                .append("    void ").append(testMethodName).append("() throws Exception {\n\n")
                .append("        try (DeskPilotSession s = DeskPilotSession.attachPickWindow(\"")
                .append(escapeJava(pickWindowLabel))
                .append("\")) {\n\n")
                .append("            Actions a = new Actions(s);\n\n");

        for (String line : renderLines(steps)) {
            b.append(line);
        }

        b.append("\n")
                .append("        }\n")
                .append("    }\n")
                .append("}\n");

        return b.toString();
    }

    private static List<String> renderLines(List<RecordedStep> steps) {
        List<String> out = new ArrayList<>();
        if (steps == null || steps.isEmpty())
            return out;

        for (int i = 0; i < steps.size(); i++) {
            RecordedStep step = steps.get(i);

            // Keep macros verbatim
            if (step instanceof RecordedStep.Macro m) {
                String line = m.renderedJavaLine().trim();
                if (!line.endsWith(";"))
                    line = line + ";";
                out.add("            " + line + "\n");
                continue;
            }

            // ---- Folding patterns ----

            // Fill → Click → Wait
            if (step instanceof RecordedStep.Fill f
                    && i + 2 < steps.size()
                    && steps.get(i + 1) instanceof RecordedStep.Click c
                    && steps.get(i + 2) instanceof RecordedStep.WaitForFound w) {

                out.add("            a.fill(Locators." + f.fieldLocatorConst()
                        + ", \"" + escapeJava(f.text()) + "\");\n");
                out.add("            a.click(Locators." + c.locatorConst() + ");\n");
                out.add(renderWaitLine(w));

                i += 2;
                continue;
            }

            // Click → Wait
            if (step instanceof RecordedStep.Click c
                    && i + 1 < steps.size()
                    && steps.get(i + 1) instanceof RecordedStep.WaitForFound w) {

                out.add("            a.click(Locators." + c.locatorConst() + ");\n");
                out.add(renderWaitLine(w));

                i += 1;
                continue;
            }

            // Fill → Wait
            if (step instanceof RecordedStep.Fill f
                    && i + 1 < steps.size()
                    && steps.get(i + 1) instanceof RecordedStep.WaitForFound w) {

                out.add("            a.fill(Locators." + f.fieldLocatorConst()
                        + ", \"" + escapeJava(f.text()) + "\");\n");
                out.add(renderWaitLine(w));

                i += 1;
                continue;
            }

            // ---- Fallback primitives ----

            out.add(renderStepLine(step));
        }

        return out;
    }

    private static String renderStepLine(RecordedStep step) {
        if (step == null)
            return "            // (null step skipped)\n";

        // Macro lines: emitted verbatim as Java statement
        if (step instanceof RecordedStep.Macro m) {
            String line = m.renderedJavaLine().trim();
            if (!line.endsWith(";"))
                line = line + ";";
            return "            " + line + "\n";
        }

        if (step instanceof RecordedStep.Click s) {
            return "            a.click(Locators." + s.locatorConst() + ");\n";
        }

        if (step instanceof RecordedStep.Fill s) {
            // Canonical: click + Ctrl+A + paste
            return "            a.fill(Locators." + s.fieldLocatorConst() +
                    ", \"" + escapeJava(s.text()) + "\");\n";
        }

        // ✅ New step type: wait for OCR locator FOUND (encapsulates condition)
        if (step instanceof RecordedStep.WaitForFound s) {
            return renderWaitLine(s);
        }

        return "            // (unknown step type: " + step.getClass().getName() + ")\n";
    }

    private static String escapeJava(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String renderWaitLine(RecordedStep.WaitForFound w) {
        return "            a.withTimeout(Duration.ofMillis(" + w.timeoutMs() + "L))" +
                ".waitFor(Locators." + w.ocrLocatorConst() + ");\n";
    }

}
