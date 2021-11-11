package com.surcumference.fingerprint.network.updateCheck;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Toast;

import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.PluginTarget;
import com.surcumference.fingerprint.bean.PluginType;
import com.surcumference.fingerprint.bean.UpdateInfo;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.network.updateCheck.github.GithubUpdateChecker;
import com.surcumference.fingerprint.plugin.PluginApp;
import com.surcumference.fingerprint.util.ApplicationUtils;
import com.surcumference.fingerprint.util.Config;
import com.surcumference.fingerprint.util.FileUtils;
import com.surcumference.fingerprint.util.Task;
import com.surcumference.fingerprint.util.log.L;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by Jason on 2017/9/10.
 */

public class UpdateFactory {

    public static void doUpdateCheck(final Context context) {
        doUpdateCheck(context, true, false);
    }

    public static void doUpdateCheck(final Context context, final boolean quite, final boolean dontSkip) {
        if (!quite) {
            Toast.makeText(context, Lang.getString(R.id.toast_checking_update), Toast.LENGTH_LONG).show();
        }
        try {
            String packageName = context.getPackageName();
            String fileName = PluginApp.runActionBaseOnCurrentPluginType(new HashMap<PluginType, Callable<String>>() {{
                put(PluginType.Magisk, () -> packageName + ".zip");
                put(PluginType.Xposed, () -> packageName + ".apk");
            }});
            File targetFile = FileUtils.getSharableFile(context, fileName);
            FileUtils.delete(targetFile);
            new GithubUpdateChecker(new UpdateResultListener() {
                @Override
                public void onNoUpdate() {
                    if (!quite) {
                        Toast.makeText(context, Lang.getString(R.id.toast_no_update), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onNetErr() {
                    if (!quite) {
                        Toast.makeText(context, Lang.getString(R.id.toast_check_update_fail_net_err), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onHasUpdate(UpdateInfo updateInfo) {
                    // Removed
                }
            }).doUpdateCheck();
        } catch (Exception | Error e) {
            //for OPPO R11 Plus 6.0 NoSuchFieldError: No instance field mResultListener
            L.e(e);
        }
    }

    private static Map<PluginTarget, File> matchMagiskModuleFileListToPluginTarget(@Nullable File[] moduleZipFiles) {
        Map<PluginTarget, File> map = new HashMap<>();
        if (moduleZipFiles == null) {
            return map;
        }
        PluginApp.iterateAllPluginTarget(pluginTarget -> {
            for (File file : moduleZipFiles) {
                if (file.getName().contains(pluginTarget.name().toLowerCase())) {
                    map.put(pluginTarget, file);
                    return;
                }
            }
        });
        return map;
    }

    public static void lazyUpdateWhenActivityAlive() {
        int lazyCheckTimeMsec = BuildConfig.DEBUG ? 200 : 6000;
        Task.onMain(lazyCheckTimeMsec, new Runnable() {
            @Override
            public void run() {
                Activity activity = ApplicationUtils.getCurrentActivity();
                if (activity == null) {
                    Task.onMain(lazyCheckTimeMsec, this);
                    return;
                }
                UpdateFactory.doUpdateCheck(activity);
            }
        });
    }

    private static boolean isSkipVersion(Context context, String targetVersion) {
        Config config = Config.from(context);
        String skipVersion = config.getSkipVersion();
        if (TextUtils.isEmpty(skipVersion)) {
            return false;
        }
        if (String.valueOf(targetVersion).equals(skipVersion)) {
            return true;
        }
        return false;
    }

    public static void installApk(Context context, File file) {
        Uri uri = FileUtils.getUri(context, file);
        file.setReadable(true, false);
        file.getParentFile().setReadable(true, false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        Task.onMain(() -> context.startActivity(intent));
    }
}
