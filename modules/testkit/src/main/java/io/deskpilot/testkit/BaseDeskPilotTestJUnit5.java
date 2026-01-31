package io.deskpilot.testkit;

import io.deskpilot.engine.DeskPilot;
import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.RunOptions;
import io.deskpilot.engine.actions.Actions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.extension.ExtendWith;
import java.nio.file.Path;

/**
 * Base class for DeskPilot JUnit 5 tests.
 *
 * Contract:
 * - Uses the single blessed attach entrypoint (DeskPilot.attachPickWindow).
 * - Creates deterministic run folder names: <class>/<method>.
 * - Always closes the session in @AfterEach so artifacts are finalized even on failures.
 */
@ExtendWith(DeskPilotJUnit5FailureExtension.class)
public abstract class BaseDeskPilotTestJUnit5 {

    private DeskPilotSession session;
    private Actions actions;
    private java.nio.file.Path runFolder;
private Throwable testFailure;

    /**
     * Override to customize run options (e.g., step screenshots, stability flags).
     */
    protected RunOptions.Builder runOptions() {
        return RunOptions.builder();
    }

    @RegisterExtension
final TestWatcher deskpilot_watcher = new TestWatcher() {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        testFailure = cause;
    }
};


    /**
     * Override to customize the runs directory (default: ./runs).
     */
    protected Path runsDir() {
        return RunOptions.DEFAULT_RUNS_DIR;
    }

@BeforeEach
final void deskpilot_beforeEach(TestInfo info) throws Exception {
    String className = info.getTestClass().map(Class::getSimpleName).orElse("Test");
    String methodName = info.getTestMethod().map(m -> m.getName()).orElse("unknown");
    String runName = className + "/" + methodName;

    RunOptions opts = runOptions()
            .runsDir(runsDir())
            .runName(runName)
            .build();

    // ✅ ALWAYS create folder first so failures have a home
    this.runFolder = RunOptions.prepareRunFolder(opts);

    try {
        this.session = DeskPilot.attachPickWindow(opts);
        this.actions = new Actions(session);
    } catch (Exception e) {
        Path startup = runFolder.resolve("01-startup");
        try {
            java.nio.file.Files.writeString(startup.resolve("attach_error.txt"), stackTrace(e));
        } catch (Exception ignored) {}
        throw e;
    }
}

@AfterEach
final void deskpilot_afterEach() {
    if (session != null) {
        try { session.close(); } catch (Exception ignored) {}
    }
    session = null;
    actions = null;
    runFolder = null;
    testFailure = null;
}


    protected final DeskPilotSession session() {
        if (session == null) throw new IllegalStateException("session is null (not attached)");
        return session;
    }

    protected final Actions actions() {
        if (actions == null) throw new IllegalStateException("actions is null (not attached)");
        return actions;
    }

    
java.nio.file.Path _deskpilotRunFolder() {
    return runFolder;
}

private static String stackTrace(Throwable t) {
    java.io.StringWriter sw = new java.io.StringWriter();
    java.io.PrintWriter pw = new java.io.PrintWriter(sw);
    t.printStackTrace(pw);
    pw.flush();
    return sw.toString();
}

}
