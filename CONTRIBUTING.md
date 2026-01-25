# Contributing

Thanks for helping improve DeskPilot.

## Dev prerequisites
- Windows 10/11
- Java 17+
- Maven 3.9+

## Build + test

```bat
mvn -q test
```

## Build the CLI jar + dist zip

```bat
mvn -q -pl modules/cli -DskipTests package
```

Artifacts:
- CLI jar: `modules\cli\target\deskpilot.jar`
- Release zip: `modules\cli\target\deskpilot-dist.zip`

## Run locally (from source)

After building the CLI jar, you can run the CLI from the repo root:

```bat
deskpilot.cmd doctor
deskpilot.cmd smoke demo
deskpilot.cmd record
```

Notes:
- The **repo-root** `deskpilot.cmd` points to `modules\cli\target\deskpilot.jar` (for developer runs).
- End users should use the `deskpilot.cmd` that ships inside the release zip (from `dist/`).

## Guidelines
- Keep DeskPilot **application-agnostic** (no app-specific logic baked into the engine).
- Prefer changes that improve **stability + diagnostics** (step artifacts, near-miss dumps, etc.).
- Add/adjust tests where reasonable.

## Submitting changes
1. Create a branch.
2. Run `mvn -q test`.
3. Open a PR with a clear description + screenshots/logs if applicable.
