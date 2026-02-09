package com.myco.tests.generated;

import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;
import io.deskpilot.engine.NormalizedRegion;
import io.deskpilot.engine.Locators;
import org.junit.jupiter.api.Test;
import java.time.Duration;

public class RecordedTest_20260209_091008Test extends BaseDeskPilotTestJUnit5 {

  @Test
  void recorded_flow() throws Exception {
    session().step("recorded_flow", () -> {

      final Duration WAIT_TIMEOUT = Duration.ofSeconds(5);

      var r01 = new NormalizedRegion(0.008854, 0.148077, 0.051042, 0.033654);
      actions().click(Locators.point("rec_click_01", r01.xPct + (r01.wPct/2.0), r01.yPct + (r01.hPct/2.0)));

    });
  }
}
