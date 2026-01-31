package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class TestClassGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ---------------- JUnit 5 (project output; uses testkit) ----------------

    public static void generateJUnit5(String pkg, String className, List<RecordedAction> actions, Path output)
            throws Exception {
                requireNonEmpty(actions);
        String src = buildJUnit5Source(pkg, className, actions);
        Files.createDirectories(output.getParent());
        Files.writeString(output, src);
    }

    public static Path generateJUnit5ToProjectDir(
            String pkg,
            String classNameOrBlank,
            List<RecordedAction> actions,
            Path projectDir
    ) throws Exception {

        String className = (classNameOrBlank == null || classNameOrBlank.isBlank())
                ? ("RecordedTest_" + TS.format(LocalDateTime.now()) + "Test")
                : classNameOrBlank;

        Path outFile = projectDir
                .resolve("src/test/java")
                .resolve(pkg.replace('.', '/'))
                .resolve(className + ".java");

        generateJUnit5(pkg, className, actions, outFile);
        return outFile;
    }

    /**
     * Repo convenience: write into modules/engine generated folder.
     * IMPORTANT: engine module must not depend on testkit, so this is standalone JUnit.
     */
    public static Path generateIntoRepoEngineGenerated(List<RecordedAction> actions) throws Exception {
    requireNonEmpty(actions);
        String pkg = "io.deskpilot.tests.generated";
    String className = "RecordedTest_" + TS.format(LocalDateTime.now()) + "Test";

    Path outFile = Path.of("modules", "engine")
            .resolve("src/test/java")
            .resolve(pkg.replace('.', '/'))
            .resolve(className + ".java");

    // ✅ ALWAYS standalone for repo engine output (NO testkit)
    String src = buildEngineStandaloneJUnitSource(pkg, className, actions);

    Files.createDirectories(outFile.getParent());
    Files.writeString(outFile, src);
    return outFile;
}


   // ---------------- TestNG (project output; uses testkit) ----------------

public static void generateTestNG(String pkg, String className, List<RecordedAction> actions, Path output)
        throws Exception {
            requireNonEmpty(actions);
    String src = buildTestNGSource(pkg, className, actions);
    Files.createDirectories(output.getParent());
    Files.writeString(output, src);
}

public static Path generateTestNGToProjectDir(
        String pkg,
        String classNameOrBlank,
        List<RecordedAction> actions,
        Path projectDir
) throws Exception {

    String className = (classNameOrBlank == null || classNameOrBlank.isBlank())
            ? ("RecordedTest_" + TS.format(LocalDateTime.now()) + "Test")
            : classNameOrBlank;

    Path outFile = projectDir
            .resolve("src/test/java")
            .resolve(pkg.replace('.', '/'))
            .resolve(className + ".java");

    generateTestNG(pkg, className, actions, outFile);
    return outFile;
}

    // ---------------- Source builders ----------------

    private static String buildJUnit5Source(String pkg, String className, List<RecordedAction> actions) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;\n");
        sb.append("import io.deskpilot.engine.NormalizedRegion;\n");
        sb.append("import io.deskpilot.engine.Locators;\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import java.time.Duration;\n\n");

        sb.append("public class ").append(className).append(" extends BaseDeskPilotTestJUnit5 {\n\n");
        sb.append("  @Test\n");
        sb.append("  void recorded_flow() throws Exception {\n");
        sb.append("    session().step(\"recorded_flow\", () -> {\n\n");

        emitActionsForProject(sb, actions);

        sb.append("    });\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private static String buildTestNGSource(String pkg, String className, List<RecordedAction> actions) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import io.deskpilot.testkit.BaseDeskPilotTestTestNG;\n");
        sb.append("import io.deskpilot.engine.NormalizedRegion;\n");
        sb.append("import io.deskpilot.engine.Locators;\n");
        sb.append("import org.testng.annotations.Test;\n");
        sb.append("import java.time.Duration;\n\n");

        sb.append("public class ").append(className).append(" extends BaseDeskPilotTestTestNG {\n\n");
        sb.append("  @Test\n");
        sb.append("  public void recorded_flow() throws Exception {\n");
        sb.append("    session().step(\"recorded_flow\", () -> {\n\n");

        emitActionsForProject(sb, actions);

        sb.append("    });\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Standalone JUnit test for engine module (NO testkit dependency).
     */
    private static String buildEngineStandaloneJUnitSource(String pkg, String className, List<RecordedAction> actions) {
        StringBuilder sb = new StringBuilder();

        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import io.deskpilot.engine.DeskPilotSession;\n");
        sb.append("import io.deskpilot.engine.NormalizedRegion;\n");
        sb.append("import io.deskpilot.engine.Locators;\n");
        sb.append("import io.deskpilot.engine.actions.Actions;\n");
        sb.append("import org.junit.jupiter.api.Test;\n");
        sb.append("import java.time.Duration;\n\n");

        sb.append("public class ").append(className).append(" {\n\n");
        sb.append("  @Test\n");
        sb.append("  void recorded_flow() throws Exception {\n");
        sb.append("    try (DeskPilotSession s = DeskPilotSession.attachPickWindow(\"recorded-flow\")) {\n");
        sb.append("      Actions a = new Actions(s);\n\n");

        int idx = 1, fillIdx = 1, clickIdx = 1, waitIdx = 1;

        for (RecordedAction ra : actions) {

            if (ra instanceof RecordedAction.Fill f) {
                String var = "r" + idx;
                emitRegion(sb, var, f.region());

                sb.append("      a.fill(")
                        .append(pointLocatorExpr("rec_fill_" + pad2(fillIdx), var))
                        .append(", ")
                        .append(javaString(f.value()))
                        .append(");\n\n");

                idx++; fillIdx++;
                continue;
            }

            if (ra instanceof RecordedAction.Click c) {
                String var = "r" + idx;
                emitRegion(sb, var, c.region());

                sb.append("      a.click(")
                        .append(pointLocatorExpr("rec_click_" + pad2(clickIdx), var))
                        .append(");\n\n");

                idx++; clickIdx++;
                continue;
            }

            if (ra instanceof RecordedAction.WaitText w) {
                String var = "r" + idx;
                emitRegion(sb, var, w.region());

                sb.append("      a.withTimeout(Duration.ofMillis(5000)).waitFor(")
                        .append("Locators.ocrContains(\"rec_wait_")
                        .append(pad2(waitIdx))
                        .append("\", ")
                        .append(var)
                        .append(", ")
                        .append(javaString(w.expectedContains()))
                        .append("));\n\n");

                idx++; waitIdx++;
            }
        }

        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Shared emitter for PROJECT output (testkit base tests).
     * WaitText uses Locators.ocrContains(label, region, expectedContains) and timeout via actions().withTimeout(...)
     */
    private static void emitActionsForProject(StringBuilder sb, List<RecordedAction> actions) {
        int stepIndex = 1;
        int fillIndex = 1;
        int clickIndex = 1;
        int waitIndex = 1;

        for (RecordedAction a : actions) {

            if (a instanceof RecordedAction.Fill f) {
                String var = "r" + stepIndex;
                emitRegion(sb, var, f.region());

                sb.append("      actions().fill(")
                        .append(pointLocatorExpr("rec_fill_" + pad2(fillIndex), var))
                        .append(", ")
                        .append(javaString(f.value()))
                        .append(");\n\n");

                stepIndex++; fillIndex++;
                continue;
            }

            if (a instanceof RecordedAction.Click c) {
                String var = "r" + stepIndex;
                emitRegion(sb, var, c.region());

                sb.append("      actions().click(")
                        .append(pointLocatorExpr("rec_click_" + pad2(clickIndex), var))
                        .append(");\n\n");

                stepIndex++; clickIndex++;
                continue;
            }

            if (a instanceof RecordedAction.WaitText w) {
                String var = "r" + stepIndex;
                emitRegion(sb, var, w.region());

                sb.append("      actions().withTimeout(Duration.ofMillis(5000)).waitFor(")
                        .append("Locators.ocrContains(\"rec_wait_")
                        .append(pad2(waitIndex))
                        .append("\", ")
                        .append(var)
                        .append(", ")
                        .append(javaString(w.expectedContains()))
                        .append("));\n\n");

                stepIndex++; waitIndex++;
            }
        }
    }

    private static void emitRegion(StringBuilder sb, String var, NormalizedRegion r) {
        sb.append("      var ").append(var).append(" = new NormalizedRegion(")
                .append(fmt(r.xPct)).append(", ")
                .append(fmt(r.yPct)).append(", ")
                .append(fmt(r.wPct)).append(", ")
                .append(fmt(r.hPct)).append(");\n");
    }

    private static String pointLocatorExpr(String label, String regionVar) {
        return "Locators.point(\"" + label + "\", " +
                regionVar + ".xPct + (" + regionVar + ".wPct/2.0), " +
                regionVar + ".yPct + (" + regionVar + ".hPct/2.0))";
    }

    private static String pad2(int n) {
        return (n < 10) ? "0" + n : Integer.toString(n);
    }

    private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.6f", d);
    }

    private static String javaString(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"";
    }

    private static void requireNonEmpty(List<RecordedAction> actions) {
    if (actions == null || actions.isEmpty()) {
        throw new IllegalArgumentException("Refusing to generate test: no actions were recorded.");
    }
}


    private TestClassGenerator() {}
}
