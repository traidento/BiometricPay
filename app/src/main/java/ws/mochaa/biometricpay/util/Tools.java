package ws.mochaa.biometricpay.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.UserManager;

import java.lang.reflect.Method;

import ws.mochaa.biometricpay.util.log.L;

/**
 * Created by Jason on 2017/11/1.
 */

public class Tools {

    public static void doUnSupportVersionUpload(Context context, String message) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;

            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("UnSupport: versionName:").append(versionName)
                    .append(" versionCode:").append(versionCode)
                    .append(" viewInfos:").append(message);

            L.e(stringBuffer);
        } catch (Exception e) {
            L.e(e);
        }
    }

    public static boolean isCurrentUserOwner(Context context) {
        try {
            Method getUserHandle = UserManager.class.getMethod("getUserHandle");
            int userHandle = (Integer) getUserHandle.invoke(context.getSystemService(Context.USER_SERVICE));
            return userHandle == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String bundle2string(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        StringBuilder string = new StringBuilder("Bundle{");
        for (String key : bundle.keySet()) {
            string.append(" ").append(key).append(" => ").append(bundle.get(key)).append(";");
        }
        string.append(" }Bundle");
        return string.toString();
    }
}
