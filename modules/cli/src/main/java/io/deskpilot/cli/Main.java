package io.deskpilot.cli;

import io.deskpilot.demo.DemoCompositeActionsSmoke;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.Arrays;

public final class Main {

    public static void main(String[] args) throws Exception {
        int code = run(args);

        // IMPORTANT: no System.exit here (so mvn exec:java won't complain)
        if (code != 0) throw new RuntimeException("deskpilot exited with code " + code);
    }

    static int run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return 2;
        }

        return switch (args[0].toLowerCase()) {
            case "doctor" -> { doctor(); yield 0; }
            case "init" -> { new InitCommand().run(slice(args)); yield 0; }
            case "record" -> { RecorderCLI.recordToFile(slice(args)); yield 0; }
            case "run" -> runTest(args);
            case "smoke" -> smoke(args);
            default -> {
                System.err.println("Unknown command: " + args[0]);
                usage();
                yield 2;
            }
        };
    }

    private static void doctor() {
        System.out.println("DeskPilot doctor");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OK");
    }

    private static int runTest(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: deskpilot run <fully.qualified.TestClass>");
            return 2;
        }

        LauncherDiscoveryRequest request =
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(args[1]))
                        .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();

        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new java.io.PrintWriter(System.out));

        return summary.getFailures().isEmpty() ? 0 : 1;
    }

    private static int smoke(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage:\n    deskpilot smoke demo");
            return 2;
        }

        String target = args[1].toLowerCase();
        try {
            switch (target) {
                case "demo" -> {
                    System.err.println("[DEBUG] DemoCompositeActionsSmoke loaded from: " +
                            DemoCompositeActionsSmoke.class.getProtectionDomain()
                                    .getCodeSource().getLocation());
                    DemoCompositeActionsSmoke.main(new String[0]);
                }
                default -> {
                    System.err.println("Unknown smoke target: " + args[1]);
                    System.err.println("Available:\n    deskpilot smoke demo");
                    return 2;
                }
            }
            return 0;
        } catch (Throwable t) {
            System.err.println("❌ SMOKE FAILED: " + t.getMessage());
            t.printStackTrace(System.err);
            return 1;
        }
    }

    private static void usage() {
        System.out.println(
                "DeskPilot CLI\n" +
                "    deskpilot doctor\n" +
                "    deskpilot record\n" +
                "    deskpilot run <TestClass>\n" +
                "    deskpilot init <dir> <junit5|testng>\n" +
                "    deskpilot smoke demo"
        );
    }

    private static String[] slice(String[] args) {
        return args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
    }

    private Main() {}
}
