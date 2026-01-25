# Release process

This repo uses tags like `v0.1.0` and a distribution zip produced by the CLI module.

## Build the release artifacts

```bat
mvn -q -pl modules/cli -DskipTests package
```

Upload `modules\cli\target\deskpilot-dist.zip` as the release asset.

## Tagging

- For the first release, set POM versions to the release version (e.g., `0.1.0`) and tag `v0.1.0`.
- After tagging, bump to the next `-SNAPSHOT` for ongoing development.
