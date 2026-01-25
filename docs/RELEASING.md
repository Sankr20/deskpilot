# Releasing DeskPilot

This repo ships a Windows-friendly distribution zip produced by the `cli` module.

## Build the distribution

```bat
mvn -q -pl modules/cli -DskipTests package
```

Output:
- `modules\cli\target\deskpilot-dist.zip`

## Tagging (recommended)

DeskPilot uses tags like `v0.1.0`, `v0.1.1`, etc.

Suggested flow:
1. Update versions to the release number (remove `-SNAPSHOT`).
2. Commit: `chore(release): vX.Y.Z`
3. Tag: `git tag vX.Y.Z`
4. Create a GitHub release and upload `deskpilot-dist.zip` as the release asset.
5. (Optional) bump versions to the next `-SNAPSHOT` and commit.

## First release in this repo

For the “clean first commit” approach:
- Make the initial commit at `0.1.0` and tag it as `v0.1.0`.
