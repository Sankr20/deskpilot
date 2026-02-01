package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TestClassGenerator {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ---------------- JUnit 5 (project output; uses testkit) ----------------

    public static void generateJUnit5(String pkg, String className, List<RecordedAction> actions, Path output)
            throws Exception {
        generateJUnit5(pkg, className, actions, output, false);
    }

    public static void generateJUnit5(String pkg, String className, List<RecordedAction> actions, Path output, boolean force)
            throws Exception {
        requireNonEmpty(actions);
        String src = buildJUnit5Source(pkg, className, actions);
        writeFile(output, src, force);
    }

    public static Path generateJUnit5ToProjectDir(String pkg, String classNameOrBlank, List<RecordedAction> actions, Path projectDir)
            throws Exception {
        return generateJUnit5ToProjectDir(pkg, classNameOrBlank, actions, projectDir, false);
    }

    public static Path generateJUnit5ToProjectDir(String pkg, String classNameOrBlank, List<RecordedAction> actions, Path projectDir, boolean force)
            throws Exception {
        String className = (classNameOrBlank == null || classNameOrBlank.isBlank())
                ? ("RecordedTest_" + TS.format(LocalDateTime.now()) + "Test")
                : classNameOrBlank;

        Path outFile = projectDir
                .resolve("src/test/java")
                .resolve(pkg.replace('.', '/'))
                .resolve(className + ".java");

        generateJUnit5(pkg, className, actions, outFile, force);
        return outFile;
    }

    /**
     * Repo convenience: write into modules/engine generated folder.
     * IMPORTANT: engine module must not depend on testkit, so this is standalone JUnit.
     */
    public static Path generateIntoRepoEngineGenerated(List<RecordedAction> actions) throws Exception {
        return generateIntoRepoEngineGenerated(actions, false);
    }

    public static Path generateIntoRepoEngineGenerated(List<RecordedAction> actions, boolean force) throws Exception {
        requireNonEmpty(actions);

        String pkg = "io.deskpilot.tests.generated";
        String className = "RecordedTest_" + TS.format(LocalDateTime.now()) + "Test";

        Path outFile = Path.of("modules", "engine")
                .resolve("src/test/java")
                .resolve(pkg.replace('.', '/'))
                .resolve(className + ".java");

        String src = buildEngineStandaloneJUnitSource(pkg, className, actions);
        writeFile(outFile, src, force);
        return outFile;
    }

    // ---------------- TestNG (project output; uses testkit) ----------------

    public static void generateTestNG(String pkg, String className, List<RecordedAction> actions, Path output)
            throws Exception {
        generateTestNG(pkg, className, actions, output, false);
    }

    public static void generateTestNG(String pkg, String className, List<RecordedAction> actions, Path output, boolean force)
            throws Exception {
        requireNonEmpty(actions);
        String src = buildTestNGSource(pkg, className, actions);
        writeFile(output, src, force);
    }

    public static Path generateTestNGToProjectDir(String pkg, String classNameOrBlank, List<RecordedAction> actions, Path projectDir)
            throws Exception {
        return generateTestNGToProjectDir(pkg, classNameOrBlank, actions, projectDir, false);
    }

    public static Path generateTestNGToProjectDir(String pkg, String classNameOrBlank, List<RecordedAction> actions, Path projectDir, boolean force)
            throws Exception {

        String className = (classNameOrBlank == null || classNameOrBlank.isBlank())
                ? ("RecordedTest_" + TS.format(LocalDateTime.now()) + "Test")
                : classNameOrBlank;

        Path outFile = projectDir
                .resolve("src/test/java")
                .resolve(pkg.replace('.', '/'))
                .resolve(className + ".java");

        generateTestNG(pkg, className, actions, outFile, force);
        return outFile;
    }

    // ---------------- Source builders ----------------

    private static String buildJUnit5Source(String pkg, String className, List<RecordedAction> actions) {
        List<RecordedAction> ordered = orderActions(actions);

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
        sb.append("      final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);\n\n");

        emitActionsForProject(sb, ordered);

        sb.append("    });\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildTestNGSource(String pkg, String className, List<RecordedAction> actions) {
        List<RecordedAction> ordered = orderActions(actions);

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
        sb.append("      final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);\n\n");

        emitActionsForProject(sb, ordered);

        sb.append("    });\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String buildEngineStandaloneJUnitSource(String pkg, String className, List<RecordedAction> actions) {
        List<RecordedAction> ordered = orderActions(actions);

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
        sb.append("      Actions a = new Actions(s);\n");
        sb.append("      final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);\n\n");

        emitActionsForEngineStandalone(sb, ordered);

        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ---------------- Emitters ----------------

    private static void emitActionsForProject(StringBuilder sb, List<RecordedAction> actions) {
        int i = 1;
        int fillIndex = 1;
        int clickIndex = 1;
        int waitIndex = 1;

        for (RecordedAction a : actions) {

            String regionVar = "r" + pad2(i);
            if (a instanceof RecordedAction.Fill f) {
                emitRegion(sb, regionVar, f.region());
                sb.append("      actions().fill(")
                        .append(pointLocatorExpr("rec_fill_" + pad2(fillIndex), regionVar))
                        .append(", ")
                        .append(javaString(f.value()))
                        .append(");\n\n");
                fillIndex++;
                i++;
                continue;
            }

            if (a instanceof RecordedAction.Click c) {
                emitRegion(sb, regionVar, c.region());
                sb.append("      actions().click(")
                        .append(pointLocatorExpr("rec_click_" + pad2(clickIndex), regionVar))
                        .append(");\n\n");
                clickIndex++;
                i++;
                continue;
            }

            if (a instanceof RecordedAction.WaitText w) {
                emitRegion(sb, regionVar, w.region());
                sb.append("      actions().withTimeout(WAIT_TIMEOUT).waitFor(")
                        .append("Locators.ocrContains(\"rec_wait_")
                        .append(pad2(waitIndex))
                        .append("\", ")
                        .append(regionVar)
                        .append(", ")
                        .append(javaString(w.expectedContains()))
                        .append("));\n\n");
                waitIndex++;
                i++;
            }
        }
    }

    private static void emitActionsForEngineStandalone(StringBuilder sb, List<RecordedAction> actions) {
        int i = 1;
        int fillIndex = 1;
        int clickIndex = 1;
        int waitIndex = 1;

        for (RecordedAction ra : actions) {

            String regionVar = "r" + pad2(i);
            if (ra instanceof RecordedAction.Fill f) {
                emitRegion(sb, regionVar, f.region());
                sb.append("      a.fill(")
                        .append(pointLocatorExpr("rec_fill_" + pad2(fillIndex), regionVar))
                        .append(", ")
                        .append(javaString(f.value()))
                        .append(");\n\n");
                fillIndex++;
                i++;
                continue;
            }

            if (ra instanceof RecordedAction.Click c) {
                emitRegion(sb, regionVar, c.region());
                sb.append("      a.click(")
                        .append(pointLocatorExpr("rec_click_" + pad2(clickIndex), regionVar))
                        .append(");\n\n");
                clickIndex++;
                i++;
                continue;
            }

            if (ra instanceof RecordedAction.WaitText w) {
                emitRegion(sb, regionVar, w.region());
                sb.append("      a.withTimeout(WAIT_TIMEOUT).waitFor(")
                        .append("Locators.ocrContains(\"rec_wait_")
                        .append(pad2(waitIndex))
                        .append("\", ")
                        .append(regionVar)
                        .append(", ")
                        .append(javaString(w.expectedContains()))
                        .append("));\n\n");
                waitIndex++;
                i++;
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

    // ---------------- Ordering ----------------

    private static List<RecordedAction> orderActions(List<RecordedAction> actions) {
        List<RecordedAction> nonWait = new ArrayList<>();
        List<RecordedAction> waits = new ArrayList<>();

        for (RecordedAction a : actions) {
            if (a instanceof RecordedAction.WaitText) waits.add(a);
            else nonWait.add(a);
        }

        List<RecordedAction> out = new ArrayList<>(actions.size());
        out.addAll(nonWait);
        out.addAll(waits);
        return out;
    }

    // ---------------- Utils ----------------

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

    private static void writeFile(Path file, String content, boolean force) throws IOException {
        Files.createDirectories(file.toAbsolutePath().normalize().getParent());

        if (force) {
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } else {
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
    }

    private TestClassGenerator() {}
}
