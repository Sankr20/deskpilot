package io.deskpilot.cli;

import io.deskpilot.common.SafePaths;
import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.RunOptions;
import io.deskpilot.recorder.RecorderManager;
import io.deskpilot.recorder.TestClassGenerator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class RecorderCLI {

    private enum Framework { JUNIT5, TESTNG }

    public static int recordToFile(String[] args) {
        if (args == null) args = new String[0];

        if (args.length >= 1 && Main.isHelp(args[0])) {
            printUsage();
            return 0;
        }

        Framework framework = null;
        boolean force = false;

        Path explicitOutFile = null;
        Path projectDir = null;
        String packageName = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null) continue;
            a = a.trim();
            if (a.isEmpty()) continue;

            if (Main.isHelp(a)) { printUsage(); return 0; }

            if ("--force".equalsIgnoreCase(a)) { force = true; continue; }

            if ("--framework".equalsIgnoreCase(a)) {
                if (i + 1 >= args.length) {
                    System.err.println("Missing value after --framework (use junit5|testng)");
                    return 2;
                }
                String f = args[++i].trim().toLowerCase(Locale.ROOT);
                if ("junit5".equals(f)) framework = Framework.JUNIT5;
                else if ("testng".equals(f)) framework = Framework.TESTNG;
                else {
                    System.err.println("Unknown framework: " + f + " (use junit5|testng)");
                    return 2;
                }
                continue;
            }

            if ("--projectdir".equalsIgnoreCase(a) || "--projectDir".equals(a)) {
                if (i + 1 >= args.length) {
                    System.err.println("Missing value after --projectDir");
                    return 2;
                }
                projectDir = Path.of(args[++i].trim());
                continue;
            }

            if ("--package".equalsIgnoreCase(a)) {
                if (i + 1 >= args.length) {
                    System.err.println("Missing value after --package");
                    return 2;
                }
                packageName = args[++i].trim();
                if (packageName.isEmpty()) {
                    System.err.println("Package cannot be empty.");
                    return 2;
                }
                try {
                    SafePaths.validateJavaPackageOrThrow(packageName);
                } catch (Exception e) {
                    System.err.println("Invalid package: " + e.getMessage());
                    return 2;
                }
                continue;
            }

            if (!a.startsWith("--") && explicitOutFile == null) {
                explicitOutFile = Path.of(a);
            }
        }

        Path propsDir = (projectDir != null) ? projectDir : Path.of(".");
        Properties props = loadDeskpilotProps(propsDir);

        if (framework == null && props != null) {
            String fw = prop(props, "deskpilot.framework");
            if (fw != null) {
                fw = fw.toLowerCase(Locale.ROOT);
                if ("testng".equals(fw)) framework = Framework.TESTNG;
                if ("junit5".equals(fw)) framework = Framework.JUNIT5;
            }
        }

        if ((packageName == null || packageName.isBlank()) && props != null) {
            String p = prop(props, "deskpilot.package");
            if (p != null && !p.isBlank()) packageName = p.trim();
        }

        if (framework == null) framework = Framework.JUNIT5;
        if (packageName == null || packageName.isBlank()) packageName = "com.example";

        String effectivePkg = packageName.endsWith(".generated")
                ? packageName
                : (packageName + ".generated");

        if (framework == Framework.TESTNG && explicitOutFile == null && projectDir == null) {
            System.err.println("Usage:\n" +
                    "  deskpilot record --framework testng <outputFile>\n" +
                    "  deskpilot record --framework testng --projectDir <dir>\n" +
                    "  (add --force to overwrite)\n");
            return 2;
        }

        if (projectDir != null) {
            try {
                Path root = SafePaths.root(projectDir);
                SafePaths.ensureDir(root);
                Path testJava = SafePaths.under(root, "src", "test", "java");
                SafePaths.ensureDir(testJava);
            } catch (Exception e) {
                System.err.println("Project directory rejected: " + e.getMessage());
                return 2;
            }
        }

        if (explicitOutFile != null) {
            String name = explicitOutFile.getFileName() == null ? "" : explicitOutFile.getFileName().toString();
            if (!name.toLowerCase(Locale.ROOT).endsWith(".java")) {
                System.err.println("Output file must end with .java: " + explicitOutFile);
                return 2;
            }
        }

        RunOptions opts = RunOptions.builder()
                .runName("record-" + System.currentTimeMillis())
                .build();

        try (DeskPilotSession session = DeskPilotSession.attachPickWindow(opts)) {

            RecorderManager recorder = new RecorderManager(session);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            printHelp();

            while (true) {
                System.out.print("> ");
                String cmd = br.readLine();
                if (cmd == null) break;

                cmd = cmd.trim();
                if (cmd.isEmpty()) break; // ENTER = stop

                if (cmd.equalsIgnoreCase("S") || cmd.equalsIgnoreCase("STOP")) break;

                if (cmd.equalsIgnoreCase("Q") || cmd.equalsIgnoreCase("QUIT")) {
                    System.out.println("Recording cancelled (quit). No test was written.");
                    return 2;
                }

                if (cmd.equalsIgnoreCase("H") || cmd.equalsIgnoreCase("HELP")) {
                    printHelp();
                    continue;
                }

                if (cmd.equalsIgnoreCase("C")) {
                    System.out.println("Drag-select a region for CLICK...");
                    try {
                        var region = session.pickRegion();
                        recorder.recordClick(region);
                        System.out.println("Recorded: CLICK " + region);
                    } catch (IllegalStateException cancel) {
                        System.out.println("[CANCELLED] " + cancel.getMessage());
                    } catch (Exception e) {
                        System.out.println("[ERROR] Click capture failed: " + e.getMessage());
                    }
                    continue;
                }

                if (cmd.equalsIgnoreCase("F")) {
                    System.out.println("Drag-select a region for FILL...");
                    try {
                        var region = session.pickRegion();

                        System.out.print("Value: ");
                        String value = br.readLine();
                        if (value == null) value = "";

                        recorder.recordFill(region, value);
                        System.out.println("Recorded: FILL " + region + " = " + value);
                    } catch (IllegalStateException cancel) {
                        System.out.println("[CANCELLED] " + cancel.getMessage());
                    } catch (Exception e) {
                        System.out.println("[ERROR] Fill capture failed: " + e.getMessage());
                    }
                    continue;
                }

                if (cmd.equalsIgnoreCase("W")) {
                    System.out.println("Drag-select a region for WAIT (OCR contains)...");
                    try {
                        var region = session.pickRegion();

                        System.out.print("Expected text contains: ");
                        String expected = br.readLine();
                        if (expected == null) expected = "";
                        expected = expected.trim();

                        if (expected.isEmpty()) {
                            System.out.println("[ERROR] Expected text cannot be empty.");
                            continue;
                        }

                        recorder.recordWaitText(region, expected);
                        System.out.println("Recorded: WAIT " + region + " contains \"" + expected + "\"");
                    } catch (IllegalStateException cancel) {
                        System.out.println("[CANCELLED] " + cancel.getMessage());
                    } catch (Exception e) {
                        System.out.println("[ERROR] Wait capture failed: " + e.getMessage());
                    }
                    continue;
                }

                if (cmd.equalsIgnoreCase("K")) {
                    System.out.print("Hotkey (e.g., CTRL+V, ALT+F4): ");
                    String chord = br.readLine();
                    if (chord == null) chord = "";
                    chord = chord.trim();
                    if (chord.isEmpty()) {
                        System.out.println("[ERROR] Hotkey cannot be empty.");
                        continue;
                    }
                    recorder.recordHotkey(chord);
                    System.out.println("Recorded: HOTKEY " + chord);
                    continue;
                }

                if (cmd.equalsIgnoreCase("P")) {
                    System.out.print("Key (e.g., ENTER, TAB, ESC, UP): ");
                    String key = br.readLine();
                    if (key == null) key = "";
                    key = key.trim();
                    if (key.isEmpty()) {
                        System.out.println("[ERROR] Key cannot be empty.");
                        continue;
                    }
                    recorder.recordPress(key);
                    System.out.println("Recorded: PRESS " + key);
                    continue;
                }

                if (cmd.equalsIgnoreCase("T")) {
                    System.out.print("Text to type: ");
                    String text = br.readLine();
                    if (text == null) text = "";
                    recorder.recordTypeText(text);
                    System.out.println("Recorded: TYPE \"" + text + "\"");
                    continue;
                }

                System.out.println("Unknown command. Use C, F, W, K, P, T, HELP, or ENTER.");
            }

            var actions = recorder.getActions();
            System.out.println("Actions recorded: " + recorder.summary());

            if (actions.isEmpty()) {
                System.out.println("Recording cancelled: no actions were recorded.");
                return 2;
            }

            System.out.print("Write test? (Y/N) [Y]: ");
            String ans = br.readLine();
            if (ans == null) ans = "";
            ans = ans.trim();
            if (!ans.isEmpty() && ans.equalsIgnoreCase("N")) {
                System.out.println("Recording cancelled. No test was written.");
                return 2;
            }

            Path written;

            boolean repoWrite = (explicitOutFile == null && projectDir == null)
                    || isDeskPilotRepoWrite(explicitOutFile, projectDir);

            if (repoWrite) {
                written = TestClassGenerator.generateIntoRepoEngineGenerated(actions, force);
                System.out.println("Recorded test written to " + written);
                return 0;
            }

            if (explicitOutFile != null) {
                Path outFile = explicitOutFile.toAbsolutePath().normalize();
                SafePaths.rejectReservedWindowsName(outFile.getFileName().toString());

                if (projectDir != null) {
                    Path normRoot = SafePaths.root(projectDir);
                    SafePaths.requireUnderRoot(normRoot, outFile);
                }

                if (framework == Framework.JUNIT5) {
                    TestClassGenerator.generateJUnit5(effectivePkg, "RecordedTest", actions, outFile, force);
                } else {
                    TestClassGenerator.generateTestNG(effectivePkg, "RecordedTest", actions, outFile, force);
                }
                written = outFile;
            } else {
                Path normRoot = SafePaths.root(projectDir);
                SafePaths.ensureDir(normRoot);

                if (framework == Framework.JUNIT5) {
                    written = TestClassGenerator.generateJUnit5ToProjectDir(effectivePkg, "", actions, normRoot, force);
                } else {
                    written = TestClassGenerator.generateTestNGToProjectDir(effectivePkg, "", actions, normRoot, force);
                }
            }

            System.out.println("Recorded test written to " + written);
            return 0;

        } catch (Throwable t) {
            System.err.println("❌ RECORD FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            return 1;
        }
    }

    private static void printUsage() {
        System.out.println(
                "Usage:\n" +
                        "  deskpilot record\n" +
                        "  deskpilot record --force\n" +
                        "  deskpilot record --framework testng <outputFile>\n" +
                        "  deskpilot record --framework testng --projectDir <dir>\n" +
                        "\n" +
                        "Options:\n" +
                        "  --framework junit5|testng\n" +
                        "  --projectDir <dir>\n" +
                        "  --package <javaPackage>\n" +
                        "  --force\n" +
                        "  --help\n"
        );
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Recorder ready.");
        System.out.println("Commands:");
        System.out.println("  C       = capture CLICK region");
        System.out.println("  F       = capture FILL region + value");
        System.out.println("  W       = capture WAIT region + expected OCR text");
        System.out.println("  K       = capture HOTKEY (e.g., CTRL+V)");
        System.out.println("  P       = capture PRESS (e.g., ENTER/TAB)");
        System.out.println("  T       = capture TYPE text (no locator)");
        System.out.println("  HELP    = show commands");
        System.out.println("  S/STOP  = stop recording");
        System.out.println("  Q/QUIT  = quit without writing");
        System.out.println("  ENTER   = stop recording");
        System.out.println();
    }

    private static boolean isInsideDeskPilotRepoEngine(Path p) {
        if (p == null) return false;
        String s = p.toString().replace("\\", "/").toLowerCase(Locale.ROOT);
        return s.contains("/modules/engine/") || s.endsWith("/modules/engine");
    }

    private static boolean isDeskPilotRepoWrite(Path explicitOutFile, Path projectDir) {
        return isInsideDeskPilotRepoEngine(explicitOutFile) || isInsideDeskPilotRepoEngine(projectDir);
    }

    private static Properties loadDeskpilotProps(Path dir) {
        try {
            Path p = dir.resolve("deskpilot.properties");
            if (!Files.exists(p)) return null;
            Properties props = new Properties();
            try (var in = Files.newInputStream(p)) {
                props.load(in);
            }
            return props;
        } catch (Exception e) {
            return null;
        }
    }

    private static String prop(Properties props, String key) {
        if (props == null) return null;
        String v = props.getProperty(key);
        return (v == null) ? null : v.trim();
    }

    private RecorderCLI() {}
}
