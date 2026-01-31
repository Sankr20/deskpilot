package io.deskpilot.engine;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;

import java.awt.Point;
import java.awt.Rectangle;

public class WindowManager {

    private static final int GA_ROOTOWNER = 3;

    /** Pick window under cursor; fallback to foreground window. */
  public static HWND pickWindowHandle() {
    POINT pt = new POINT();
    if (WinUser32.INSTANCE.GetCursorPos(pt)) {
        HWND h = WinUser32.INSTANCE.WindowFromPoint(pt);
        if (h != null) return toTopLevel(h);   // ✅ always lift
    }
    return toTopLevel(WinUser32.INSTANCE.GetForegroundWindow());
}


    /** Convert any child HWND to a top-level/root owner window. */
    public static HWND toTopLevel(HWND hwnd) {
        if (hwnd == null) return null;
        HWND top = WinUser32.INSTANCE.GetAncestor(hwnd, GA_ROOTOWNER);
        return (top != null) ? top : hwnd;
    }

    public static Rectangle getWindowRectOrThrow(HWND hwnd) {
        if (hwnd == null) {
            throw new RuntimeException("HWND is null (could not pick window). lastError=" + Kernel32.INSTANCE.GetLastError());
        }

        RECT r = new RECT();
        boolean ok = WinUser32.INSTANCE.GetWindowRect(hwnd, r);
        if (!ok) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("GetWindowRect failed. lastError=" + err + " hwnd=" + hwnd);
        }

        int w = r.right - r.left;
        int h = r.bottom - r.top;
        if (w <= 0 || h <= 0) {
            throw new RuntimeException("Invalid window rect: left=" + r.left + " top=" + r.top + " right=" + r.right + " bottom=" + r.bottom);
        }
        return new Rectangle(r.left, r.top, w, h);
    }

    public static Rectangle getClientRectOnScreenOrThrow(HWND hwnd) {
        if (hwnd == null) throw new RuntimeException("HWND is null (could not pick window).");

        RECT client = new RECT();
        boolean ok = WinUser32.INSTANCE.GetClientRect(hwnd, client);
        if (!ok) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("GetClientRect failed. lastError=" + err);
        }

        POINT topLeft = new POINT();
        topLeft.x = 0;
        topLeft.y = 0;

        ok = WinUser32.INSTANCE.ClientToScreen(hwnd, topLeft);
        if (!ok) {
            int err = Kernel32.INSTANCE.GetLastError();
            throw new RuntimeException("ClientToScreen failed. lastError=" + err);
        }

        int width = client.right - client.left;
        int height = client.bottom - client.top;

        if (width <= 0 || height <= 0) {
            throw new RuntimeException("Invalid client size: w=" + width + " h=" + height);
        }

        return new Rectangle(topLeft.x, topLeft.y, width, height);
    }

    public static Point getMousePositionWin32() {
        POINT p = new POINT();
        WinUser32.INSTANCE.GetCursorPos(p);
        return new Point(p.x, p.y);
    }

    public static HWND getForegroundWindow() {
        return WinUser32.INSTANCE.GetForegroundWindow();
    }

    public static String getWindowTitle(HWND hwnd) {
        if (hwnd == null) return "";
        char[] buffer = new char[1024];
        WinUser32.INSTANCE.GetWindowTextW(hwnd, buffer, buffer.length);
        return Native.toString(buffer).trim();
    }

    public static void bringToFront(HWND hwnd) {
        if (hwnd != null) WinUser32.INSTANCE.SetForegroundWindow(hwnd);
    }

 public static HWND pickWindowHandleOnClick(long timeoutMs) {
    long deadline = System.currentTimeMillis() + timeoutMs;

    // 1) ensure we start from released state
    while (System.currentTimeMillis() < deadline) {
        if (!isLeftMouseDown()) break;
        sleep(15);
    }

    // 2) wait for mouse-down
    POINT downPt = new POINT();
    while (System.currentTimeMillis() < deadline) {
        if (isLeftMouseDown()) {
            WinUser32.INSTANCE.GetCursorPos(downPt);
            break;
        }
        sleep(15);
    }

    if (downPt.x == 0 && downPt.y == 0 && System.currentTimeMillis() >= deadline) {
        throw new RuntimeException("Attach cancelled: timed out waiting for click (" + timeoutMs + "ms)");
    }

    // 3) wait for mouse-up (so the click completes and foreground changes)
    while (System.currentTimeMillis() < deadline) {
        if (!isLeftMouseDown()) break;
        sleep(15);
    }

    // 4) let Windows process activation
    sleep(80);

    // 5) prefer foreground (post-click), fallback to window-from-point
    HWND fg = WinUser32.INSTANCE.GetForegroundWindow();
    HWND fromPoint = WinUser32.INSTANCE.WindowFromPoint(downPt);

    HWND picked = null;

    if (fg != null) {
        picked = toTopLevel(fg);
    }
    if (picked == null && fromPoint != null) {
        picked = toTopLevel(fromPoint);
    }
    if (picked == null) {
        picked = toTopLevel(WinUser32.INSTANCE.GetForegroundWindow());
    }
    return picked;
}


private static boolean isLeftMouseDown() {
    final int VK_LBUTTON = 0x01;
    // High-order bit set means key is down
    return (WinUser32.INSTANCE.GetAsyncKeyState(VK_LBUTTON) & 0x8000) != 0;
}

private static void sleep(long ms) {
    try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
}

    
}
