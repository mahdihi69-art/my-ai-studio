package com.mahdihi69.dastansaz;

/** Compatibility shim for legacy code that references Build.VERSION. */
public final class Build {
    private Build() {}
    public static final class VERSION {
        public static final int SDK_INT = android.os.Build.VERSION.SDK_INT;
        private VERSION() {}
    }
}
