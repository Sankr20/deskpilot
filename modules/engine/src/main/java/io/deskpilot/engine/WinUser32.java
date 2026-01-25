package io.deskpilot.engine;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface WinUser32 extends StdCallLibrary {

    WinUser32 INSTANCE = Native.load("user32", WinUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean GetCursorPos(POINT pt);
    HWND WindowFromPoint(POINT pt);
    HWND GetForegroundWindow();
    HWND GetAncestor(HWND hWnd, int gaFlags);
    boolean GetWindowRect(HWND hWnd, RECT rect);
    int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);
    boolean SetForegroundWindow(HWND hWnd);
    boolean GetClientRect(HWND hWnd, RECT rect);
boolean ClientToScreen(HWND hWnd, POINT point);

}
