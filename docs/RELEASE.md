# Release process

DeskPilot uses Maven for builds and the Maven Assembly Plugin to create the distribution zip.

## Build a release zip

```bat
mvn -q clean test
mvn -q -pl modules/cli -DskipTests package
```

Output:
- `modules/cli/target/deskpilot-dist.zip`

## Tagging

Recommended flow:
1. Set project version to a non-SNAPSHOT (e.g., `0.1.0`).
2. Commit with message like `Release 0.1.0`.
3. Create an annotated tag: `v0.1.0`.
4. (Optional) bump to next `-SNAPSHOT` on `main`.
