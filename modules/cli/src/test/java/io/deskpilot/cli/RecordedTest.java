package io.deskpilot.cli;

import io.deskpilot.engine.Locators;
import io.deskpilot.engine.NormalizedRegion;
import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;


@Disabled("Interactive UI test - run manually")
public class RecordedTest extends BaseDeskPilotTestJUnit5 {

    @Test
    void recorded_flow() throws Exception {
        session().step("recorded_flow", () -> {

                      var r1 = new NormalizedRegion(0.002604, 0.004955, 0.150000, 0.042616);
            actions().fill(Locators.point("rec_fill_02",
                    r1.xPct + (r1.wPct / 2.0),
                    r1.yPct + (r1.hPct / 2.0)),
                    "this is an automated test");

            var r2 = new NormalizedRegion(0.953646, 0.007929, 0.043229, 0.037661);
            actions().click(Locators.point("rec_click_01",
                    r2.xPct + (r2.wPct / 2.0),
                    r2.yPct + (r2.hPct / 2.0)));


        });
    }
}
