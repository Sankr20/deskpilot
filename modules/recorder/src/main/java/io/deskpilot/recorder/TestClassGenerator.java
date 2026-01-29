package io.deskpilot.recorder;

import io.deskpilot.engine.NormalizedRegion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestClassGenerator {

    public static void generateJUnit5(String pkg, String className, List<RecordedAction> actions, Path output)
            throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;\n");
        sb.append("import io.deskpilot.engine.NormalizedRegion;\n");
        sb.append("import io.deskpilot.engine.Locators;\n");
        sb.append("import org.junit.jupiter.api.Test;\n\n");
        sb.append("public class ").append(className).append(" extends BaseDeskPilotTestJUnit5 {\n\n");
        sb.append("  @Test\n");
        sb.append("  void recorded_flow() throws Exception {\n");
        sb.append("    session().step(\"recorded_flow\", () -> {\n");

        int i = 1;
        for (RecordedAction a : actions) {
            if (a instanceof RecordedAction.Click c) {
                String var = "r" + i;
                emitRegion(sb, var, c.region());
                sb.append("      actions().click(")
                        .append(pointLocatorExpr("rec_click_" + pad2(i), var))
                        .append(");\n\n");

                i++;
            } else if (a instanceof RecordedAction.Fill f) {
                String var = "r" + i;
                emitRegion(sb, var, f.region());
                sb.append("      actions().fill(")
                        .append(pointLocatorExpr("rec_fill_" + pad2(i), var))
                        .append(", ")
                        .append(javaString(f.value()))
                        .append(");\n\n");

                i++;
            }
        }

        sb.append("    });\n");
        sb.append("  }\n");
        sb.append("}\n");

        Files.writeString(output, sb.toString());
    }

    private static void emitRegion(StringBuilder sb, String var, NormalizedRegion r) {
        sb.append("      var ").append(var).append(" = new NormalizedRegion(")
                .append(fmt(r.xPct)).append(", ")
                .append(fmt(r.yPct)).append(", ")
                .append(fmt(r.wPct)).append(", ")
                .append(fmt(r.hPct)).append(");\n");
    }

    // Builds Locators.point("label", centerXPct, centerYPct)
    private static String pointLocatorExpr(String label, String regionVar) {
        return "Locators.point(\"" + label + "\", " +
                regionVar + ".xPct + (" + regionVar + ".wPct/2.0), " +
                regionVar + ".yPct + (" + regionVar + ".hPct/2.0))";
    }

    private static String pad2(int n) {
        return (n < 10) ? "0" + n : Integer.toString(n);
    }

    private static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.6f", d);
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
}
