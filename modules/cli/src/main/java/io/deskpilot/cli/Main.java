package io.deskpilot.cli;

import io.deskpilot.demo.DemoCompositeActionsSmoke;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;

public final class Main {

    public static void main(String[] args) throws Exception {
        int code = run(args);

        // IMPORTANT: no System.exit here (so mvn exec:java won't complain)
        if (code != 0) {
            throw new RuntimeException("deskpilot exited with code " + code);
        }
    }

    static int run(String[] args) throws Exception {
        if (args == null) args = new String[0];

        // ---------- global flags ----------
        if (args.length == 0 || isHelp(args[0])) {
            printGlobalHelp();
            return 0;
        }

        if (args.length == 1 && isVersion(args[0])) {
            printVersion();
            return 0;
        }

        String cmd = lower(args[0]);

        return switch (cmd) {
    case "doctor" -> {
        doctor();
        yield 0;
    }
    case "init" -> {
        yield new InitCommand().run(slice(args));
    }
    case "record" -> {
        yield RecorderCLI.recordToFile(slice(args));
    }
    case "run" -> {
        yield runTest(slice(args));
    }
    case "smoke" -> {
        yield smoke(slice(args));
    }
    default -> {
        System.err.println("Unknown command: " + args[0]);
        System.err.println("Run: deskpilot --help");
        System.err.println();
        printGlobalHelp();
        yield 2;
    }
};



    }

    // ---------- commands ----------

    private static void doctor() {
        System.out.println("DeskPilot doctor");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OK");
    }

    private static int runTest(String[] args) {
        if (args.length == 0 || isHelp(args[0])) {
            printRunHelp();
            return 0;
        }

        String testClass = args[0].trim();
        if (testClass.isEmpty()) {
            System.err.println("[ERROR] Missing test class.");
            printRunHelp();
            return 2;
        }

        LauncherDiscoveryRequest request =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(testClass))
                        .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        return summary.getFailures().isEmpty() ? 0 : 1;
    }

    private static int smoke(String[] args) throws Exception {
        if (args.length == 0 || isHelp(args[0])) {
            printSmokeHelp();
            return 0;
        }

        String target = lower(args[0]);

        try {
            return switch (target) {
                case "demo" -> {
                    System.err.println("[DEBUG] DemoCompositeActionsSmoke loaded from: " +
                            DemoCompositeActionsSmoke.class
                                    .getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation());
                    DemoCompositeActionsSmoke.main(new String[0]);
                    yield 0;
                }
                default -> {
                    System.err.println("Unknown smoke target: " + args[0]);
                    System.err.println();
                    printSmokeHelp();
                    yield 2;
                }
            };
        } catch (Throwable t) {
            System.err.println("❌ SMOKE FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            return 1;
        }
    }

    // ---------- help / version ----------

    private static void printGlobalHelp() {
        System.out.println("DeskPilot CLI");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  deskpilot --help");
        System.out.println("  deskpilot --version");
        System.out.println("  deskpilot doctor");
        System.out.println("  deskpilot init <dir> <junit5|testng>");
        System.out.println("  deskpilot record [--framework junit5|testng] [--projectDir <dir>] [--force] [<outputFile>]");
        System.out.println("  deskpilot run <fully.qualified.TestClass>");
        System.out.println("  deskpilot smoke demo");
        System.out.println();
    }

    private static void printRunHelp() {
        System.out.println("Run tests");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  deskpilot run <fully.qualified.TestClass>");
        System.out.println();
    }

    private static void printSmokeHelp() {
        System.out.println("Smoke tests");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  deskpilot smoke demo");
        System.out.println();
    }

    private static void printVersion() {
        String v = Main.class.getPackage().getImplementationVersion();
        if (v == null || v.isBlank()) v = "dev";
        System.out.println("deskpilot " + v);
    }

    // ---------- utils ----------

    static boolean isHelp(String s) {
        if (s == null) return false;
        String a = s.trim().toLowerCase(Locale.ROOT);
        return a.equals("help")
                || a.equals("--help")
                || a.equals("-h")
                || a.equals("/?")
                || a.equals("-help");
    }

    private static boolean isVersion(String s) {
        if (s == null) return false;
        String a = s.trim().toLowerCase(Locale.ROOT);
        return a.equals("--version") || a.equals("-v");
    }

    private static String lower(String s) {
        return (s == null) ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String[] slice(String[] args) {
        return args.length <= 1
                ? new String[0]
                : Arrays.copyOfRange(args, 1, args.length);
    }

    private Main() {}
}
