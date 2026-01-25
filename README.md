# DeskPilot

DeskPilot is a **Windows desktop automation tool** (think “Playwright-style” ergonomics, but for native apps).
It’s designed to be **application-agnostic**: you define locators (points/regions/templates/OCR checks) and run reliable, step-artifacted automations across different screen sizes and DPI setups.

> Status: **v0.1.0** (first public release). Windows + Java 17+.

## What you can do today

- **Run a smoke demo** against the included DeskPilot Demo App
- **Record locators** (points/regions/templates) and generate a runnable test class
- Use a stable **Actions API** (`click`, `fill`, `paste`, `waitFor`, etc.) backed by step screenshots + diagnostics
- Verify UI text via **OCR contains** locators (with preprocessing + failure artifacts)

## Quick start (from a release zip)

1) Install **Java 17+**.

2) Download the latest `deskpilot-dist.zip` from Releases and extract it.

3) From the extracted folder:

```bat
deskpilot doctor
deskpilot smoke demo
deskpilot record
```

4) Run a generated test:

```bat
deskpilot run io.deskpilot.tests.generated.<YourTestClass>
```

## Build from source

Requirements: **Windows**, **Java 17+**, **Maven 3.9+**.

```bat
mvn -q test
mvn -q -pl modules/cli -DskipTests package
```

Then you can run the CLI via:

```bat
deskpilot.cmd doctor
```

## Repository layout

- `modules/engine` — core engine (window selection, coordinate normalization, template matching, OCR, artifacts)
- `modules/cli` — DeskPilot CLI and distribution packaging
- `modules/demo-app` — a small Windows demo app used by smoke tests
- `dist/` — release zip content (launcher + README used by the assembly)

## License

MIT (see `LICENSE`).

## Contributing

See `CONTRIBUTING.md`.
