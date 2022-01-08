package ws.mochaa.biometricpay.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.WeakHashMap;

import ws.mochaa.biometricpay.BuildConfig;
import ws.mochaa.biometricpay.util.log.L;

/**
 * Created by Jason on 2017/9/9.
 */

public class Config {


    private static final WeakHashMap<Context, ObjectCache> sConfigCache = new WeakHashMap<>();

    public static Config from(Context context) {
        return new Config(context);
    }

    private ObjectCache mCache;

    private Config(Context context) {
        if (sConfigCache.containsKey(context)) {
            mCache = sConfigCache.get(context);
        }
        if (mCache == null) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID + ".settings", Context.MODE_PRIVATE);
            SharedPreferences mainAppSharePreference;
            try {
                mainAppSharePreference = XPreferenceProvider.getRemoteSharedPreference(context);
            } catch (Exception e) {
                mainAppSharePreference = sharedPreferences;
                L.e(e);
            }
            mCache = new ObjectCache(sharedPreferences, mainAppSharePreference);
            sConfigCache.put(context, mCache);
        }
    }

    public boolean isOn() {
        return mCache.sharedPreferences.getBoolean("switch_on1", false);
    }

    public void setOn(boolean on) {
        mCache.sharedPreferences.edit().putBoolean("switch_on1", on).apply();
    }

    @Nullable
    public String getPassword() {
        String password = mCache.sharedPreferences.getString("password", null);
        if (TextUtils.isEmpty(password)) {
            return null;
        }
        return password;
    }

    public void setPassword(String password) {
        mCache.sharedPreferences.edit().putString("password", password).apply();
    }

    private class ObjectCache {
        SharedPreferences sharedPreferences;
        SharedPreferences mainAppSharedPreferences;

        public ObjectCache(SharedPreferences sharedPreferences, SharedPreferences mainAppSharedPreferences) {
            this.sharedPreferences = sharedPreferences;
            this.mainAppSharedPreferences = mainAppSharedPreferences;
        }
    }
}
