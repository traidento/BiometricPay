package ws.mochaa.biometricpay.plugin.xposed;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ws.mochaa.biometricpay.BuildConfig;
import ws.mochaa.biometricpay.plugin.AlipayBasePlugin;
import ws.mochaa.biometricpay.util.bugfixer.xposed.XposedLogNPEBugFixer;
import ws.mochaa.biometricpay.util.log.L;

/**
 * Created by Jason on 2017/9/8.
 */

public class AlipayPlugin extends AlipayBasePlugin {

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            XposedHelpers.findAndHookMethod(AppCompatActivity.class, "onCreate", Bundle.class, new XC_MethodHook() {

                @TargetApi(21)
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    onActivityCreated((AppCompatActivity) param.thisObject);
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }
}
