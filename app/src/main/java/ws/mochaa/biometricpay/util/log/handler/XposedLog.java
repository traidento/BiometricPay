package ws.mochaa.biometricpay.util.log.handler;

import de.robv.android.xposed.XposedBridge;
import ws.mochaa.biometricpay.util.log.inf.ILog;

/**
 * Created by Jason on 2017/9/10.
 */

public class XposedLog implements ILog {

    @Override
    public void debug(String tag, String msg) {
        XposedBridge.log(tag + " " + msg);
    }

    @Override
    public void error(String tag, String msg) {
        XposedBridge.log(tag + " " + msg);
    }
}
