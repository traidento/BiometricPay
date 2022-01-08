package ws.mochaa.biometricpay.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;
import com.crossbowffs.remotepreferences.RemotePreferences;
import ws.mochaa.biometricpay.BuildConfig;

import ws.mochaa.biometricpay.Constant;
import ws.mochaa.biometricpay.util.log.L;

public class XPreferenceProvider extends RemotePreferenceProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".XPreferenceProvider";
    public static final String PREF_NAME = "main_prefs";

    private static volatile SharedPreferences sSharedPreferenceInstance;

    public static SharedPreferences getRemoteSharedPreference(Context context) {
        if (sSharedPreferenceInstance == null) {
            synchronized (XPreferenceProvider.class) {
                if (sSharedPreferenceInstance == null) {
                    sSharedPreferenceInstance = new RemotePreferences(context, AUTHORITY, PREF_NAME);
                }
            }
        }
        return sSharedPreferenceInstance;
    }

    public XPreferenceProvider() {
        super(AUTHORITY, new String[]{PREF_NAME});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        String callingPackage = getCallingPackage();
        L.e("callingPackage", callingPackage);
        switch (callingPackage) {
            case Constant.PACKAGE_NAME_WECHAT:
            case Constant.PACKAGE_NAME_ALIPAY:
            case Constant.PACKAGE_NAME_TAOBAO:
            case Constant.PACKAGE_NAME_QQ:
            case BuildConfig.APPLICATION_ID:
                return true;
        }
        return false;
    }
}