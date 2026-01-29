package com.example;

import io.deskpilot.testkit.BaseDeskPilotTestJUnit5;
import org.junit.jupiter.api.Test;

public class RecordedTest extends BaseDeskPilotTestJUnit5 {

  @Test
  void recorded_flow() throws Exception {
    session().step("recorded_flow", () -> {
      // TODO CLICK: click
      // TODO FILL: fill_this_is_a_recorded_test = this is a recorded test
    });
  }
}
