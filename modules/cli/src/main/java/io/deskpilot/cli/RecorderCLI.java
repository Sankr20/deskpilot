package io.deskpilot.cli;

import io.deskpilot.engine.DeskPilot;
import io.deskpilot.engine.RunOptions;
import io.deskpilot.recorder.RecorderManager;
import io.deskpilot.recorder.TestClassGenerator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;

public class RecorderCLI {

    public static void recordToFile(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: deskpilot record <output file>");
            return;
        }

        Path out = Path.of(args[0]);

        var opts = RunOptions.builder()
                .runName("recording-run")
                .build();

        var session = DeskPilot.attachPickWindow(opts);
        var recorder = new RecorderManager(session);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        printHelp();

        while (true) {
    System.out.print("> ");
    String cmd = br.readLine();
    if (cmd == null) break;
    cmd = cmd.trim();

    if (cmd.isEmpty()) break;

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

    System.out.println("Unknown command. Use C, F, HELP, or ENTER.");
}


        var actions = recorder.getActions();
        System.out.println("Actions recorded: " + actions.size());
        TestClassGenerator.generateJUnit5("com.example", "RecordedTest", actions, out);
        System.out.println("Recorded test written to " + out);
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Recorder ready.");
        System.out.println("Commands:");
        System.out.println("  C     = capture CLICK region");
        System.out.println("  F     = capture FILL region + value");
        System.out.println("  HELP  = show commands");
        System.out.println("  ENTER = stop and write test");
        System.out.println();
    }
}
