## DeskPilot v0.1.3

This release focuses on **solid real-world usage**: dependency-only adoption + recorder output that writes into the user’s Maven project (no DeskPilot repo clone required).

### Highlights
- **Dependency-only adoption**: users can add io.deskpilot:testkit:0.1.3 and run tests with Maven.
- **Recorder writes into user project**: deskpilot record generates a runnable test under src/test/java/<package>/generated.
- Recorder commands:
  - **C** click (captures region → normalized locator)
  - **F** fill (captures region + value)
  - **W** wait (OCR contains)
  - **K** hotkey (e.g., CTRL+A)
  - **P** press (e.g., ENTER/DELETE)
  - **T** type text
- **Diagnostics-first artifacts**: each run produces step folders with BEFORE/AFTER screenshots + OCR/template debug outputs.

### Assets
- deskpilot.jar (CLI)
- deskpilot-dist.zip (distribution zip)

### Notes
- deskpilotw.cmd downloads the matching CLI jar from GitHub Releases based on deskpilot.properties (deskpilot.version).
