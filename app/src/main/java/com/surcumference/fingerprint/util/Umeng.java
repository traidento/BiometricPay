package com.surcumference.fingerprint.util;

import android.content.Context;

import com.surcumference.fingerprint.util.log.L;

/**
 * Created by Jason on 2017/9/11.
 */

public class Umeng {

    private static Context sContext;

    public static void init(Context context) {
        sContext = context;
    }

    public static void reportError(String message) {
        // Removed
    }

    public static void onResume(Context context) {
        // Removed
    }

    public static void onPause(Context context) {
        // Removed
    }
}
