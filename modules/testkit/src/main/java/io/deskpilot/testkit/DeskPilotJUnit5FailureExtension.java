package io.deskpilot.testkit;

import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DeskPilotJUnit5FailureExtension implements AfterTestExecutionCallback {

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        var exOpt = context.getExecutionException();
        if (exOpt.isEmpty()) return;

        Throwable t = exOpt.get();

        Object instance = context.getRequiredTestInstance();
        if (!(instance instanceof BaseDeskPilotTestJUnit5 base)) {
            return; // not our base class, ignore
        }

        Path runFolder = base._deskpilotRunFolder();
        if (runFolder == null) return;

        Path failureDir = runFolder.resolve("99-failure");
        Files.createDirectories(failureDir);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();

        Files.writeString(
                failureDir.resolve("failed.txt"),
                sw.toString(),
                StandardCharsets.UTF_8
        );
    }
}
