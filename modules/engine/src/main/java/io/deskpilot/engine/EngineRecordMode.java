package io.deskpilot.engine;

import io.deskpilot.engine.recording.RecorderPolicy;
import io.deskpilot.engine.recording.RecorderValidation;
import io.deskpilot.engine.recording.RegistryIndex;
import io.deskpilot.engine.recording.ValidationResult;
import io.deskpilot.engine.locators.LocatorKind;
import io.deskpilot.engine.recorder.RecordedStep;
import io.deskpilot.engine.recorder.RecordedTestWriter;
import io.deskpilot.engine.targets.TemplateTarget;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class EngineRecordMode {

    private static final class RunStats {
        int addedPoints;
        int addedRegions;
        int addedTemplates;
        int addedVariants;
        int overwrites;
        int warns;
        int rejects;

        final LinkedHashMap<String, Integer> warnReasons = new LinkedHashMap<>();
        final LinkedHashMap<String, Integer> rejectReasons = new LinkedHashMap<>();

        void warn(String reason) {
            warns++;
            bump(warnReasons, reason);
        }

        void reject(String reason) {
            rejects++;
            bump(rejectReasons, reason);
        }

        private static void bump(LinkedHashMap<String, Integer> m, String k) {
            if (k == null || k.isBlank()) k = "other";
            m.put(k, m.getOrDefault(k, 0) + 1);
        }

        String summaryLine() {
            return "added: points=" + addedPoints
                    + ", regions=" + addedRegions
                    + ", templates=" + addedTemplates
                    + ", variants=" + addedVariants
                    + " | overwrites=" + overwrites
                    + " | warns=" + warns
                    + " | rejects=" + rejects;
        }
    }

    public static void main(String[] args) throws Exception {

        // 13B: registry index + knobs
      io.deskpilot.engine.recording.RegistryIndex index =
                io.deskpilot.engine.recording.RegistryIndex.load(
                        io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout()
                );

        boolean allowOverwrite = Boolean.getBoolean("deskpilot.record.allowOverwrite");
        boolean rejectNearDup = Boolean.getBoolean("deskpilot.record.rejectNearDuplicate");
        double eps = Double.parseDouble(System.getProperty("deskpilot.record.nearDupEps", "0.003"));

        // 13C: IoU knob (used for regions only)
        double minIou = Double.parseDouble(System.getProperty("deskpilot.record.nearDupMinIou", "0.85"));

        boolean requireOverwriteConfirm = Boolean.getBoolean("deskpilot.record.requireOverwriteConfirm");
        boolean overwriteConfirm = Boolean.getBoolean("deskpilot.record.overwriteConfirm");

        // ✅ IMPORTANT: after "step-scoped artifacts", do NOT call before() outside step()
       try(DeskPilotSession s = DeskPilotSession.attachPickWindow("record-mode")){
        
        RecorderValidation validator = new RecorderValidation(RecorderPolicy.defaults());
        RecorderPolicy pol = RecorderPolicy.defaults();

        RunStats stats = new RunStats();

        // ✅ 16.1: accumulate steps to generate a runnable JUnit test
        List<RecordedStep> recordedSteps = new ArrayList<>();

        // 13I: startup banner for knobs
        var paths = io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout();
        System.out.println();
        System.out.println("=== DeskPilot Record Mode ===");
        System.out.println("Knobs:");
        System.out.println("  allowOverwrite=" + allowOverwrite
                + " (requireConfirm=" + requireOverwriteConfirm + ", overwriteConfirm=" + overwriteConfirm + ")");
        System.out.println("  rejectNearDuplicate=" + rejectNearDup);
        System.out.println("  nearDupEps=" + eps);
        System.out.println("  nearDupMinIou=" + minIou);
        System.out.println("  minRegionPx=" + pol.minRegionPxW + "x" + pol.minRegionPxH);
        System.out.println("  minTemplatePx=" + pol.minTemplatePxW + "x" + pol.minTemplatePxH);
        System.out.println("Paths:");
        System.out.println("  resourcesBase=modules/engine/src/main/resources");
        System.out.println("  uiMap=" + paths.uiMap());
        System.out.println("  uiRegions=" + paths.uiRegions());
        System.out.println("  uiTemplates=" + paths.uiTemplates());
        System.out.println("  locators=" + paths.locators());

        System.out.println("Choose mode each time:");
        System.out.println("  P = record POINT (UiTarget) (capture + saves locator)");
        System.out.println("  R = record REGION (NormalizedRegion) (capture + saves locator)");
        System.out.println("  T = record TEMPLATE (TemplateTarget) (capture + saves locator)");
        System.out.println("  V = add TEMPLATE VARIANT (append png to existing TemplateTarget)");
        System.out.println("  C = record CLICK step (const only, no capture, adds step)");
        System.out.println("  F = record FILL step (field const + text, no capture, adds step)");
        System.out.println("  W = record WAIT step (OCR locator const + timeout, no capture, adds step)");
        System.out.println("Type 'exit' anytime to quit.");
        System.out.println();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("Mode (P/R/T/V/C/F/W) + ENTER (or 'exit'): ");
            String mode = br.readLine();
            if (mode == null) break;

            mode = mode.trim();

            // ✅ on exit, generate JUnit test if we have steps
            if (mode.equalsIgnoreCase("exit")) {
                if (!recordedSteps.isEmpty()) {
                    String className = RecordedTestWriter.defaultClassName();
                    String methodName = RecordedTestWriter.defaultMethodName();

                    boolean forceWriteTest = allowOverwrite && (!requireOverwriteConfirm || overwriteConfirm);

Path out = RecordedTestWriter.writeTest(
        className,
        methodName,
        "recorded-flow",
        recordedSteps,
        forceWriteTest
);

                    System.out.println("[OK] Generated test: " + out.toAbsolutePath());
                    System.out.println("Class: " + className);
System.out.println("Steps: " + recordedSteps.size());
for (int i = 0; i < Math.min(3, recordedSteps.size()); i++) {
    System.out.println("  - " + recordedSteps.get(i));
}
                    System.out.println("Run: mvn -pl modules/engine test");
                } else {
                    System.out.println("No steps recorded. Exiting.");
                }
                break;
            }

            if (mode.isEmpty()) continue;

            // ✅ FIX: include C in allowed modes
            if (!(mode.equalsIgnoreCase("P") || mode.equalsIgnoreCase("R")
                    || mode.equalsIgnoreCase("T") || mode.equalsIgnoreCase("V")
                    || mode.equalsIgnoreCase("C") || mode.equalsIgnoreCase("F")
                    || mode.equalsIgnoreCase("W"))) {
                System.out.println("Invalid mode. Use P, R, T, V, C, F, or W.\n");
                continue;
            }


// -------------------------
// C = CLICK (no capture)
// -------------------------
if (mode.equalsIgnoreCase("C")) {
    String locatorConst = readConstOrExit(br,
            "Locator const to click (e.g., POINT_BUTTON_SEARCH or Locators.POINT_BUTTON_SEARCH): ");
    if (locatorConst == null) break;
    if (locatorConst.equals("exit")) break;
    if (locatorConst.isEmpty()) continue;

if (!index.existsAnyLocatorConst(locatorConst)) {
    System.out.println("[REJECT] Unknown locator const: " + locatorConst);
    printSuggestionHintAny(index, locatorConst);   // 👈 change helper name
    stats.reject("unknown_locator");
    System.out.println();
    continue;
}


    recordedSteps.add(new RecordedStep.Click(locatorConst));
    System.out.println("Recorded step: click " + locatorConst + "\n");
    continue;
}


// -------------------------
// W = WAIT (OCR locator const + timeout)
// UX hardening:
// - allow user to type an OCR locator const OR a REGION const (then offer to create OCR locator)
// - friendly suggestions for OCR locators
// - timeout parsing: 5000, 5s, 2.5s, 800ms
// -------------------------
if (mode.equalsIgnoreCase("W")) {
    String input = readConstOrExit(br,
            "Wait for (OCR const OR REGION const) (e.g., STATUS_TEXT_SEARCHED or SEARCHSTATUSUPDATEREGION): ");
    if (input == null) break;
    if (input.equals("exit")) break;
    if (input.isEmpty()) continue;

    // 1) If it's already an OCR locator => just record WAIT
    if (index.existsOcrLocatorConst(input)) {
        long timeoutMs = readTimeoutMsOrDefault(br, 5000);
        if (timeoutMs < 0) break; // user typed exit or EOF
        recordedSteps.add(new RecordedStep.WaitForFound(input, timeoutMs));
        System.out.println("Recorded step: waitFor " + input + " (timeoutMs=" + timeoutMs + ")\n");
        continue;
    }

    // 2) If user typed a REGION constant by mistake, offer to create OCR locator
    if (index.existsRegionLocatorConst(input)) {
        System.out.println("[INFO] '" + input + "' looks like a REGION, but WAIT requires an OCR locator.");
        System.out.println("[INFO] I can create an OCR 'contains' locator in Locators.java for you.");

        String yn = readLineOrExit(br, "Create OCR locator now? (y/N): ");
        if (yn == null) break;
        if (yn.equalsIgnoreCase("exit")) break;

        if (yn.equalsIgnoreCase("y") || yn.equalsIgnoreCase("yes")) {

            // Need UiRegions const for ocrContains(..., UiRegions.X, ...)
           // We accept either UiRegions.X or just X (where X exists in UiRegions AUTOGEN)

String raw = input.trim();

String uiRegionConst;
if (raw.regionMatches(true, 0, "UiRegions.", 0, "UiRegions.".length())) {
    uiRegionConst = raw.substring("UiRegions.".length()).trim();
} else {
    uiRegionConst = raw;
}


// Must exist as a UiRegions const to safely create OCR locator
if (!index.existsUiRegionConst(uiRegionConst)) {
    System.out.println("[REJECT] For creation, please enter a UiRegions const (e.g., UiRegions.SEARCHSTATUSUPDATEREGION or SEARCHSTATUSUPDATEREGION).");
    System.out.println("[HINT] You entered: " + input);
    printUiRegionsHintList(index);     // ✅ optional but helpful
    stats.reject("need_uiregions_const");
    System.out.println();
    continue;
}


            String expected = readLineOrExit(br, "Expected text contains (e.g., searched): ");
            if (expected == null) break;
            if (expected.equalsIgnoreCase("exit")) break;
            expected = expected.trim();
            if (expected.isEmpty()) {
                System.out.println("[REJECT] expected text cannot be blank.\n");
                stats.reject("blank_expected");
                continue;
            }

            String rawName = readLineOrExit(br, "New OCR locator name (blank = auto): ");
            if (rawName == null) break;
            if (rawName.equalsIgnoreCase("exit")) break;

            String name;
            if (rawName.isBlank()) {
                // simple auto name: status_text_<expected> (sanitized)
                name = "status_text_" + expected.toLowerCase(java.util.Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "_")
                        .replaceAll("^_+|_+$", "");
            } else {
                name = rawName.trim();
            }

            // Write locator
            UiFileWriter.upsertLocatorOcrContains(name, uiRegionConst, expected);

            // reload index (so next checks/suggestions reflect it)
            index = io.deskpilot.engine.recording.RegistryIndex.load(
                    io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout()
            );

            String ocrConst = UiNaming.toConst(UiNaming.normalizeLabel(name, UiNaming.Kind.REGION));
            System.out.println("[SAVED] Locators OCR: " + ocrConst + " (region=" + uiRegionConst + ", contains=\"" + expected + "\")");

            long timeoutMs = readTimeoutMsOrDefault(br, 5000);
            if (timeoutMs < 0) break;

            recordedSteps.add(new RecordedStep.WaitForFound(ocrConst, timeoutMs));
            System.out.println("Recorded step: waitFor " + ocrConst + " (timeoutMs=" + timeoutMs + ")\n");
            continue;
        }

        // user said no
printSuggestionHintForKind(index, input, LocatorKind.OCR);
printOcrLocatorHintList(index);   // ✅ add here so Flow-3 prints it
stats.reject("not_ocr");
System.out.println();
continue;


    }

    // 3) Otherwise: unknown / wrong kind
    System.out.println("[REJECT] Not an OCR locator const: " + input);

    // More helpful: if they typed something that exists but is POINT/TEMPLATE, we can hint too.
    if (index.existsPointLocatorConst(input)) {
        System.out.println("[HINT] That looks like a POINT const. WAIT needs OCR (created with ocrContains).");
    } else if (index.existsTemplateLocatorConst(input)) {
        System.out.println("[HINT] That looks like a TEMPLATE const. WAIT needs OCR (created with ocrContains).");
    } else if (index.existsAnyLocatorConst(input)) {
        System.out.println("[HINT] That const exists, but it’s not OCR.");
    } else {
        System.out.println("[HINT] That const doesn't exist.");
    }

printSuggestionHintForKind(index, input, LocatorKind.OCR);
printOcrLocatorHintList(index);   // ✅ add here too
stats.reject("not_ocr");
System.out.println();
continue;


}




// -------------------------
// F = FILL (no capture)
// -------------------------
if (mode.equalsIgnoreCase("F")) {
    String fieldConst = readConstOrExit(br,
            "Field POINT const (e.g., POINT_INPUT_SEARCH or Locators.POINT_INPUT_SEARCH): ");
    if (fieldConst == null) break;
    if (fieldConst.equals("exit")) break;
    if (fieldConst.isEmpty()) continue;

    // ✅ typed validation (point)
if (!index.existsPointLocatorConst(fieldConst)) {
    System.out.println("[REJECT] Not a POINT locator/const: " + fieldConst);
    printSuggestionHintForKind(index, fieldConst, LocatorKind.POINT); // ✅
    stats.reject("wrong_kind");
    System.out.println();
    continue;
}


    System.out.print("Text to fill: ");
    String text = br.readLine();
    if (text == null) break;
    if (text.trim().equalsIgnoreCase("exit")) break;

    if (text.isEmpty()) {
    System.out.println("[WARN] Fill text is empty. This will still run, but usually you want non-empty text.");
    stats.warn("empty_fill_text");
}


    recordedSteps.add(new RecordedStep.Fill(fieldConst, text));
    System.out.println("Recorded step: fill " + fieldConst + " = \"" + text + "\"\n");
    continue;
}


            // -------------------------
            // For P/R/T/V we need a name
            // -------------------------
            System.out.print("Name: ");
            String rawName = br.readLine();
            if (rawName == null) break;

            rawName = rawName.trim();
            if (rawName.equalsIgnoreCase("exit")) break;
            if (rawName.isEmpty()) continue;

            final UiNaming.Kind kind = mode.equalsIgnoreCase("P")
                    ? UiNaming.Kind.POINT
                    : mode.equalsIgnoreCase("R")
                    ? UiNaming.Kind.REGION
                    : UiNaming.Kind.TEMPLATE; // T or V

            final String label = UiNaming.normalizeLabel(rawName, kind);
            final String constName = UiNaming.toConst(label);

            System.out.println("Normalized name: " + label + "  =>  " + constName);
            System.out.println();

            // -------------------------
            // P = POINT (capture + saves locator, NO step)
            // -------------------------
            if (mode.equalsIgnoreCase("P")) {

                boolean exists = index.existsPointConst(constName);
                if (exists) {
                    if (!allowOverwrite) {
                        System.out.println("[REJECT] Name already exists: " + constName);
                        System.out.println("[HINT] " + buildCollisionHint(constName, "point"));
                        stats.reject("collision");
                        System.out.println();
                        continue;
                    }

                    if (requireOverwriteConfirm && !overwriteConfirm) {
                        System.out.println("[REJECT] Overwrite requested but overwriteConfirm is not enabled for: " + constName);
                        System.out.println("[HINT] Re-run with -Ddeskpilot.record.overwriteConfirm=true (or disable requireOverwriteConfirm). "
                                + "This protects against accidental overwrites.");
                        stats.reject("overwrite_confirm");
                        System.out.println();
                        continue;
                    }

                    System.out.println("[OVERWRITE] Replacing existing " + constName + " in UiMap + Locators");
                    stats.overwrites++;
                }

                int pointDelayMs = Integer.getInteger("deskpilot.pointDelayMs", 3000);
                System.out.println("Move mouse to the target point... recording in " + (pointDelayMs / 1000.0) + " seconds");
                Thread.sleep(pointDelayMs);

                System.out.println("Recording point NOW...");

                // ✅ Step-scoped capture (writes artifacts)
                final UiTarget[] out = new UiTarget[1];
                s.step("record-point-" + label, () -> out[0] = s.recordTargetFromMouse(label));
                UiTarget t = out[0];

                var nearP = index.findNearDuplicatePoint(constName, t.xPct(), t.yPct(), eps);
                if (nearP.isPresent()) {
                    String msg = "Near-duplicate point of existing const: " + nearP.get() + " (eps=" + eps + ")";
                    if (rejectNearDup) {
                        System.out.println("[REJECT] " + msg);
                        System.out.println("[HINT] Rename it (e.g., add _alt), OR reduce eps strictness (-Ddeskpilot.record.nearDupEps=...), "
                                + "OR allow warn-only by leaving rejectNearDuplicate=false.");
                        stats.reject("near_dup_point");
                        System.out.println();
                        continue;
                    } else {
                        System.out.println("[WARN] " + msg);
                        System.out.println("[HINT] If this is intentional, consider naming it with a suffix like _alt. If accidental, re-record. "
                                + "You can also enforce rejection with -Ddeskpilot.record.rejectNearDuplicate=true.");
                        stats.warn("near_dup_point");
                    }
                }

                UiFileWriter.upsertTarget(t);
                UiFileWriter.upsertLocatorPoint(label);

                index = io.deskpilot.engine.recording.RegistryIndex.load(
                        io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout()
                );

                // ✅ Option A: do NOT add test steps here
                System.out.println("[SAVED] UiMap + Locators: " + constName);
                stats.addedPoints++;
                System.out.println();
                continue;
            }

            // -------------------------
            // R = REGION (capture + saves locator, NO step)
            // -------------------------
            if (mode.equalsIgnoreCase("R")) {

                boolean exists = index.existsRegionConst(constName);
                if (exists) {
                    if (!allowOverwrite) {
                        System.out.println("[REJECT] Name already exists: " + constName);
                        System.out.println("[HINT] " + buildCollisionHint(constName, "region"));
                        stats.reject("collision");
                        System.out.println();
                        continue;
                    }

                    if (requireOverwriteConfirm && !overwriteConfirm) {
                        System.out.println("[REJECT] Overwrite requested but overwriteConfirm is not enabled for: " + constName);
                        System.out.println("[HINT] Re-run with -Ddeskpilot.record.overwriteConfirm=true (or disable requireOverwriteConfirm). "
                                + "This protects against accidental overwrites.");
                        stats.reject("overwrite_confirm");
                        System.out.println();
                        continue;
                    }

                    System.out.println("[OVERWRITE] Replacing existing " + constName + " in UiRegions + Locators");
                    stats.overwrites++;
                }

                System.out.println("Drag to select the region on screen (ESC to cancel)...");
                Rectangle rectWin32 = RegionPickerOverlay.pick("Drag to select region: " + label);

                if (rectWin32 == null) {
                    System.out.println("[CANCEL] Region selection cancelled.\n");
                    continue;
                }

                System.out.println("[DEBUG] Selected rectWin32 = " + rectWin32);

                NormalizedRegion r;
                try {
                    r = NormalizedRegion.fromScreenRect(rectWin32, s.getClientRectWin32());
                } catch (Exception ex) {
                    System.out.println("[REJECT] fromScreenRect failed: " + ex.getMessage());
                    System.out.println();
                    continue;
                }

                RecorderValidation.ValidatedRegion vr =
                        validator.validateRegionSelection(rectWin32, s.getClientRectWin32(), r);

                if (vr.result().status() == ValidationResult.Status.REJECT) {
                    System.out.println("[REJECT] " + String.join(" | ", vr.result().messages()));
                    System.out.println("[HINT] " + buildRegionRejectHint(vr.result(), pol));
                    stats.reject(classifyRegionRejectReason(vr.result()));
                    System.out.println();
                    continue;
                }

                if (vr.result().status() == ValidationResult.Status.WARN) {
                    System.out.println("[WARN] " + String.join(" | ", vr.result().messages()));
                    System.out.println("[HINT] " + buildRegionWarnHint(vr.result()));
                    stats.warn(classifyRegionWarnReason(vr.result()));
                } else {
                    System.out.println("[OK] Region validated.");
                }

                // 13C: near-duplicate region using eps + IoU
                var hit = index.findNearDuplicateRegionVerbose(r, eps, minIou);
                if (hit.isPresent()) {
                    var h = hit.get();

                    String msg;
                    if (h.type() == io.deskpilot.engine.recording.RegistryIndex.NearDupType.STRICT) {
                        msg = "Near-duplicate region (STRICT) of existing const: " + h.otherConst()
                                + " | dx=" + fmt6(h.dx()) + " dy=" + fmt6(h.dy())
                                + " dw=" + fmt6(h.dw()) + " dh=" + fmt6(h.dh())
                                + " (eps=" + eps + ")"
                                + " | iou=" + String.format(java.util.Locale.US, "%.3f", h.iou());
                    } else {
                        msg = "Near-duplicate region (IOU) of existing const: " + h.otherConst()
                                + " | dx=" + fmt6(h.dx()) + " dy=" + fmt6(h.dy())
                                + " dw=" + fmt6(h.dw()) + " dh=" + fmt6(h.dh())
                                + " | iou=" + String.format(java.util.Locale.US, "%.3f", h.iou())
                                + " (minIou=" + minIou + ")";
                    }

                    String hint = buildNearDupHint(h, eps, minIou);

                    if (rejectNearDup) {
                        System.out.println("[REJECT] " + msg);
                        System.out.println("[HINT] " + hint);
                        stats.reject(h.type() == io.deskpilot.engine.recording.RegistryIndex.NearDupType.STRICT
                                ? "near_dup_strict"
                                : "near_dup_iou");
                        System.out.println();
                        continue;
                    } else {
                        System.out.println("[WARN] " + msg);
                        System.out.println("[HINT] " + hint);
                        stats.warn(h.type() == io.deskpilot.engine.recording.RegistryIndex.NearDupType.STRICT
                                ? "near_dup_strict"
                                : "near_dup_iou");
                    }
                }

                Rectangle overlayRect = (vr.clampedWin32() != null) ? vr.clampedWin32() : rectWin32;

                // ✅ Step-scoped overlay save (writes artifacts)
                s.step("record-region-" + label, () -> s.saveRegionOverlay(label, overlayRect, r));

                UiFileWriter.upsertRegion(label, r);
                UiFileWriter.upsertLocatorRegion(label);

                index = io.deskpilot.engine.recording.RegistryIndex.load(
                        io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout()
                );

                System.out.println("[SAVED] UiRegions + Locators: " + constName);
                stats.addedRegions++;
                System.out.println();
                continue;
            }

            // -------------------------
            // T = TEMPLATE (capture + saves locator, NO step)
            // -------------------------
            if (mode.equalsIgnoreCase("T")) {

                boolean exists = index.existsTemplateConst(constName);
                if (exists) {
                    if (!allowOverwrite) {
                        System.out.println("[REJECT] Name already exists: " + constName);
                        System.out.println("[HINT] " + buildCollisionHint(constName, "template"));
                        stats.reject("collision");
                        System.out.println();
                        continue;
                    }

                    if (requireOverwriteConfirm && !overwriteConfirm) {
                        System.out.println("[REJECT] Overwrite requested but overwriteConfirm is not enabled for: " + constName);
                        System.out.println("[HINT] Re-run with -Ddeskpilot.record.overwriteConfirm=true (or disable requireOverwriteConfirm). "
                                + "This protects against accidental overwrites.");
                        stats.reject("overwrite_confirm");
                        System.out.println();
                        continue;
                    }

                    System.out.println("[OVERWRITE] Replacing existing " + constName + " in UiTemplates + Locators");
                    stats.overwrites++;
                }

                try {
                    final TemplateTarget[] tplOut = new TemplateTarget[1];
                    s.step("record-template-" + label, () -> tplOut[0] = s.recordTemplateFromDrag(label));
                    TemplateTarget tpl = tplOut[0];

                    if (!validateTemplateImagesOrReject(tpl)) {
                        stats.reject("template_invalid");
                        System.out.println();
                        continue;
                    }

                    UiFileWriter.upsertTemplate(tpl);
                    UiFileWriter.upsertLocatorTemplate(label);

                    index = io.deskpilot.engine.recording.RegistryIndex.load(
                            io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout()
                    );

                    System.out.println("[SAVED] UiTemplates + Locators: " + constName);
                    stats.addedTemplates++;
                    System.out.println();
                } catch (Exception e) {
                    System.out.println("[CANCEL] Template selection cancelled/failed: " + e.getMessage());
                    System.out.println();
                }
                continue;
            }

            // -------------------------
            // V = TEMPLATE VARIANT (capture + updates template, NO step)
            // -------------------------
            if (mode.equalsIgnoreCase("V")) {
                try {
                    String resourcePath = nextVariantPath(label);

                    s.step("record-variant-" + label, () -> s.recordTemplateFromDrag(label, resourcePath));

                    RecorderPolicy p = RecorderPolicy.defaults();
                    Path abs = Paths.get("modules", "engine", "src", "main", "resources").resolve(resourcePath);

                    if (!Files.exists(abs)) {
                        System.out.println("[REJECT] variant image not found: " + resourcePath);
                        System.out.println("[HINT] Try recording the variant again. If the overlay saved elsewhere, move it under modules/engine/src/main/resources.");
                        stats.reject("variant_missing");
                        System.out.println();
                        continue;
                    }

                    BufferedImage img = ImageIO.read(abs.toFile());
                    if (img == null) {
                        System.out.println("[REJECT] variant is not a readable image: " + resourcePath);
                        System.out.println("[HINT] Ensure the file is a valid PNG. Re-record the variant selection.");
                        stats.reject("variant_invalid");
                        System.out.println();
                        continue;
                    }

                    if (img.getWidth() < p.minTemplatePxW || img.getHeight() < p.minTemplatePxH) {
                        System.out.println("[REJECT] variant too small: " + img.getWidth() + "x" + img.getHeight()
                                + "px (min " + p.minTemplatePxW + "x" + p.minTemplatePxH + "px)");
                        System.out.println("[HINT] Drag a slightly larger area around the icon. If needed tune minTemplateW/minTemplateH JVM props.");
                        stats.reject("variant_too_small");
                        System.out.println();
                        continue;
                    }

                    UiFileWriter.addTemplateVariant(label, resourcePath);

                    index = io.deskpilot.engine.recording.RegistryIndex.load(
                            io.deskpilot.engine.recording.RegistryIndex.EnginePaths.fromRepoLayout()
                    );

                    System.out.println("[SAVED] Template VARIANT: " + constName + " += " + resourcePath);
                    stats.addedVariants++;
                    System.out.println();
                } catch (Exception e) {
                    System.out.println("[CANCEL] Template variant cancelled/failed: " + e.getMessage());
                    System.out.println();
                }
            }
        } // end while

        // 13H: end-of-run summary
        System.out.println();
        System.out.println("=== Record Mode Summary ===");
        System.out.println(stats.summaryLine());

        if (!stats.warnReasons.isEmpty()) {
            System.out.println("Warnings (top reasons):");
            for (var e : stats.warnReasons.entrySet()) {
                System.out.println("  - " + e.getKey() + ": " + e.getValue());
            }
        }
        if (!stats.rejectReasons.isEmpty()) {
            System.out.println("Rejects (top reasons):");
            for (var e : stats.rejectReasons.entrySet()) {
                System.out.println("  - " + e.getKey() + ": " + e.getValue());
            }
        }

        System.out.println("Record Mode ended.");
    }}

    private static String buildCollisionHint(String constName, String kind) {
        return "Choose a different name (e.g., add _alt/_v2), OR enable overwrite with -Ddeskpilot.record.allowOverwrite=true. "
                + "If overwriting, double-check you're replacing the right " + kind + " (const=" + constName + ").";
    }

    private static String buildRegionWarnHint(ValidationResult vr) {
        String msg = String.join(" | ", vr.messages()).toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("clamped")) {
            return "Your selection crossed outside the client area and was clamped. If this was accidental, re-drag fully inside the window.";
        }
        return "Review the warning and re-record if it looks unintended.";
    }

    private static String buildRegionRejectHint(ValidationResult vr, RecorderPolicy pol) {
        String msg = String.join(" | ", vr.messages()).toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("too small")) {
            return "Drag a slightly larger region. Minimum is " + pol.minRegionPxW + "x" + pol.minRegionPxH + "px "
                    + "(tunable via -Ddeskpilot.record.minRegionW / -Ddeskpilot.record.minRegionH).";
        }
        if (msg.contains("collapsed") || msg.contains("empty")) {
            return "The region collapsed after clamping. Re-drag fully inside the client area.";
        }
        return "Re-record the region. If this keeps happening, verify the client rect and DPI settings.";
    }

    private static String classifyRegionWarnReason(ValidationResult vr) {
        String msg = String.join(" | ", vr.messages()).toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("clamped")) return "clamped";
        return "other";
    }

    private static String classifyRegionRejectReason(ValidationResult vr) {
        String msg = String.join(" | ", vr.messages()).toLowerCase(java.util.Locale.ROOT);
        if (msg.contains("too small")) return "region_too_small";
        if (msg.contains("collapsed") || msg.contains("empty")) return "region_empty";
        if (msg.contains("invalid")) return "invalid";
        return "other";
    }

    private static String nextVariantPath(String label) {
        Path base = Paths.get("modules", "engine", "src", "main", "resources", "icons");
        String v1 = label + ".png";
        Path v1Path = base.resolve(v1);

        if (!Files.exists(v1Path)) return "icons/" + v1;

        for (int i = 2; i <= 50; i++) {
            String name = label + "_v" + i + ".png";
            if (!Files.exists(base.resolve(name))) return "icons/" + name;
        }

        return "icons/" + label + "_v" + System.currentTimeMillis() + ".png";
    }

    private static boolean validateTemplateImagesOrReject(TemplateTarget tpl) {
        try {
            RecorderPolicy p = RecorderPolicy.defaults();
            Path resources = Paths.get("modules", "engine", "src", "main", "resources");

            for (String rel : tpl.imagePaths) {
                if (rel == null || rel.isBlank()) {
                    System.out.println("[REJECT] template has empty image path.");
                    return false;
                }

                Path abs = resources.resolve(rel);
                if (!Files.exists(abs)) {
                    System.out.println("[REJECT] template image not found: " + rel + " (" + abs + ")");
                    return false;
                }

                BufferedImage img = ImageIO.read(abs.toFile());
                if (img == null) {
                    System.out.println("[REJECT] not a readable image: " + rel);
                    return false;
                }

                if (img.getWidth() < p.minTemplatePxW || img.getHeight() < p.minTemplatePxH) {
                    System.out.println("[REJECT] template too small: " + rel + " => "
                            + img.getWidth() + "x" + img.getHeight()
                            + "px (min " + p.minTemplatePxW + "x" + p.minTemplatePxH + "px)");
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("[REJECT] template validation failed: " + e.getMessage());
            return false;
        }
    }

    private static String buildNearDupHint(
            io.deskpilot.engine.recording.RegistryIndex.NearDupRegionHit h,
            double eps,
            double minIou
    ) {
        if (h.type() == io.deskpilot.engine.recording.RegistryIndex.NearDupType.STRICT) {
            return "This is effectively the same region as " + h.otherConst() + ". "
                    + "If it was accidental: re-record with a different area OR enable rejection "
                    + "(-Ddeskpilot.record.rejectNearDuplicate=true). "
                    + "If it was intentional: keep it (warn only) or increase eps sensitivity only if needed (current eps=" + eps + ").";
        }

        return "This heavily overlaps with " + h.otherConst() + ". "
                + "If you want fewer IOU matches: raise minIou (e.g., -Ddeskpilot.record.nearDupMinIou=0.90). "
                + "If overlap is expected but distinct: shrink/adjust the region so IOU drops below " + minIou + ".";
    }

    private static String fmt6(double v) {
        return String.format(java.util.Locale.US, "%.6f", v);
    }


private static String readConstOrExit(java.io.BufferedReader br, String prompt) throws java.io.IOException {
    System.out.print(prompt);
    String raw = br.readLine();
    if (raw == null) return null;

    raw = raw.trim();
    if (raw.equalsIgnoreCase("exit")) return "exit";
    if (raw.isEmpty()) return "";

    return normalizeConstInput(raw);
}

private static String normalizeConstInput(String input) {
    if (input == null) return "";
    String s = input.trim();

    // allow "Locators.X" and "X"
    if (s.startsWith("Locators.")) s = s.substring("Locators.".length());

    // your consts are UPPER_SNAKE
    return s.trim().toUpperCase(java.util.Locale.ROOT);
}

private static void printSuggestionHintForKind(RegistryIndex index, String input, LocatorKind requiredKind) {
    index.suggestClosestConstOfKind(input, requiredKind).ifPresentOrElse(
            s -> System.out.println("[HINT] Try a " + requiredKind + " like: " + s),
            () -> System.out.println("[HINT] Record the correct asset first (P/R/T) or copy the exact name from Locators.java.")
    );
}

private static void printSuggestionHintAny(RegistryIndex index, String input) {
    index.suggestClosestLocator(input).ifPresentOrElse(
            s -> System.out.println("[HINT] Did you mean: " + s + " ?"),
            () -> System.out.println("[HINT] Record it first (P/R/T) or copy the exact name from Locators.java.")
    );
}

private static String readLineOrExit(BufferedReader br, String prompt) throws IOException {
    System.out.print(prompt);
    String raw = br.readLine();
    if (raw == null) return null;

    raw = raw.trim();
    if (raw.equalsIgnoreCase("exit")) return "exit";
    return raw;
}

private static boolean isUiRegionsConst(String c) {
    if (c == null) return false;
    String s = c.trim();
    // input was normalized already, but allow UiRegions.X
    return s.startsWith("UIREGIONS.") || s.startsWith("UiRegions.");
}

private static boolean isUiRegionsConst(RegistryIndex index, String input) {
    if (input == null) return false;
    String c = input.trim();
    if (c.startsWith("UiRegions.")) c = c.substring("UiRegions.".length());
    c = c.trim().toUpperCase(java.util.Locale.ROOT);
    return index.existsUiRegionConst(c); // ✅ you may need to add this method
}


private static long readTimeoutMsOrDefault(BufferedReader br, long defaultMs) throws IOException {
    String raw = readLineOrExit(br, "Timeout (default " + defaultMs + "ms). Examples: 5000, 5s, 800ms: ");
    if (raw == null) return -1;
    if (raw.equalsIgnoreCase("exit")) return -1;
    raw = raw.trim();
    if (raw.isBlank()) return defaultMs;

    try {
        return parseDurationToMs(raw);
    } catch (Exception e) {
        System.out.println("[REJECT] Invalid timeout: '" + raw + "'. Use 5000, 5s, 800ms.\n");
        return defaultMs; // safe fallback
    }
}

private static long parseDurationToMs(String raw) {
    String s = raw.trim().toLowerCase(java.util.Locale.ROOT);

    if (s.endsWith("ms")) {
        return Long.parseLong(s.substring(0, s.length() - 2).trim());
    }

    if (s.endsWith("s")) {
        String n = s.substring(0, s.length() - 1).trim();
        double seconds = Double.parseDouble(n);
        return (long) Math.round(seconds * 1000.0);
    }

    // plain number = ms (keep your current behavior)
    return Long.parseLong(s);
}

private static void printOcrLocatorHintList(RegistryIndex index) {
    List<String> ocr = index.listOcrLocatorConsts();
    if (ocr == null || ocr.isEmpty()) {
        System.out.println("[HINT] No OCR locators exist yet. Create one by typing W then a UiRegions const and choosing 'y'.");
        return;
    }

    System.out.println("[HINT] Available OCR locators:");
    for (int i = 0; i < Math.min(8, ocr.size()); i++) {
        System.out.println("  - " + ocr.get(i));
    }
    if (ocr.size() > 8) {
        System.out.println("  ... (" + ocr.size() + " total)");
    }
}



private static void printUiRegionsHintList(RegistryIndex index) {
    try {
        var regs = index.listUiRegionConsts();
        if (regs == null || regs.isEmpty()) {
            System.out.println("[HINT] No UiRegions constants exist yet. Record a region first (R).");
            return;
        }

        int n = Math.min(6, regs.size());
        System.out.println("[HINT] UiRegions constants (" + regs.size() + "):");
        for (int i = 0; i < n; i++) {
            System.out.println("       - " + regs.get(i));
        }
        if (regs.size() > n) System.out.println("       ... +" + (regs.size() - n) + " more");
    } catch (Exception ignore) {
        // UX only
    }
}


}
