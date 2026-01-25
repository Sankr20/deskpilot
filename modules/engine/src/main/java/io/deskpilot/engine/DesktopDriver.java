package io.deskpilot.engine;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class DesktopDriver {

    private final Robot robot;

    public DesktopDriver() {
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(20);
            this.robot.setAutoWaitForIdle(true);
        } catch (AWTException e) {
            throw new RuntimeException("Failed to initialize Robot", e);
        }
    }

    // -------------------------
    // SCREENSHOTS
    // -------------------------

    public BufferedImage screenshotFullScreen() {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle r = new Rectangle(0, 0, d.width, d.height);
        return robot.createScreenCapture(r);
    }

    public BufferedImage screenshot(Rectangle region) {
        return robot.createScreenCapture(region);
    }

    // -------------------------
    // MOUSE ACTIONS
    // -------------------------

    /** Move mouse in ROBOT coordinates */
    public void moveMouse(Point robotPoint) {
        robot.mouseMove(robotPoint.x, robotPoint.y);
    }

    /** Low-level left click in ROBOT coordinates */
    public void clickLeft(Point robotPoint) {
        moveMouse(robotPoint);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /** Click a WIN32 screen point (converted to Robot internally) */
    public void clickWin32(Point win32Point) {
        if (win32Point == null)
            throw new IllegalArgumentException("win32Point is null");

        Point robotPoint = RobotCoords.toRobotPoint(win32Point);
        clickLeft(robotPoint);
    }

    /** Convenience alias used by Target abstraction */
    public void click(Point win32Point) {
        clickWin32(win32Point);
    }

public void selectAll() {
    keyPress(KeyEvent.VK_CONTROL);
    keyTap(KeyEvent.VK_A);
    keyRelease(KeyEvent.VK_CONTROL);
    robot.delay(40);
}


    // -------------------------
    // NORMALIZED CLICKS
    // -------------------------

    /**
     * Canonical click:
     * normalized (0..1) within CLIENT RECT (WIN32)
     * returns the WIN32 click point
     */
    public Point clickNormalized(Rectangle clientRectWin32, double xPct, double yPct) {
        Point win32Point = NormalizedRegion.toScreenPoint(xPct, yPct, clientRectWin32);
        clickWin32(win32Point);
        return win32Point;
    }

    /** Backward-compatible alias */
    public Point clickClientNormalized(Rectangle clientRectWin32, double xPct, double yPct) {
        return clickNormalized(clientRectWin32, xPct, yPct);
    }

    /** Click inside a NormalizedRegion using region-relative normalized coords */
    public Point clickRegionNormalized(Rectangle clientRectWin32,
                                       NormalizedRegion region,
                                       double rxPct,
                                       double ryPct) {

        double xPct = region.xPct + (region.wPct * clamp01(rxPct));
        double yPct = region.yPct + (region.hPct * clamp01(ryPct));
        return clickNormalized(clientRectWin32, xPct, yPct);
    }

    private static double clamp01(double v) {
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    // -------------------------
    // KEYBOARD HELPERS
    // -------------------------

    public void delay(int ms) {
        robot.delay(ms);
    }

    public void keyPress(int keyCode) {
        robot.keyPress(keyCode);
    }

    public void keyRelease(int keyCode) {
        robot.keyRelease(keyCode);
    }

    public void keyTap(int keyCode) {
        robot.keyPress(keyCode);
        robot.keyRelease(keyCode);
    }

    public void keyCombo(int modifierKey, int key) {
        robot.keyPress(modifierKey);
        robot.keyPress(key);
        robot.keyRelease(key);
        robot.keyRelease(modifierKey);
    }

    // -------------------------
    // TEXT INPUT
    // -------------------------

    /**
     * Pastes text using clipboard + Ctrl+V.
     * Saves and restores previous clipboard content (best effort).
     */
    public void pasteText(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        Transferable old = null;
        try {
            old = clipboard.getContents(null);
        } catch (Exception ignored) {}

        try {
            clipboard.setContents(new StringSelection(text), null);
            robot.delay(50);
            keyCombo(KeyEvent.VK_CONTROL, KeyEvent.VK_V);
            robot.delay(50);
        } finally {
            if (old != null) {
                try {
                    clipboard.setContents(old, null);
                } catch (Exception ignored) {}
            }
        }
    }

    /** Types text character-by-character (fallback when paste fails) */
    public void typeText(String text) {
        if (text == null || text.isEmpty()) return;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            typeCharSmart(ch);
            robot.delay(8);
        }
    }

    private void typeCharSmart(char ch) {
        switch (ch) {
            case '\n': keyTap(KeyEvent.VK_ENTER); return;
            case '\t': keyTap(KeyEvent.VK_TAB); return;
            case '\b': keyTap(KeyEvent.VK_BACK_SPACE); return;
            case ' ':  keyTap(KeyEvent.VK_SPACE); return;
        }

        if (Character.isLetter(ch)) {
            boolean upper = Character.isUpperCase(ch);
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(Character.toUpperCase(ch));
            if (keyCode == KeyEvent.VK_UNDEFINED) {
                pasteText(String.valueOf(ch));
                return;
            }
            if (upper) robot.keyPress(KeyEvent.VK_SHIFT);
            keyTap(keyCode);
            if (upper) robot.keyRelease(KeyEvent.VK_SHIFT);
            return;
        }

        KeyStroke ks = mapPunctuationOrDigit(ch);
        if (ks != null) {
            if (ks.shift) robot.keyPress(KeyEvent.VK_SHIFT);
            keyTap(ks.keyCode);
            if (ks.shift) robot.keyRelease(KeyEvent.VK_SHIFT);
            return;
        }

        int keyCode = KeyEvent.getExtendedKeyCodeForChar(ch);
        if (keyCode != KeyEvent.VK_UNDEFINED) {
            keyTap(keyCode);
            return;
        }

        pasteText(String.valueOf(ch));
    }

    private static final class KeyStroke {
        final int keyCode;
        final boolean shift;
        KeyStroke(int keyCode, boolean shift) {
            this.keyCode = keyCode;
            this.shift = shift;
        }
    }

    /**
     * US keyboard mapping for common punctuation.
     * Good enough for most enterprise apps.
     */
    private KeyStroke mapPunctuationOrDigit(char ch) {
        if (ch >= '0' && ch <= '9') {
            return new KeyStroke(KeyEvent.VK_0 + (ch - '0'), false);
        }

        switch (ch) {
            case '-': return new KeyStroke(KeyEvent.VK_MINUS, false);
            case '=': return new KeyStroke(KeyEvent.VK_EQUALS, false);
            case '[': return new KeyStroke(KeyEvent.VK_OPEN_BRACKET, false);
            case ']': return new KeyStroke(KeyEvent.VK_CLOSE_BRACKET, false);
            case '\\': return new KeyStroke(KeyEvent.VK_BACK_SLASH, false);
            case ';': return new KeyStroke(KeyEvent.VK_SEMICOLON, false);
            case '\'': return new KeyStroke(KeyEvent.VK_QUOTE, false);
            case ',': return new KeyStroke(KeyEvent.VK_COMMA, false);
            case '.': return new KeyStroke(KeyEvent.VK_PERIOD, false);
            case '/': return new KeyStroke(KeyEvent.VK_SLASH, false);
            case '`': return new KeyStroke(KeyEvent.VK_BACK_QUOTE, false);

            case '_': return new KeyStroke(KeyEvent.VK_MINUS, true);
            case '+': return new KeyStroke(KeyEvent.VK_EQUALS, true);
            case '{': return new KeyStroke(KeyEvent.VK_OPEN_BRACKET, true);
            case '}': return new KeyStroke(KeyEvent.VK_CLOSE_BRACKET, true);
            case '|': return new KeyStroke(KeyEvent.VK_BACK_SLASH, true);
            case ':': return new KeyStroke(KeyEvent.VK_SEMICOLON, true);
            case '"': return new KeyStroke(KeyEvent.VK_QUOTE, true);
            case '<': return new KeyStroke(KeyEvent.VK_COMMA, true);
            case '>': return new KeyStroke(KeyEvent.VK_PERIOD, true);
            case '?': return new KeyStroke(KeyEvent.VK_SLASH, true);
            case '~': return new KeyStroke(KeyEvent.VK_BACK_QUOTE, true);

            case '!': return new KeyStroke(KeyEvent.VK_1, true);
            case '@': return new KeyStroke(KeyEvent.VK_2, true);
            case '#': return new KeyStroke(KeyEvent.VK_3, true);
            case '$': return new KeyStroke(KeyEvent.VK_4, true);
            case '%': return new KeyStroke(KeyEvent.VK_5, true);
            case '^': return new KeyStroke(KeyEvent.VK_6, true);
            case '&': return new KeyStroke(KeyEvent.VK_7, true);
            case '*': return new KeyStroke(KeyEvent.VK_8, true);
            case '(': return new KeyStroke(KeyEvent.VK_9, true);
            case ')': return new KeyStroke(KeyEvent.VK_0, true);
        }

        return null;
    }
}
