# DeskPilot Security & Risk Analysis

DeskPilot automates Windows desktop apps using screenshots/regions, simulated input (click/paste/type), OCR, and artifact logging.
It is diagnostics-first: it intentionally captures evidence to help debug failures.

This document describes the security posture, risks, and mitigations.

---

## 1. Assets to protect

**User system safety**
- Avoid unintended clicks/keystrokes into the wrong app/window.
- Avoid disruptive runaway loops or huge disk usage.

**Data confidentiality**
- Screenshots may contain PII/PHI/client names/financial data.
- OCR text dumps may contain sensitive strings.
- Clipboard-based paste may expose secrets to other apps.

**File system integrity**
- Prevent writing artifacts/tests outside intended directories (path traversal).
- Avoid overwriting user files unless explicitly requested.

**Supply chain integrity**
- Maven dependencies and native/interop libraries (JNA, tess4j, tesseract data).

---

## 2. Threat model (what we assume)

- DeskPilot runs locally under the user’s account.
- The user selects the target window by clicking.
- DeskPilot is not a sandbox and cannot safely run untrusted scripts/tests.
- If you run DeskPilot with elevated privileges (Admin), it can interact with elevated apps—treat that as high-risk.

---

## 3. Attack surfaces

1. **Window selection / focus**
   - Attaching to the wrong window or losing focus mid-step.

2. **Simulated input**
   - Mouse clicks and keyboard shortcuts can have destructive effects.

3. **OCR and screenshots**
   - Saved artifacts may leak sensitive data.

4. **Recorder output**
   - Writes Java files to disk; risk of writing outside intended root.

5. **CLI paths**
   - `--projectDir` and output file paths can be used to write into arbitrary locations.

6. **Clipboard**
   - Fill/paste uses clipboard; other apps can read clipboard contents.

7. **Dependencies**
   - Maven supply chain, OCR engine, native bindings.

---

## 4. Risk register

| Risk | Likelihood | Impact | Example | Mitigation (current) | Mitigation (planned) |
|---|---:|---:|---|---|---|
| Wrong window attached | Medium | High | Keystrokes into prod tool | Explicit “click to attach”, bring-to-front option | Strong “verify window title/process” checks |
| Focus stolen mid-run | Medium | High | Clicks go elsewhere | Stabilizers, diagnostics on failure | Optional “always bring-to-front before step” |
| Sensitive artifacts on disk | High | High | Screenshots include client data | Runs folder isolation, deterministic artifacts | “Privacy mode” to suppress OCR text + optional screenshot reduction |
| Path traversal / writing outside project | Medium | High | `--projectDir C:\` | Safe path utilities + validation | Stricter default: refuse outside CWD unless forced |
| Overwrite user files | Medium | High | output file collisions | `--force` required for overwrite | Additional “directory non-empty” checks |
| Clipboard leakage | Medium | Medium | Password copied into clipboard | Prefer non-secret data | Optional “type mode” for secrets + clipboard restore |
| Disk exhaustion | Low–Med | Medium | Massive runs/artifacts | Step folders + safe filenames | Global artifact size caps / retention |
| Dependency compromise | Low | High | malicious transitive dep | Pinned versions + release tags | SBOM + dependency scanning |

---

## 5. Safe-by-default decisions

- Refuse overwrites unless `--force`.
- Prefer writing under repository `runs/` and under provided `--projectDir`.
- Recorder refuses to generate a test when no actions were recorded.
- OCR locators use “contains” checks to tolerate OCR noise.

---

## 6. Recommended operational guidance

- Run DeskPilot only on test environments or non-production data where possible.
- Avoid recording sensitive screens.
- Treat run artifacts as sensitive; do not share them publicly without review/redaction.
- Do not run DeskPilot as Admin unless required.
- Use stable OCR tokens (prefixes) and sufficiently large OCR regions to avoid clipped characters.

---

## 7. Reporting vulnerabilities

If you discover a security issue:
- Provide repro steps and affected version/tag.
- Avoid publishing proof-of-concepts that could harm users.
