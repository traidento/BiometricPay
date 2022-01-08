package ws.mochaa.biometricpay.plugin.xposed;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ws.mochaa.biometricpay.BuildConfig;
import ws.mochaa.biometricpay.plugin.WeChatBasePlugin;
import ws.mochaa.biometricpay.util.Tools;
import ws.mochaa.biometricpay.util.log.L;

/**
 * Created by Jason on 2017/9/8.
 */

public class WeChatPlugin extends WeChatBasePlugin {

    @Keep
    public void main(final Context context, final XC_LoadPackage.LoadPackageParam lpparam) {
        L.d("Xposed plugin init version: " + BuildConfig.VERSION_NAME);
        try {
            //for multi user
            if (!Tools.isCurrentUserOwner(context)) {
                XposedHelpers.findAndHookMethod(UserHandle.class, "getUserId", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (mMockCurrentUser) {
                            param.setResult(0);
                        }
                    }
                });
            }
            XposedHelpers.findAndHookMethod(AppCompatActivity.class, "onResume", new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
                    onActivityResumed((AppCompatActivity) param.thisObject);
                }
            });

            XposedHelpers.findAndHookMethod(AppCompatActivity.class, "onPause", new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
                    onActivityPaused((AppCompatActivity) param.thisObject);
                }
            });
        } catch (Throwable l) {
            XposedBridge.log(l);
        }
    }
}
