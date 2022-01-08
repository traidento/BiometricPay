package ws.mochaa.biometricpay.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ws.mochaa.biometricpay.util.log.L;

public class ActivityViewObserver {

    private final WeakReference<AppCompatActivity> mActivityRef;
    private boolean mRunning = false;
    private final String mViewIdentifier;

    public ActivityViewObserver(AppCompatActivity weakRefActivity, String viewIdentifier) {
        this.mActivityRef = new WeakReference<>(weakRefActivity);
        this.mViewIdentifier = viewIdentifier;
    }

    public void start(long loopMSec, IActivityViewListener listener) {
        if (mRunning) {
            return;
        }
        mRunning = true;
        task(loopMSec, listener);
    }

    public void stop() {
        mRunning = false;
    }

    private void task(long loopMSec, IActivityViewListener listener) {
        if (!mRunning) {
            return;
        }
        AppCompatActivity activity = mActivityRef.get();
        if (activity == null) {
            mRunning = false;
            return;
        }
        if (activity.isFinishing()) {
            mRunning = false;
            return;
        }
        if (activity.isDestroyed()) {
            mRunning = false;
            return;
        }

        List<View> viewList = new ArrayList<>();
        List<View> decorViewList = ViewUtils.getWindowManagerViews();
        for (View decorView : decorViewList) {
            if (decorView instanceof ViewGroup) {
            } else {
                continue;
            }
            ViewUtils.getChildViewsByType((ViewGroup) decorView, this.mViewIdentifier, viewList);
            if (viewList.size() > 0) {
                break;
            }
        }
        if (viewList.size() > 0) {
            for (View targetView : viewList) {
                if (ViewUtils.isViewVisibleInScreen(targetView.getRootView())) {
                    onViewFounded(listener, targetView);
                }
            }
        }
        Task.onMain(loopMSec, () -> task(loopMSec, listener));
    }

    private void onViewFounded(IActivityViewListener listener, View view) {
        try {
            listener.onViewFounded(this, view);
        } catch (Exception e) {
            L.e(e);
        }
    }

    public interface IActivityViewListener {
        void onViewFounded(ActivityViewObserver observer, View view);
    }
}
