package io.deskpilot.engine;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public final class DpiAwareness {

    private interface User32 extends StdCallLibrary {
        User32 INSTANCE = Native.load("user32", User32.class, W32APIOptions.DEFAULT_OPTIONS);

        int SetProcessDPIAware();
    }

    private interface Shcore extends StdCallLibrary {
        Shcore INSTANCE = Native.load("shcore", Shcore.class);

        int SetProcessDpiAwareness(int value);
    }

    // Windows 8.1+
    private static final int PROCESS_PER_MONITOR_DPI_AWARE = 2;

    public static void enable() {
        try {
            Shcore.INSTANCE.SetProcessDpiAwareness(PROCESS_PER_MONITOR_DPI_AWARE);
        } catch (Throwable t) {
            // Fallback for older Windows
            User32.INSTANCE.SetProcessDPIAware();
        }
    }
}
