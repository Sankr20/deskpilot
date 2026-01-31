package io.deskpilot.cli;

public final class CliEntrypoint {
    public static void main(String[] args) throws Exception {
        int code = Main.run(args);
        System.exit(code);
    }
    private CliEntrypoint() {}
}
