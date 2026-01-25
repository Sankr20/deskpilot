package io.deskpilot.engine;

import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;

import java.awt.Rectangle;

public class EnginePickPoint {

    public static void main(String[] args) throws Exception {
        DpiAwareness.enable();

        System.out.println("Move mouse to the exact target point INSIDE the input field...");
        System.out.println("Click on the target window in 5 seconds...");
        Thread.sleep(5000);

        HWND hwnd = WindowManager.pickWindowHandle();
        hwnd = WindowManager.toTopLevel(hwnd);

        String title = WindowManager.getWindowTitle(hwnd);
        Rectangle client = WindowManager.getClientRectOnScreenOrThrow(hwnd);

        System.out.println("Selected window: " + title);
        System.out.println("Client rect (win32): " + client);

        // Read current mouse cursor position (win32)
        POINT pt = new POINT();
        WinUser32.INSTANCE.GetCursorPos(pt);

        int relX = pt.x - client.x;
        int relY = pt.y - client.y;

        double xPct = relX / (double) client.width;
        double yPct = relY / (double) client.height;

        System.out.printf("Mouse (win32): (%d,%d)%n", pt.x, pt.y);
        System.out.printf("Relative to client: (%d,%d)%n", relX, relY);
        System.out.printf("Normalized: xPct=%.6f, yPct=%.6f%n", xPct, yPct);
    }
}
