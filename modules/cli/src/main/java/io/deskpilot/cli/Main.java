package io.deskpilot.cli;

import io.deskpilot.engine.EngineRecordMode;
import io.deskpilot.engine.EngineActionSmoke;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.Arrays;


import java.util.Arrays;

public final class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(2);
        }

        switch (args[0].toLowerCase()) {
            case "doctor" -> doctor();
            case "record" -> EngineRecordMode.main(slice(args));
            case "run" -> runTest(args);
            case "smoke" -> smoke(args);
            default -> {
                System.err.println("Unknown command: " + args[0]);
                usage();
                System.exit(2);
            }
        }
    }

    private static void doctor() {
        System.out.println("DeskPilot doctor");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("OK");
    }

    private static void runTest(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: deskpilot run <fully.qualified.TestClass>");
            System.exit(2);
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

        System.exit(summary.getFailures().isEmpty() ? 0 : 1);
    }

    private static void smoke(String[] args) throws Exception {
        if (args.length < 2 || !"demo".equalsIgnoreCase(args[1])) {
            System.err.println("Usage: deskpilot smoke demo");
            System.exit(2);
        }
        EngineActionSmoke.main(new String[0]);
    }

    private static void usage() {
        System.out.println("""
            DeskPilot CLI

            deskpilot doctor
            deskpilot record
            deskpilot run <TestClass>
            deskpilot smoke demo
            """);
    }

    private static String[] slice(String[] args) {
        return args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
    }
}
