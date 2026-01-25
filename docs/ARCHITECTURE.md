# Architecture (high level)

DeskPilot is split into three Maven modules:

- `engine`: core automation engine
- `cli`: command-line interface + packaging
- `demo-app`: small Windows demo app used for smoke flows

## Core concepts

### Targets and locators
DeskPilot captures UI intent using targets/locators:

- **POINT** — click/type location (normalized coordinates)
- **REGION** — rectangle region (normalized)
- **TEMPLATE** — image template match (optional variants)
- **OCR_CONTAINS** — region + expected text snippet

These are stored in registry-style classes (`UiMap`, `UiRegions`, `UiTemplates`, `Locators`) with safe AUTOGEN sections for recorder output.

### Sessions, stabilization, and artifacts
A `DeskPilotSession` drives steps:

- “before/after” screenshots per step
- UI stabilization/waits where needed
- failure artifacts (template/OCR diagnostics)

Artifacts are written under `runs/<run-name>/<step>/...`.
