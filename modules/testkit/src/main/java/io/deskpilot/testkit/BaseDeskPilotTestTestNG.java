package io.deskpilot.testkit;

import io.deskpilot.engine.DeskPilot;
import io.deskpilot.engine.DeskPilotSession;
import io.deskpilot.engine.RunOptions;
import io.deskpilot.engine.actions.Actions;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.nio.file.Path;

/**
 * Base class for DeskPilot TestNG tests.
 *
 * Contract:
 * - Uses the single blessed attach entrypoint (DeskPilot.attachPickWindow).
 * - Creates deterministic run folder names: <class>/<method>.
 * - Always closes the session in @AfterMethod so artifacts are finalized even on failures.
 */
public abstract class BaseDeskPilotTestTestNG {

    private DeskPilotSession session;
    private Actions actions;
    private Path runFolder;

    protected RunOptions.Builder runOptions() {
        return RunOptions.builder();
    }

    protected Path runsDir() {
        return RunOptions.DEFAULT_RUNS_DIR;
    }

    @BeforeMethod(alwaysRun = true)
    final void deskpilot_beforeMethod(ITestResult result) throws Exception {
        String className = result.getTestClass() != null
                ? result.getTestClass().getRealClass().getSimpleName()
                : "Test";
        String methodName = result.getMethod() != null
                ? result.getMethod().getMethodName()
                : "unknown";
        String runName = className + "/" + methodName;

        RunOptions opts = runOptions()
                .runsDir(runsDir())
                .runName(runName)
                .build();

        this.session = DeskPilot.attachPickWindow(opts);
        this.actions = new Actions(session);
        this.runFolder = opts.runDir();
    }

    @AfterMethod(alwaysRun = true)
    final void deskpilot_afterMethod() {
        if (session != null) {
            try { session.close(); } catch (Exception ignored) {}
        }
        session = null;
        actions = null;
        runFolder = null;
    }

    protected final DeskPilotSession session() {
        if (session == null) throw new IllegalStateException("session is null (not attached)");
        return session;
    }

    protected final Actions actions() {
        if (actions == null) throw new IllegalStateException("actions is null (not attached)");
        return actions;
    }

    Path _deskpilotRunFolder() {
        return runFolder;
    }
}
