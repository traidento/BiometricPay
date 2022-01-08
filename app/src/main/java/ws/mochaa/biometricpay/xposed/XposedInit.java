package ws.mochaa.biometricpay.xposed;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import ws.mochaa.biometricpay.BuildConfig;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ws.mochaa.biometricpay.Constant;
import ws.mochaa.biometricpay.plugin.xposed.AlipayPlugin;
import ws.mochaa.biometricpay.plugin.xposed.TaobaoPlugin;
import ws.mochaa.biometricpay.plugin.xposed.WeChatPlugin;
import ws.mochaa.biometricpay.util.log.L;
import ws.mochaa.biometricpay.xposed.loader.XposedPluginLoader;


public class XposedInit implements IXposedHookZygoteInit, IXposedHookLoadPackage {


    public void initZygote(StartupParam startupParam) throws Throwable {
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (Constant.PACKAGE_NAME_WECHAT.equals(lpparam.packageName)) {
            initWechat(lpparam);
        } else if (Constant.PACKAGE_NAME_ALIPAY.equals(lpparam.packageName)) {
            initAlipay(lpparam);
        } else if (Constant.PACKAGE_NAME_TAOBAO.equals(lpparam.packageName)) {
            initTaobao(lpparam);
        }
        initGeneric(lpparam);
    }

    private void initWechat(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                Context context = (Context) param.args[0];
                XposedPluginLoader.load(WeChatPlugin.class, context, lpparam);
            }
        });
    }

    private void initAlipay(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            private boolean mCalled = false;

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                if (!mCalled) {
                    mCalled = true;
                    Context context = (Context) param.args[0];
                    XposedPluginLoader.load(AlipayPlugin.class, context, lpparam);
                }
            }
        });
    }

    private void initTaobao(final LoadPackageParam lpparam) {
        L.d("loaded: [" + lpparam.packageName + "]" + " version:" + BuildConfig.VERSION_NAME);
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            //受Atlas影响Application onCreate入口只需执行一次即可
            private boolean mCalled = false;

            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                L.d("Application onCreate");
                if (mCalled == false) {
                    mCalled = true;
                    Context context = (Context) param.args[0];
                    if (context == null) {
                        L.d("context eq null what the hell.");
                        return;
                    }
                    XposedPluginLoader.load(TaobaoPlugin.class, context, lpparam);
                }
            }
        });
    }

    private void initGeneric(final LoadPackageParam lpparam) {
        //for multi user
        if ("android".equals(lpparam.processName)
                || Constant.PACKAGE_NAME_WECHAT.equals(lpparam.packageName)
                || Constant.PACKAGE_NAME_QQ.equals(lpparam.packageName)) {
            XposedHelpers.findAndHookMethod(ActivityManager.class, "checkComponentPermission", String.class, int.class, int.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String permission = (String) param.args[0];
                    if (TextUtils.isEmpty(permission)) {
                        return;
                    }
                    if (!permission.contains("MANAGE_USERS")) {
                        return;
                    }
                    param.setResult(PackageManager.PERMISSION_GRANTED);
                }
            });
        }
    }
}