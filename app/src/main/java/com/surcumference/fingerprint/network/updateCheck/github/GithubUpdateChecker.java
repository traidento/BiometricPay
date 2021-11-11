package com.surcumference.fingerprint.network.updateCheck.github;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.surcumference.fingerprint.BuildConfig;
import com.surcumference.fingerprint.Constant;
import com.surcumference.fingerprint.Lang;
import com.surcumference.fingerprint.R;
import com.surcumference.fingerprint.bean.UpdateInfo;
import com.surcumference.fingerprint.network.inf.UpdateResultListener;
import com.surcumference.fingerprint.network.updateCheck.BaseUpdateChecker;
import com.surcumference.fingerprint.network.updateCheck.github.bean.GithubAssetsInfo;
import com.surcumference.fingerprint.network.updateCheck.github.bean.GithubLatestInfo;
import com.surcumference.fingerprint.util.DateUtils;
import com.surcumference.fingerprint.util.StringUtils;
import com.surcumference.fingerprint.util.log.L;

import java.io.IOException;
import java.util.Date;

/**
 * Created by Jason on 2017/9/9.
 */

public class GithubUpdateChecker extends BaseUpdateChecker {
    public GithubUpdateChecker(UpdateResultListener listener) {
        super(listener);
    }

    @Override
    public void doUpdateCheck() {
        // Removed
        onNoUpdate();
    }
}
