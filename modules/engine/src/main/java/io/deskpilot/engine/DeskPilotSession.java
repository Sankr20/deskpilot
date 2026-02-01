package io.deskpilot.engine;

import com.sun.jna.platform.win32.WinDef.HWND;
import io.deskpilot.engine.image.ImageUtil;
import io.deskpilot.engine.locators.Locator;
import io.deskpilot.engine.locators.LocatorResult;
import io.deskpilot.engine.locators.LocatorSession;
import io.deskpilot.engine.locators.RegionLocator;
import io.deskpilot.engine.actions.Actions;
import io.deskpilot.engine.locators.TemplateLocatorEngine;
import io.deskpilot.engine.ocr.OcrConfig;
import io.deskpilot.engine.targets.TemplateTarget;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;


/**
 * Milestone 14: Locator-only execution.
 * - NO Targets.of / Target interface
 * - All actions go through Locator + LocatorResult
 */
public class DeskPilotSession implements LocatorSession, AutoCloseable  {
    private final DesktopDriver driver;
    private final HWND hwnd;
    private final Rectangle clientRectWin32;
    private final Rectangle clientRectRobot;
private Actions actions;
    private final Artifacts artifacts;
    private final RunOptions runOptions;
    private Path currentStepDir; // set when step() starts
private static final int OCR_MIN_CROP_W = 60;
private static final int OCR_MIN_CROP_H = 18;

    private OcrEngine ocr;
    private OcrConfig ocrConfig = OcrConfig.defaults();

    private BufferedImage beforeImg;
    private BufferedImage afterImg;

    private DeskPilotSession(
            DesktopDriver driver,
            HWND hwnd,
            Rectangle clientRectWin32,
            Rectangle clientRectRobot,
            Artifacts artifacts,
            RunOptions runOptions) {
        this.driver = driver;
        this.hwnd = hwnd;
        this.clientRectWin32 = clientRectWin32;
        this.clientRectRobot = clientRectRobot;
        this.artifacts = artifacts;
        this.runOptions = java.util.Objects.requireNonNull(runOptions, "runOptions is null");
    }

    // -------------------------
    // Attach
    // -------------------------

    public static DeskPilotSession attachPickWindow(String runFolder) throws Exception {
    return attachPickWindow(RunOptions.builder().runName(runFolder).build());
}


public static DeskPilotSession attachPickWindow(RunOptions options) throws Exception {
    DpiAwareness.enable();

    long timeoutMs = options.attachTimeoutMs();
    System.out.println("Click the target window to attach (timeout " + (timeoutMs / 1000) + "s)...");
    HWND hwnd = WindowManager.pickWindowHandleOnClick(timeoutMs);
    hwnd = WindowManager.toTopLevel(hwnd);

    String title = WindowManager.getWindowTitle(hwnd);
    Rectangle clientRectWin32 = WindowManager.getClientRectOnScreenOrThrow(hwnd);
    Rectangle clientRectRobot = RobotCoords.toRobotRect(clientRectWin32);

    System.out.println("Selected window: " + title);
    System.out.println("Client rect (win32): " + clientRectWin32);

    if (options.bringToFrontOnAttach()) {
        WindowManager.bringToFront(hwnd);
        Thread.sleep(300);
    }

    Path outDir = RunOptions.prepareRunFolder(options);

// Ensure startup dir exists (important)
Path startupDir = outDir.resolve("01-startup");
Files.createDirectories(startupDir);

// Write attach diagnostics
Files.writeString(startupDir.resolve("attach.txt"),
        "title=" + title + System.lineSeparator() +
        "clientRectWin32=" + clientRectWin32 + System.lineSeparator() +
        "clientRectRobot=" + clientRectRobot + System.lineSeparator()
);

Artifacts artifacts = new Artifacts(outDir);
DesktopDriver driver = new DesktopDriver();

DeskPilotSession s = new DeskPilotSession(driver, hwnd, clientRectWin32, clientRectRobot, artifacts, options);

// ✅ make attach stabilization step-scoped
s.step("startup", () -> {
    s.before();                 // gives you a baseline screenshot in 01-startup
    s.stabilizeInStep("attach");
});

return s;
}




    public Actions actions() {
    if (actions == null) actions = new Actions(this);
    return actions;
}

    // -------------------------
    // Getters
    // -------------------------

    @Override
    public Rectangle getClientRectWin32() {
        return clientRectWin32;
    }

    public Rectangle getClientRectRobot() {
        return clientRectRobot;
    }

    public DesktopDriver getDriver() {
        return driver;
    }

    public Artifacts getArtifacts() {
        return artifacts;
    }

    private OcrEngine ocr() {
        if (ocr == null)
            ocr = new OcrEngine();
        return ocr;
    }

    public OcrEngine getOcr() {
        return ocr();
    }

    public OcrConfig getOcrConfig() {
        return ocrConfig;
    }

    public DeskPilotSession withOcrConfig(OcrConfig cfg) {
        if (cfg == null)
            throw new IllegalArgumentException("cfg is null");
        this.ocrConfig = cfg;
        return this;
    }

    private BufferedImage captureClient() {
    return driver.screenshot(clientRectRobot);
}


    // -------------------------
    // Evidence
    // -------------------------

    public DeskPilotSession before() throws Exception {
        this.beforeImg = driver.screenshot(clientRectRobot);
        saveStepPng("before.png", beforeImg);
        System.out.println("Saved BEFORE screenshot");
        return this;
    }

    public DeskPilotSession after() throws Exception {
        this.afterImg = driver.screenshot(clientRectRobot);
        saveStepPng("after.png", afterImg);
        System.out.println("Saved AFTER screenshot");

        return this;
    }

    // -------------------------
    // Locator execution (Milestone 14)
    // -------------------------

    /**
     * Locate without performing an action. Never throws for NOT_FOUND / NEAR_MISS.
     */
   public LocatorResult locate(Locator locator) throws Exception {
    if (locator == null)
        throw new IllegalArgumentException("locator is null");

    LocatorResult r = locator.locate(this);

    return r;
}

    /** Click any locator that resolves to a WIN32 point. */
    public DeskPilotSession click(Locator btnpoint) throws Exception {
        if (btnpoint == null)
            throw new IllegalArgumentException("locator is null");

        LocatorResult r = locate(btnpoint);

        if (!r.isFound())
            throw new RuntimeException("Locator not found: " + r);
        if (r.point == null)
            throw new RuntimeException("Locator resolved without a click point: " + r);

        System.out.println("Clicking Locator: label=" + r.label + " kind=" + r.kind);

        clickWin32(r.point);
        return this;
    }

    /** Click with retries until timeout. */
    public DeskPilotSession click(Locator locator, long timeoutMs) throws Exception {
        if (locator == null)
            throw new IllegalArgumentException("locator is null");
        if (timeoutMs <= 0)
            throw new IllegalArgumentException("timeoutMs must be > 0");

        long end = System.currentTimeMillis() + timeoutMs;
        Exception last = null;
        int attempts = 0;

        while (System.currentTimeMillis() < end) {
            attempts++;
            try {
                return click(locator);
            } catch (Exception e) {
                last = e;
                Thread.sleep(200);
            }
        }

        throw new RuntimeException(
                "Timeout clicking locator: label=" + locator.label() +
                        " kind=" + locator.kind() +
                        " timeoutMs=" + timeoutMs +
                        " attempts=" + attempts +
                        (last != null ? " lastError=" + last.getMessage() : ""),
                last);
    }

    public DeskPilotSession waitForTextContains(Locator locator, String expected, long timeoutMs) throws Exception {
        if (locator == null)
            throw new IllegalArgumentException("locator is null");
        if (!(locator instanceof RegionLocator rl)) {
            throw new IllegalArgumentException("waitForTextContains requires a REGION locator, got: " + locator.kind());
        }
        return waitForTextContains(rl.region(), expected, timeoutMs);
    }

    public String readText(Locator locator) throws Exception {
        if (locator == null)
            throw new IllegalArgumentException("locator is null");
        if (!(locator instanceof RegionLocator rl)) {
            throw new IllegalArgumentException("readText requires a REGION locator, got: " + locator.kind());
        }
        return readText(rl.region());
    }

    public BigDecimal readMoney(Locator locator) throws Exception {
        if (locator == null)
            throw new IllegalArgumentException("locator is null");
        if (!(locator instanceof RegionLocator rl)) {
            throw new IllegalArgumentException("readMoney requires a REGION locator, got: " + locator.kind());
        }
        return readMoney(rl.region());
    }

    // -------------------------
    // LocatorSession (used by locators.*)
    // -------------------------

    @Override
    public Point resolvePointWin32(UiTarget target) throws Exception {
        if (target == null)
            throw new IllegalArgumentException("UiTarget is null");
        return NormalizedRegion.toScreenPoint(target.xPct(), target.yPct(), clientRectWin32);
    }

    @Override
 public LocatorResult locateTemplate(TemplateTarget target, String label) throws Exception {
    BufferedImage clientShot = driver.screenshot(clientRectRobot);

    LocatorResult r = TemplateLocatorEngine.locate(clientShot, clientRectWin32, target, label);

    if (r != null && !r.isFound()) {
        dumpTemplateDiagnostics(label, clientShot, r);
    }

    return r;
}


    // -------------------------
    // Low-level actions
    // -------------------------

  public DeskPilotSession clickWin32(Point win32) throws Exception {
    if (win32 == null) throw new IllegalArgumentException("win32 is null");
    driver.click(win32);
    return this;
}

public DeskPilotSession clearFocused() {
    driver.delay(120);
    driver.keyCombo(java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_A);
    driver.keyTap(java.awt.event.KeyEvent.VK_BACK_SPACE);
    driver.delay(120);
    return this;
}


 public DeskPilotSession paste(String text) throws Exception {
    System.out.println("Pasting: " + text);
    driver.pasteText(text);
    return this;
}


    public DeskPilotSession selectAll() throws Exception {
    driver.selectAll();       // implement in DesktopDriver
    return this;
}


public DeskPilotSession type(String text) throws Exception {
    return typeText(text);
}


public DeskPilotSession typeText(String text) throws Exception {
    driver.typeText(text);
    return this;
}


public void saveStepText(String name, String content) throws Exception {
    if (currentStepDir == null) return;
    artifacts.saveText(currentStepDir, name, content == null ? "" : content);
}


    // -------------------------
    // Verification / waits
    // -------------------------

    public DeskPilotSession waitForUiStable(long timeoutMs) throws Exception {
        UiStabilityWait.waitForStable(
                driver,
                clientRectRobot,
                timeoutMs,
                350,
                0.0015,
                90);
        return this;
    }

    public DeskPilotSession waitForPixelChange(NormalizedRegion region, long timeoutMs, double diffThreshold01)
            throws Exception {

        Rectangle regionWin32 = region.toScreenRect(clientRectWin32);
        Rectangle regionRobot = RobotCoords.toRobotRect(regionWin32);

        BufferedImage baseline = driver.screenshot(regionRobot);
        waitForPixelChangeFromBaseline(baseline, regionRobot, timeoutMs, diffThreshold01);
        return this;
    }

    private void waitForPixelChangeFromBaseline(
            BufferedImage baseline,
            Rectangle regionRobot,
            long timeoutMs,
            double diffThreshold01) throws Exception {

        long end = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < end) {
            Thread.sleep(80);
            BufferedImage now = driver.screenshot(regionRobot);

            double diff = imageDiff01(baseline, now);
            if (diff >= diffThreshold01) {
                System.out.println("Pixel change detected. diff=" + diff);
                return;
            }
        }

        throw new RuntimeException("Timeout waiting for pixel change. timeoutMs=" + timeoutMs);
    }

    /**
     * Semantic verification via OCR: wait until OCR text contains expected
     * substring.
     */
    public DeskPilotSession waitForTextContains(NormalizedRegion region, String expected, long timeoutMs)
            throws Exception {

        if (region == null)
            throw new IllegalArgumentException("region is null");
        if (expected == null || expected.trim().isEmpty())
            throw new IllegalArgumentException("expected text is empty");

        String exp = expected.trim().toLowerCase();
        long end = System.currentTimeMillis() + timeoutMs;

        String last = "";
        int polls = 0;

        while (System.currentTimeMillis() < end) {
            polls++;

            OcrCapture cap = captureForOcr(region);
            String text = getOcr().readText(cap.preprocessed);
            last = text;

            if (Boolean.getBoolean("deskpilot.ocr.dump")) {
    String base = "ocr-poll-" + polls;
    saveStepPng(base + "-cropped.png", cap.cropped);
    saveStepPng(base + "-preprocessed.png", cap.preprocessed);

    if (currentStepDir != null) {
        artifacts.saveText(
                currentStepDir,
                base + ".txt",
                "expectedContains: " + expected + "\n" +
                "poll: " + polls + "\n" +
                "region: " + region + "\n" +
                "raw:\n" + (text == null ? "" : text) + "\n"
        );
    }
}

            if (!text.isEmpty() && text.contains(exp)) {
                System.out.println(
                        "Text matched. expectedContains='" + expected + "' actual='" + text + "' polls=" + polls);
                return this;
            }

            sleep(200);
        }

        // Save last crop for debugging
        // Save last OCR inputs for debugging
try {
    OcrCapture cap = captureForOcr(region);

    String base = "ocr-timeout-" + safeFile(expected);

    // 1) what we cropped from the client screenshot
    saveStepPng(base + "-cropped.png", cap.cropped);

    // 2) what we actually fed into Tesseract
    saveStepPng(base + "-preprocessed.png", cap.preprocessed);

    // 3) dump text details (raw + normalized)
    String raw = last == null ? "" : last;
    String norm = normalizeOcrText(raw);

    if (currentStepDir != null) {
        artifacts.saveText(
                currentStepDir,
                base + ".txt",
                "expectedContains: " + expected + "\n" +
                "polls: " + polls + "\n" +
                "region: " + region + "\n" +
                "raw:\n" + raw + "\n\n" +
                "normalized:\n" + norm + "\n"
        );
    }

    System.out.println("Saved OCR timeout artifacts: " + base + "-cropped.png / -preprocessed.png / .txt");

} catch (Exception ignored) {
}


        throw new RuntimeException("Timeout waiting for text. expectedContains='" + expected + "', lastOcr='" + last
                + "', timeoutMs=" + timeoutMs);
    }

    public DeskPilotSession waitForTextContainsWin32Bounds(Rectangle boundsWin32, String expected, long timeoutMs) throws Exception {
    if (boundsWin32 == null) throw new IllegalArgumentException("boundsWin32 is null");
    if (expected == null) throw new IllegalArgumentException("expected is null");

    // Convert WIN32 bounds -> NormalizedRegion relative to client rect
    NormalizedRegion region = NormalizedRegion.fromScreenRect(boundsWin32, clientRectWin32);

    return waitForTextContains(region, expected, timeoutMs);
}


    // -------------------------
    // OCR text IO (Region-based)
    // -------------------------

   public String readText(NormalizedRegion region) throws Exception {
    if (region == null)
        throw new IllegalArgumentException("region is null");

    OcrCapture cap = captureForOcr(region);
    String raw = getOcr().readText(cap.preprocessed);
    String txt = (raw == null) ? "" : raw;

    // Always persist the last OCR text alongside last images.
    // This makes OCR failures diagnosable without needing deskpilot.ocr.dump=true.
    try {
        var cfg = getOcrConfig();
        String preset = (cfg != null && cfg.preset != null) ? cfg.preset.name().toLowerCase() : "default";
        String norm = normalizeOcrText(txt);

        saveStepText(
                "ocr_text_last_" + preset + ".txt",
                "preset=" + preset + "\n" +
                "region=" + region + "\n\n" +
                "raw:\n" + txt + "\n\n" +
                "normalized:\n" + norm + "\n"
        );
    } catch (Exception ignore) {
        // diagnostics must never fail OCR reads
    }

    System.out.println("readText region=" + region + " => '" + txt + "'");
    return txt;
}



    public BigDecimal readNumber(NormalizedRegion region) throws Exception {
        String t = readTextNormalized(region);
        return parseDecimalOrThrow(t, "readNumber");
    }

    public BigDecimal readMoney(NormalizedRegion region) throws Exception {
        String t = readTextNormalized(region);

        t = t.replaceAll("[$€£₹¥₩₽₺₫฿₦₱₪₴₲₵₡₭₮₸₠]+", "");
        t = t.replaceAll("(?i)\\b(usd|cad|aud|eur|gbp|inr|jpy|cny|rs|sar|aed)\\b", "");

        return parseDecimalOrThrow(t, "readMoney");
    }

    public String readTextNormalized(NormalizedRegion region) throws Exception {
        return normalizeOcrText(readText(region));
    }

   private OcrCapture captureForOcr(NormalizedRegion region) throws Exception {
    Rectangle regionWin32 = region.toScreenRect(clientRectWin32);
    Rectangle regionRobot = RobotCoords.toRobotRect(regionWin32);

    BufferedImage clientShot = driver.screenshot(clientRectRobot);

    Rectangle local = new Rectangle(
            regionRobot.x - clientRectRobot.x,
            regionRobot.y - clientRectRobot.y,
            regionRobot.width,
            regionRobot.height
    );

    // ✅ clamp to screenshot bounds (prevents edge failures)
    Rectangle shotBounds = new Rectangle(0, 0, clientShot.getWidth(), clientShot.getHeight());
    Rectangle clamped = local.intersection(shotBounds);

    if (clamped.isEmpty()) {
        try { saveStepPng("ocr_client.png", clientShot); } catch (Exception ignore) {}
        throw new IllegalArgumentException("OCR region is outside client screenshot. local=" + local + " shot=" + shotBounds);
    }

    BufferedImage cropped = ImageUtil.crop(clientShot, clamped);

    // ✅ min-size guard
    int w = cropped.getWidth();
    int h = cropped.getHeight();

    if (w < OCR_MIN_CROP_W || h < OCR_MIN_CROP_H) {
        try { saveStepPng("ocr_crop_too_small.png", cropped); } catch (Exception ignore) {}
        throw new IllegalArgumentException(
                "OCR region too small: " + w + "x" + h +
                " (min " + OCR_MIN_CROP_W + "x" + OCR_MIN_CROP_H + "). " +
                "Record a slightly larger REGION around the text."
        );
    }

    var cfg = getOcrConfig();
    var res = io.deskpilot.engine.ocr.OcrPipeline.preprocess(cropped, cfg);

    // ✅ preset-tagged artifacts (optional but extremely helpful)
String preset = (cfg != null && cfg.preset != null) ? cfg.preset.name().toLowerCase() : "default";

// Always keep the *latest* OCR inputs (overwrite each attempt)
try {
    saveStepPng("ocr_crop_last_" + preset + ".png", cropped);
    saveStepPng("ocr_pre_last_" + preset + ".png", res.preprocessed);
} catch (Exception ignore) {}

// Verbose per-attempt dumps only when enabled
if (Boolean.getBoolean("deskpilot.ocr.dump")) {
    try {
        String base = "ocr_attempt_" + System.currentTimeMillis() + "_" + preset;
        saveStepPng(base + "_crop.png", cropped);
        saveStepPng(base + "_pre.png", res.preprocessed);
    } catch (Exception ignore) {}
}

    return new OcrCapture(cropped, res.preprocessed, res.scaleFactor, regionWin32, regionRobot, clamped);
}

    private static final class OcrCapture {
        final BufferedImage cropped;
        final BufferedImage preprocessed;
        final double scaleFactor;

        final Rectangle regionWin32;
        final Rectangle regionRobot;
        final Rectangle localInClientShot;

        OcrCapture(
                BufferedImage cropped,
                BufferedImage preprocessed,
                double scaleFactor,
                Rectangle regionWin32,
                Rectangle regionRobot,
                Rectangle localInClientShot) {
            this.cropped = cropped;
            this.preprocessed = preprocessed;
            this.scaleFactor = scaleFactor;
            this.regionWin32 = regionWin32;
            this.regionRobot = regionRobot;
            this.localInClientShot = localInClientShot;
        }
    }

    // -------------------------
    // Recording helpers (still useful)
    // -------------------------

    public UiTarget recordTargetFromMouse(String name) throws Exception {
        Point mouseWin32 = WindowManager.getMousePositionWin32();
        int relX = mouseWin32.x - clientRectWin32.x;
        int relY = mouseWin32.y - clientRectWin32.y;

        double xPct = relX / (double) clientRectWin32.width;
        double yPct = relY / (double) clientRectWin32.height;

        UiTarget t = new UiTarget(name, xPct, yPct);

        System.out.println("RECORDED TARGET:");
        System.out.println("  name=\"" + name + "\"");
        System.out.println("  mouseWin32=" + mouseWin32);
        System.out.println("  relToClient=(" + relX + "," + relY + ")");
        System.out.printf("  xPct=%.6f, yPct=%.6f%n", xPct, yPct);

        if (beforeImg == null) {
            this.beforeImg = driver.screenshot(clientRectRobot);
            saveStepPng("before.png", beforeImg);
        }

        // Overlay point evidence
        Point clickRobot = RobotCoords.toRobotPoint(mouseWin32);
        int rx = clickRobot.x - clientRectRobot.x;
        int ry = clickRobot.y - clientRectRobot.y;

        BufferedImage overlay = new BufferedImage(beforeImg.getWidth(), beforeImg.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.drawImage(beforeImg, 0, 0, null);

        g.setStroke(new BasicStroke(3));
        g.setFont(new Font("Arial", Font.BOLD, 18));

        int r = 8;
        g.setColor(Color.RED);
        g.fillOval(rx - r, ry - r, r * 2, r * 2);

        g.setColor(Color.BLACK);
        g.drawOval(rx - r - 2, ry - r - 2, (r * 2) + 4, (r * 2) + 4);

        g.setColor(Color.RED);
        g.drawString("REC: " + name, rx + 12, ry - 12);

        g.dispose();

        String fileName = "rec-point-" + safeFile(name) + ".png";
        saveStepPng(fileName, overlay);
        System.out.println("Saved RECORD POINT overlay: " + fileName);

        return t;
    }

    public void saveRegionOverlay(String name, Rectangle rectWin32, NormalizedRegion region) throws Exception {
        if (beforeImg == null) {
            this.beforeImg = driver.screenshot(clientRectRobot);
            saveStepPng("before.png", beforeImg);
        }

        Rectangle rectRobot = RobotCoords.toRobotRect(rectWin32);

        int rx = rectRobot.x - clientRectRobot.x;
        int ry = rectRobot.y - clientRectRobot.y;

        BufferedImage overlay = new BufferedImage(beforeImg.getWidth(), beforeImg.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = overlay.createGraphics();
        g.drawImage(beforeImg, 0, 0, null);

        g.setStroke(new BasicStroke(3));
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.RED);

        g.drawRect(rx, ry, rectRobot.width, rectRobot.height);
        g.drawString("REC-REGION: " + name, rx + 10, Math.max(20, ry - 10));

        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        g.drawString(String.format(Locale.US, "x=%.6f y=%.6f w=%.6f h=%.6f",
                region.xPct, region.yPct, region.wPct, region.hPct),
                rx + 10, Math.max(40, ry + 20));

        g.dispose();

        String fileName = "rec-region-" + safeFile(name) + ".png";
        saveStepPng(fileName, overlay);

        System.out.println("Saved RECORD REGION overlay: " + fileName);
    }

    // -------------------------
    // Artifacts / utils
    // -------------------------
private void saveStepPng(String fileName, BufferedImage img) throws Exception {
    // Production rule: never crash just because a step wrapper wasn’t active.
    Path dir = (currentStepDir != null) ? currentStepDir : artifacts.outDir();

    if (currentStepDir == null) {
        System.err.println("[WARN] No active stepDir; saving " + fileName + " to run root: " + dir.toAbsolutePath());
        
    }

    artifacts.savePng(dir, fileName, img);
}


  private void dumpTemplateDiagnostics(String label, BufferedImage clientShot, LocatorResult r) {
    if (currentStepDir == null) return;

    try {
        artifacts.savePng(currentStepDir, "client.png", clientShot);
        artifacts.saveText(currentStepDir, "diag.txt", String.valueOf(r));

        // Only save overlay if we have bounds
        if (r != null && r.bounds != null) {
            Rectangle b = r.bounds;

            Rectangle local = new Rectangle(
                    b.x - clientRectWin32.x,
                    b.y - clientRectWin32.y,
                    b.width,
                    b.height
            );

            // Optional clamp
            Rectangle imgBounds = new Rectangle(0, 0, clientShot.getWidth(), clientShot.getHeight());
            local = local.intersection(imgBounds);

            BufferedImage overlay = new BufferedImage(
                    clientShot.getWidth(),
                    clientShot.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D g = overlay.createGraphics();
            g.drawImage(clientShot, 0, 0, null);
            g.setStroke(new BasicStroke(3));
            g.setColor(Color.RED);
            g.drawRect(local.x, local.y, local.width, local.height);
            g.dispose();

            artifacts.savePng(currentStepDir, "overlay.png", overlay);
        }

    } catch (Exception ignored) {
        // diagnostics must never fail the run
    }
}





    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private static double imageDiff01(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());

        long total = 0;
        long changed = 0;

        int stride = 4;

        for (int y = 0; y < h; y += stride) {
            for (int x = 0; x < w; x += stride) {
                total++;
                if (a.getRGB(x, y) != b.getRGB(x, y))
                    changed++;
            }
        }

        return total == 0 ? 0 : (changed / (double) total);
    }

    private static String normalizeOcrText(String s) {
        if (s == null)
            return "";
        s = s.replace('\u00A0', ' ');
        s = s.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", " ");
        s = s.replaceAll("[ \t]+", " ");
        s = s.replaceAll("\\s*\\n\\s*", "\n");
        return s.trim();
    }

    private static BigDecimal parseDecimalOrThrow(String text, String label) {
        if (text == null)
            text = "";
        String t = text;

        t = t.replaceAll("[^0-9,\\.\\-]+", "");

        if (t.contains(",") && t.contains(".")) {
            t = t.replace(",", "");
        } else {
            if (t.contains(",") && !t.contains(".")) {
                t = t.replace(",", ".");
            }
        }

        t = t.replaceAll("(?<!^)-", "");

        int firstDot = t.indexOf('.');
        if (firstDot >= 0) {
            String before = t.substring(0, firstDot + 1);
            String after = t.substring(firstDot + 1).replace(".", "");
            t = before + after;
        }

        t = t.trim();
        if (t.isEmpty() || t.equals("-") || t.equals(".")) {
            throw new RuntimeException(label + ": could not parse number from OCR text: '" + text + "'");
        }

        try {
            return new BigDecimal(t);
        } catch (Exception e) {
            throw new RuntimeException(
                    label + ": could not parse number from OCR text: '" + text + "' cleaned='" + t + "'", e);
        }
    }

    private static String safeFile(String s) {
        if (s == null)
            return "null";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    @FunctionalInterface
    public interface StepBody {
        void run() throws Exception;
    }

    /**
     * Run a named step wrapper for consistent logs and future artifact hardening.
     */
public void step(String stepName, StepBody body) throws Exception {
    if (stepName == null || stepName.isBlank())
        stepName = "step";
    if (body == null)
        throw new IllegalArgumentException("body is null");

    Path prev = currentStepDir;
    Path stepDir = artifacts.stepDir(stepName);
    currentStepDir = stepDir;

    System.out.println();
    System.out.println("=== STEP: " + stepName + " ===");
    System.out.println("Step dir: " + stepDir.toAbsolutePath());

   boolean stepScreenshots = runOptions.stepScreenshots()
        && !stepName.equals("startup")
        && !stepName.equals("teardown");


    try {
        // ✅ optional: always capture a "before" screenshot for this step
        if (stepScreenshots) {
            try {
                BufferedImage img = driver.screenshot(clientRectRobot);
                saveStepPng("step_before.png", img);
            } catch (Exception ignore) {
                // don't fail the step if screenshot fails
            }
        }

        body.run();

        // ✅ optional: always capture an "after" screenshot for this step
        if (stepScreenshots) {
            try {
                BufferedImage img = driver.screenshot(clientRectRobot);
                saveStepPng("step_after.png", img);
            } catch (Exception ignore) {
                // don't fail the step if screenshot fails
            }
        }

    } catch (Exception e) {
        artifacts.saveText(stepDir, "error.txt", String.valueOf(e));
        System.out.println("STEP FAILED: " + stepName + " err=" + e.getMessage());
        throw e;
    } finally {
        currentStepDir = prev;
    }
}


    // -------------------------
    // Recorder helpers (template drag) - used by EngineRecordMode
    // -------------------------

    public TemplateTarget recordTemplateFromDrag(String label) throws Exception {
        return recordTemplateFromDrag(label, "icons/" + label + ".png");
    }

    public TemplateTarget recordTemplateFromDrag(String label, String resourcePath) throws Exception {
        if (label == null || label.isBlank())
            throw new IllegalArgumentException("label is blank");
        if (resourcePath == null || resourcePath.isBlank())
            throw new IllegalArgumentException("resourcePath is blank");

        System.out.println("Drag to select TEMPLATE: " + label + " (ESC to cancel) ...");
        Rectangle pickedScreen = RegionPickerOverlay.pick("Drag template: " + label);

        if (pickedScreen == null) {
            throw new RuntimeException("Template selection cancelled.");
        }

        // Take ONE client screenshot (truth for cropping)
        BufferedImage clientShot = driver.screenshot(clientRectRobot);

        // Interpret picker rectangle as either robot coords or win32 coords; choose
        // whichever intersects client more.
        Rectangle rAsRobot1 = pickedScreen;
        Rectangle rAsRobot2 = RobotCoords.toRobotRect(pickedScreen);

        Rectangle i1 = rAsRobot1.intersection(clientRectRobot);
        Rectangle i2 = rAsRobot2.intersection(clientRectRobot);

        long area1 = (long) Math.max(0, i1.width) * Math.max(0, i1.height);
        long area2 = (long) Math.max(0, i2.width) * Math.max(0, i2.height);

        Rectangle pickedRobot = (area2 > area1) ? rAsRobot2 : rAsRobot1;
        Rectangle clippedRobot = pickedRobot.intersection(clientRectRobot);

        if (clippedRobot.isEmpty() || clippedRobot.width <= 1 || clippedRobot.height <= 1) {
            throw new RuntimeException(
                    "Template drag selection is outside client area. pickedScreen=" + pickedScreen +
                            " pickedRobot=" + pickedRobot + " clientRectRobot=" + clientRectRobot);
        }

        // Convert to client-local for cropping
        Rectangle local = new Rectangle(
                clippedRobot.x - clientRectRobot.x,
                clippedRobot.y - clientRectRobot.y,
                clippedRobot.width,
                clippedRobot.height);

        Rectangle bounds = new Rectangle(0, 0, clientShot.getWidth(), clientShot.getHeight());
        Rectangle localClipped = local.intersection(bounds);

        if (localClipped.isEmpty() || localClipped.width <= 1 || localClipped.height <= 1) {
            throw new RuntimeException("Local crop out of bounds. local=" + local + " bounds=" + bounds);
        }

        BufferedImage template = ImageUtil.crop(clientShot, localClipped);

        saveTemplateResource(template, resourcePath);

        // Debug artifact
        try {
            saveStepPng("rec-tpl-crop-" + safeFile(label) + ".png", template);

        } catch (Exception ignored) {
        }

        // Suggest search area around selection with padding
        int cw = clientShot.getWidth();
        int ch = clientShot.getHeight();

        double xPct = localClipped.x / (double) cw;
        double yPct = localClipped.y / (double) ch;
        double wPct = localClipped.width / (double) cw;
        double hPct = localClipped.height / (double) ch;

        double padXPct = 0.05;
        double padYPct = 0.04;

        double ax = clamp01(xPct - padXPct);
        double ay = clamp01(yPct - padYPct);
        double aw = clamp01((xPct + wPct + padXPct) - ax);
        double ah = clamp01((yPct + hPct + padYPct) - ay);

        if (aw < 0.01)
            aw = 0.01;
        if (ah < 0.01)
            ah = 0.01;

        TemplateTarget t = TemplateTarget.of(label, resourcePath)
                .withSearchAreaPct(new io.deskpilot.engine.targets.SearchAreaPct(ax, ay, aw, ah))
                .withMinScore(0.85);

        System.out.println("RECORDED TEMPLATE:");
        System.out.println("  label=" + label);
        System.out.println("  resourcePath=" + resourcePath);
        System.out.println("  pickedScreen=" + pickedScreen);
        System.out.println("  clippedRobot=" + clippedRobot);
        System.out.println("  localCrop=" + localClipped);
        System.out.println("  searchAreaPct=x=" + String.format(Locale.US, "%.3f", ax) +
                " y=" + String.format(Locale.US, "%.3f", ay) +
                " w=" + String.format(Locale.US, "%.3f", aw) +
                " h=" + String.format(Locale.US, "%.3f", ah));

        return t;
    }

    private static double clamp01(double v) {
        if (v < 0)
            return 0;
        if (v > 1)
            return 1;
        return v;
    }

    private static void saveTemplateResource(BufferedImage img, String resourcePath) throws Exception {
        Path out = Paths.get("modules", "engine", "src", "main", "resources", resourcePath);
        Files.createDirectories(out.getParent());
        javax.imageio.ImageIO.write(img, "png", out.toFile());
        System.out.println("Saved TEMPLATE PNG to: " + out.toAbsolutePath());
    }

 // in DeskPilotSession fields
private boolean closed = false;

@Override
public void close() {
    if (closed) return;          // ✅ idempotent
    closed = true;

    try {
        step("teardown", this::after);
    } catch (Exception e) {
        System.err.println("DeskPilotSession.close() failed: " + e.getMessage());
        e.printStackTrace();
    }
}


public DeskPilotSession restoreAndBringToFront() throws Exception {
    // If you already have bringToFront(), call it here.
    // Add "restore" behavior (SW_RESTORE) if minimized.
    WindowManager.bringToFront(hwnd); // implement using User32.ShowWindow + SetForegroundWindow
    Thread.sleep(150);
    return this;
}


public NormalizedRegion pickRegion() throws Exception {
    System.out.println("Drag to select region (ESC to cancel) ...");

    Rectangle rectWin32 = RegionPickerOverlay.pick("Drag to select region (ESC to cancel)");
    if (rectWin32 == null) {
        throw new IllegalStateException("User cancelled region selection");
    }

    NormalizedRegion r = NormalizedRegion.fromScreenRect(rectWin32, getClientRectWin32());

    try {
        step("pick-region", () -> saveRegionOverlay("picked", rectWin32, r));
    } catch (Exception ignore) {}

    return r;
}

public void stabilizeInStep(String reason) throws Exception {
    StabilityOptions opt = runOptions.stability();
    if (opt == null || !opt.enabled) return;

    if (opt.bringToFront) {
        WindowManager.bringToFront(hwnd);
        driver.delay(120);
    }

    long start = System.currentTimeMillis();
    long deadline = start + opt.timeoutMs;

    BufferedImage prev = captureClient();
    saveStepPng("stabilize_before.png", prev);

    long stableSince = -1L;
    int iter = 0;

    while (System.currentTimeMillis() < deadline) {
        driver.delay((int) opt.pollMs);

        BufferedImage cur = captureClient();
        double diff = ImageDiff.diffRatio(prev, cur);

        iter++;
        saveStepText(String.format("stabilize_diff_%02d.txt", iter),
                "diff=" + diff + System.lineSeparator() +
                "threshold=" + opt.diffThreshold + System.lineSeparator());

        if (diff <= opt.diffThreshold) {
            if (stableSince < 0) stableSince = System.currentTimeMillis();
            long stableFor = System.currentTimeMillis() - stableSince;

            if (stableFor >= opt.stableForMs) {
                saveStepPng("stabilize_stable.png", cur);
                saveStepText("stabilize_timing.txt",
                        "reason=" + reason + System.lineSeparator() +
                        "elapsedMs=" + (System.currentTimeMillis() - start) + System.lineSeparator());
                return;
            }
        } else {
            stableSince = -1L;
        }

        prev = cur;
    }

    saveStepPng("stabilize_timeout_last.png", prev);
    throw new RuntimeException("UI did not stabilize within timeoutMs=" + opt.timeoutMs + " (reason=" + reason + ")");
}

public void stabilizeAttempt() {
    try {
        StabilityOptions opt = runOptions.stability();
        if (opt == null || !opt.enabled) return;

        if (opt.bringToFront) {
            WindowManager.bringToFront(hwnd);
        }
        driver.delay(80); // tiny settle, no artifacts
    } catch (Exception ignored) {
        // never fail locate attempts due to stabilizer
    }
}



}
