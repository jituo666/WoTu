package com.wotu.util;

import android.os.SystemClock;

//
// The animation time should ideally be the vsync time the frame will be
// displayed, but that is an unknown time in the future. So we use the system
// time just after eglSwapBuffers (when GLSurfaceView.onDrawFrame is called)
// as a approximation.
//
public class AnimTimer {
    private static volatile long sTime;

    // Sets current time as the animation time.
    public static void update() {
        sTime = SystemClock.uptimeMillis();
    }

    // Returns the animation time.
    public static long get() {
        return sTime;
    }

    public static long startTime() {
        sTime = SystemClock.uptimeMillis();
        return sTime;
    }
}
