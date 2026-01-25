package io.deskpilot.engine;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;


public interface User32Ex extends StdCallLibrary {

    User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

    HWND WindowFromPoint(POINT point);

    boolean GetWindowRect(HWND hWnd, RECT rect);

    HWND GetAncestor(HWND hWnd, int gaFlags);

    int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);

    boolean SetForegroundWindow(HWND hWnd);
    boolean GetCursorPos(POINT pt);

HWND GetParent(HWND hWnd);

int GetWindowLong(HWND hWnd, int nIndex);
boolean GetClientRect(HWND hWnd, RECT rect);
boolean ClientToScreen(HWND hWnd, POINT point);


}
