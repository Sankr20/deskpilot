package io.deskpilot.cli;

import io.deskpilot.engine.DeskPilot;
import io.deskpilot.engine.RunOptions;
import io.deskpilot.recorder.RecorderManager;
import io.deskpilot.recorder.TestClassGenerator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public final class RecorderCLI {

    private enum Framework {
        JUNIT5, TESTNG
    }

    /**
     * Usage:
     * deskpilot record
     * deskpilot record <outputFile>
     * deskpilot record --framework testng <outputFile>
     * deskpilot record --framework testng --projectDir
     * <dir>
     * deskpilot record --projectDir
     * <dir>
     */
    public static int recordToFile(String[] args) {
        Framework framework = Framework.JUNIT5;
        Path explicitOutFile = null;
        Path projectDir = null;

        if (args == null)
            args = new String[0];

        // ---------------- Parse args ----------------
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a == null)
                continue;
            a = a.trim();
            if (a.isEmpty())
                continue;

            if ("--framework".equalsIgnoreCase(a)) {
                if (i + 1 >= args.length) {
                    System.err.println("Missing value after --framework (use junit5|testng)");
                    return 2;
                }
                String f = args[++i].trim().toLowerCase();
                if ("junit5".equals(f)) {
                    framework = Framework.JUNIT5;
                } else if ("testng".equals(f)) {
                    framework = Framework.TESTNG;
                } else {
                    System.err.println("Unknown framework: " + f + " (use junit5|testng)");
                    return 2;
                }
                continue;
            }

            if ("--projectdir".equalsIgnoreCase(a)) {
                if (i + 1 >= args.length) {
                    System.err.println("Missing value after --projectDir");
                    return 2;
                }
                projectDir = Path.of(args[++i].trim());
                continue;
            }

            // First non-flag token = output file (optional)
            if (!a.startsWith("--") && explicitOutFile == null) {
                explicitOutFile = Path.of(a);
            }
        }

        // ---------------- Safety defaults ----------------
        // TestNG must not default into repo engine folder.
        if (framework == Framework.TESTNG && explicitOutFile == null && projectDir == null) {
            System.err.println("Usage:\n" +
                    "  deskpilot record --framework testng <outputFile>\n" +
                    "  deskpilot record --framework testng --projectDir <dir>\n");
            return 2;
        }

        RunOptions opts = RunOptions.builder()
                .runName("record-" + System.currentTimeMillis())
                .build();

        try (var session = DeskPilot.attachPickWindow(opts)) {

            RecorderManager recorder = new RecorderManager(session);
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            printHelp();

            // ---------------- Record loop ----------------
            while (true) {
                System.out.print("> ");
                String cmd = br.readLine();
                if (cmd == null)
                    break;

                cmd = cmd.trim();
                if (cmd.isEmpty()) {
                    // ENTER = stop recording (will later confirm write)
                    break;
                }

                if (cmd.equalsIgnoreCase("S") || cmd.equalsIgnoreCase("STOP")) {
                    // explicit stop
                    break;
                }

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
                        if (value == null)
                            value = "";

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
                        if (expected == null)
                            expected = "";
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

                System.out.println("Unknown command. Use C, F, W, HELP, or ENTER.");
            }

            var actions = recorder.getActions();
            System.out.println("Actions recorded: " + recorder.summary());

            // ---------------- Refuse empty recording ----------------
            if (actions.isEmpty()) {
                System.out.println("Recording cancelled: no actions were recorded.");
                return 2; // non-zero exit, intentional
            }
            System.out.print("Write test? (Y/N) [Y]: ");
            String ans = br.readLine();
            if (ans == null)
                ans = "";
            ans = ans.trim();

            if (!ans.isEmpty() && ans.equalsIgnoreCase("N")) {
                System.out.println("Recording cancelled. No test was written.");
                return 2;
            }

            // ---------------- Write output ----------------
            Path written;

            // Repo write detection: default (no args) is repo output
            boolean repoWrite = (explicitOutFile == null && projectDir == null)
                    || isDeskPilotRepoWrite(explicitOutFile, projectDir);

            // In repo mode, NEVER generate testkit-based tests
            if (repoWrite) {
                written = TestClassGenerator.generateIntoRepoEngineGenerated(actions);
                System.out.println("Recorded test written to " + written);
                return 0;
            }

            // Project mode outputs
            if (explicitOutFile != null) {
                if (framework == Framework.JUNIT5) {
                    TestClassGenerator.generateJUnit5("com.example", "RecordedTest", actions, explicitOutFile);
                } else {
                    TestClassGenerator.generateTestNG("com.example", "RecordedTest", actions, explicitOutFile);
                }
                written = explicitOutFile;

            } else if (projectDir != null) {
                if (framework == Framework.JUNIT5) {
                    written = TestClassGenerator.generateJUnit5ToProjectDir("com.example", "", actions, projectDir);
                } else {
                    written = TestClassGenerator.generateTestNGToProjectDir("com.example", "", actions, projectDir);
                }

            } else {
                // Should not happen because repoWrite covers this, but keep safe
                written = TestClassGenerator.generateIntoRepoEngineGenerated(actions);
            }

            System.out.println("Recorded test written to " + written);
            return 0;

        } catch (Throwable t) {
            System.err.println("❌ RECORD FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            return 1;
        }
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Recorder ready.");
        System.out.println("Commands:");
        System.out.println("  C     = capture CLICK region");
        System.out.println("  F     = capture FILL region + value");
        System.out.println("  W     = capture WAIT region + expected OCR text");
        System.out.println("  HELP  = show commands");
        System.out.println("  S/STOP  = stop recording");
        System.out.println("  Q/QUIT  = quit without writing");
        System.out.println("  ENTER   = stop recording");

        System.out.println();
        System.out.println("Examples:");
        System.out.println("  deskpilot record");
        System.out.println("  deskpilot record --framework testng C:\\Temp\\RecordedTest.java");
        System.out.println("  deskpilot record --framework testng --projectDir C:\\Work\\MyProj");
        System.out.println();
    }

    // -------- Repo write detection --------

    private static boolean isInsideDeskPilotRepoEngine(Path p) {
        if (p == null)
            return false;
        String s = p.toString().replace("\\", "/").toLowerCase();
        return s.contains("/modules/engine/") || s.endsWith("/modules/engine");
    }

    private static boolean isDeskPilotRepoWrite(Path explicitOutFile, Path projectDir) {
        return isInsideDeskPilotRepoEngine(explicitOutFile) || isInsideDeskPilotRepoEngine(projectDir);
    }

    private RecorderCLI() {
    }
}
