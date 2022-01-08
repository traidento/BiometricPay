package ws.mochaa.biometricpay.bean;

import ws.mochaa.biometricpay.Lang;
import ws.mochaa.biometricpay.R;

public enum PluginTarget {
    WeChat(Lang.getString(R.id.settings_title_wechat)),
    Alipay(Lang.getString(R.id.settings_title_alipay)),
    Taobao(Lang.getString(R.id.settings_title_taobao));

    private final String mAppName;

    PluginTarget(String appName) {
        mAppName = appName;
    }

    public String getAppName() {
        return mAppName;
    }
}