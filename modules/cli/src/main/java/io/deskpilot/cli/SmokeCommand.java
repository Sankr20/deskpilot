package io.deskpilot.cli;

import io.deskpilot.demo.DemoCompositeActionsSmoke;

import java.util.Arrays;

public final class SmokeCommand {

    /**
     * Usage:
     *   deskpilot smoke demo [--debug]
     *
     * Return:
     *   0 success, 1 failure, 2 usage
     */
    public int run(String[] args) throws Exception {
        if (args.length < 1) {
            usage();
            return 2;
        }

        boolean debug = Arrays.stream(args).anyMatch(a -> a.equalsIgnoreCase("--debug"));
        String target = args[0].toLowerCase();

        try {
            switch (target) {
                case "demo" -> {
                    if (debug) {
                        System.err.println("[DEBUG] DemoCompositeActionsSmoke loaded from: " +
                                DemoCompositeActionsSmoke.class.getProtectionDomain()
                                        .getCodeSource().getLocation());
                    }
                    DemoCompositeActionsSmoke.main(new String[0]);
                }
                default -> {
                    System.err.println("Unknown smoke target: " + args[0]);
                    usage();
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
        System.err.println("Usage:\n    deskpilot smoke demo [--debug]");
    }
}
