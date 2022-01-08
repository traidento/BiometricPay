package ws.mochaa.biometricpay.plugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

import ws.mochaa.biometricpay.BuildConfig;
import ws.mochaa.biometricpay.Constant;
import ws.mochaa.biometricpay.Lang;
import ws.mochaa.biometricpay.R;
import ws.mochaa.biometricpay.util.ActivityViewObserver;
import ws.mochaa.biometricpay.util.ApplicationUtils;
import ws.mochaa.biometricpay.util.Config;
import ws.mochaa.biometricpay.util.DpUtils;
import ws.mochaa.biometricpay.util.ImageUtils;
import ws.mochaa.biometricpay.util.NotifyUtils;
import ws.mochaa.biometricpay.util.StyleUtils;
import ws.mochaa.biometricpay.util.Task;
import ws.mochaa.biometricpay.util.ViewUtils;
import ws.mochaa.biometricpay.util.drawable.XDrawable;
import ws.mochaa.biometricpay.util.log.L;
import ws.mochaa.biometricpay.util.paydialog.WeChatPayDialog;
import ws.mochaa.biometricpay.view.SettingsView;

public class WeChatBasePlugin {

    private ActivityViewObserver mActivityViewObserver;
    private final WeakHashMap<View, View.OnAttachStateChangeListener> mView2OnAttachStateChangeListenerMap = new WeakHashMap<>();
    protected boolean mMockCurrentUser = false;

    protected synchronized void initFingerPrintLock(Context context, Runnable onSuccessUnlockRunnable) {
        mMockCurrentUser = true;
        // use BiometricPrompt to authenticate
        BiometricManager mBiometricManager = BiometricManager.from(context);

        if (mBiometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS) {
            // If biometric authentication is available, then authenticate with BiometricPrompt.
            Executor mExecutor = ContextCompat.getMainExecutor(context);
            BiometricPrompt mBiometricPrompt = new BiometricPrompt((FragmentActivity) context, mExecutor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    L.d("onAuthenticationError: " + errorCode + " " + errString);
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    L.d("指纹识别成功");
                    onSuccessUnlockRunnable.run();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    L.d("多次尝试错误，请使用密码输入");
                    NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_retry_ended));
                }
            });

            // Show the biometric dialog
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("WeChat")
                    .setSubtitle("Verify Biometric to Pay")
                    .setConfirmationRequired(true)
                    .build();
            mBiometricPrompt.authenticate(promptInfo);
        } else {
            NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_enable));
        }
    }

    protected boolean isHeaderViewExistsFallback(ListView listView) {
        if (listView == null) {
            return false;
        }
        if (listView.getHeaderViewsCount() <= 0) {
            return false;
        }
        try {
            Field mHeaderViewInfosField = ListView.class.getDeclaredField("mHeaderViewInfos");
            mHeaderViewInfosField.setAccessible(true);
            ArrayList<ListView.FixedViewInfo> mHeaderViewInfos = (ArrayList<ListView.FixedViewInfo>) mHeaderViewInfosField.get(listView);
            if (mHeaderViewInfos != null) {
                for (ListView.FixedViewInfo viewInfo : mHeaderViewInfos) {
                    if (viewInfo.view == null) {
                        continue;
                    }
                    Object tag = viewInfo.view.getTag();
                    if (BuildConfig.APPLICATION_ID.equals(tag)) {
                        L.d("found plugin settings headerView");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            L.e(e);
        }
        return false;
    }

    protected void onActivityResumed(AppCompatActivity activity) {
        L.d("Activity onResume =", activity);
        final String activityClzName = activity.getClass().getName();
        if (activityClzName.contains("com.tencent.mm.plugin.setting.ui.setting.SettingsUI")
                || activityClzName.contains("com.tencent.mm.plugin.wallet.pwd.ui.WalletPasswordSettingUI")) {
            Task.onMain(100, () -> doSettingsMenuInject(activity));
        } else if (activityClzName.contains(".WalletPayUI")
                || activityClzName.contains(".UIPageFragmentActivity")) {
            stopAndRemoveCurrentActivityViewObserver();
            ActivityViewObserver activityViewObserver = new ActivityViewObserver(activity, ".EditHintPasswdView");
            activityViewObserver.start(100, new ActivityViewObserver.IActivityViewListener() {
                @Override
                public void onViewFounded(ActivityViewObserver observer, View view) {
                    ActivityViewObserver.IActivityViewListener l = this;
                    observer.stop();
                    L.d("onViewFounded:", view, " rootView: ", view.getRootView());

                    onPayDialogShown((ViewGroup) view.getRootView());
                    View.OnAttachStateChangeListener listener = mView2OnAttachStateChangeListenerMap.get(view);
                    if (listener != null) {
                        view.removeOnAttachStateChangeListener(listener);
                    }
                    listener = new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            L.d("onViewAttachedToWindow:", view);

                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            L.d("onViewDetachedFromWindow:", view);
                            Context context = v.getContext();
                            onPayDialogDismiss(context);
                            Task.onMain(500, () -> observer.start(100, l));
                        }
                    };
                    view.addOnAttachStateChangeListener(listener);
                    mView2OnAttachStateChangeListenerMap.put(view, listener);
                }
            });
            mActivityViewObserver = activityViewObserver;
        }
    }

    protected void onActivityPaused(AppCompatActivity activity) {
        try {
            L.d("Activity onPause =", activity);
            final String activityClzName = activity.getClass().getName();
            if (activityClzName.contains(".WalletPayUI")) {
                stopAndRemoveCurrentActivityViewObserver();
                onPayDialogDismiss(activity);
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    protected void onPayDialogShown(ViewGroup rootView) {
        L.d("PayDialog show");
        Context context = rootView.getContext();
        if (Config.from(context).isOn()) {
            WeChatPayDialog payDialogView = WeChatPayDialog.findFrom(rootView);
            L.d(payDialogView);
            if (payDialogView == null) {
                NotifyUtils.notifyVersionUnSupport(context, Constant.PACKAGE_NAME_WECHAT);
                return;
            }

            ViewGroup passwordLayout = payDialogView.passwordLayout;
            EditText mInputEditText = payDialogView.inputEditText;
            View keyboardView = payDialogView.keyboardView;
            TextView usePasswordText = payDialogView.usePasswordText;
            TextView titleTextView = payDialogView.titleTextView;

            RelativeLayout fingerPrintLayout = new RelativeLayout(context);
            fingerPrintLayout.setTag("fingerPrintLayout");
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            fingerPrintLayout.setLayoutParams(layoutParams);
            ImageView fingerprintImageView = new ImageView(context);

            try {
                final Bitmap bitmap = ImageUtils.base64ToBitmap(Constant.ICON_FINGER_PRINT_WECHAT_BASE64);
                fingerprintImageView.setImageBitmap(bitmap);
                fingerprintImageView.getViewTreeObserver().addOnWindowAttachListener(new ViewTreeObserver.OnWindowAttachListener() {
                    @Override
                    public void onWindowAttached() {

                    }

                    @Override
                    public void onWindowDetached() {
                        fingerprintImageView.getViewTreeObserver().removeOnWindowAttachListener(this);
                        try {
                            bitmap.recycle();
                        } catch (Exception ignored) {
                        }
                    }
                });
            } catch (OutOfMemoryError e) {
                L.d(e);
            }
            fingerPrintLayout.addView(fingerprintImageView);


            final Runnable switchToFingerprintRunnable = () -> {
                mInputEditText.setVisibility(View.GONE);
                keyboardView.setVisibility(View.GONE);
                View fingerPrintLayoutLast = passwordLayout.findViewWithTag("fingerPrintLayout");
                if (fingerPrintLayoutLast != null) {
                    passwordLayout.removeView(fingerPrintLayoutLast);
                }
                passwordLayout.addView(fingerPrintLayout);

                initFingerPrintLock(context, () -> {
                    Config config = Config.from(context);
                    String pwd = config.getPassword();
                    if (TextUtils.isEmpty(pwd)) {
                        Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_wechat), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mInputEditText.setText(pwd);
                });
                if (titleTextView != null) {
                    titleTextView.setText(Lang.getString(R.id.wechat_payview_fingerprint_title));
                }
                if (usePasswordText != null) {
                    usePasswordText.setText(Lang.getString(R.id.wechat_payview_password_switch_text));
                }
            };

            final Runnable switchToPasswordRunnable = () -> {
                passwordLayout.removeView(fingerPrintLayout);
                mInputEditText.setVisibility(View.VISIBLE);
                keyboardView.setVisibility(View.VISIBLE);
                mInputEditText.performClick();
                mMockCurrentUser = false;
                if (titleTextView != null) {
                    titleTextView.setText(Lang.getString(R.id.wechat_payview_password_title));
                }
                if (usePasswordText != null) {
                    usePasswordText.setText(Lang.getString(R.id.wechat_payview_fingerprint_switch_text));
                }
            };

            if (usePasswordText != null) {
                Task.onMain(() -> usePasswordText.setVisibility(View.VISIBLE));
                usePasswordText.setOnTouchListener((view, motionEvent) -> {
                    try {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                            if (mInputEditText.getVisibility() == View.GONE) {
                                switchToPasswordRunnable.run();
                            } else {
                                switchToFingerprintRunnable.run();
                            }
                        }
                    } catch (Exception e) {
                        L.e(e);
                    }
                    return true;
                });
            }

            fingerprintImageView.setOnClickListener(view -> switchToPasswordRunnable.run());
            switchToFingerprintRunnable.run();
        }
    }

    protected void onPayDialogDismiss(Context context) {
        L.d("PayDialog dismiss");
        if (Config.from(context).isOn()) {
            mMockCurrentUser = false;
        }
    }

    protected void stopAndRemoveCurrentActivityViewObserver() {
        ActivityViewObserver activityViewObserver = mActivityViewObserver;
        if (activityViewObserver != null) {
            activityViewObserver.stop();
            mActivityViewObserver = null;
        }
    }

    protected void doSettingsMenuInject(final AppCompatActivity activity) {
        int versionCode = ApplicationUtils.getPackageVersionCode(activity, Constant.PACKAGE_NAME_WECHAT);
        ListView itemView = (ListView) ViewUtils.findViewByName(activity, "android", "list");
        if (ViewUtils.findViewByText(itemView, Lang.getString(R.id.app_settings_name)) != null
                || isHeaderViewExistsFallback(itemView)) {
            return;
        }

        boolean isDarkMode = StyleUtils.isDarkMode(activity);

        LinearLayout settingsItemRootLLayout = new LinearLayout(activity);
        settingsItemRootLLayout.setOrientation(LinearLayout.VERTICAL);
        settingsItemRootLLayout.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        settingsItemRootLLayout.setPadding(0, DpUtils.dip2px(activity, 20), 0, 0);

        LinearLayout settingsItemLinearLayout = new LinearLayout(activity);
        settingsItemLinearLayout.setOrientation(LinearLayout.VERTICAL);

        settingsItemLinearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));


        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);

        itemHlinearLayout.setBackground(new XDrawable.Builder()
                .defaultColor(isDarkMode ? 0xFF191919 : Color.WHITE)
                .pressedColor(isDarkMode ? 0xFF1D1D1D : 0xFFE5E5E5)
                .create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtils.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        itemNameText.setTextColor(isDarkMode ? 0xFFD3D3D3 : 0xFF353535);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(DpUtils.dip2px(activity, 16), 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(isDarkMode ? 0xFF656565 : 0xFF999999);

        //try use WeChat style
        try {
            View generalView = ViewUtils.findViewByText(itemView, "通用", "一般", "General", "服务管理", "服務管理", "Manage Services");
            L.d("generalView", generalView);
            if (generalView instanceof TextView) {
                TextView generalTextView = (TextView) generalView;
                float scale = itemNameText.getTextSize() / generalTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, generalTextView.getTextSize());

                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                View generalItemView;
                if (versionCode >= 1380) { //7.0.0
                    generalItemView = (View) generalView.getParent().getParent().getParent().getParent().getParent();
                } else {
                    generalItemView = (View) generalView.getParent().getParent().getParent().getParent().getParent().getParent();
                }
                if (generalItemView != null) {
                    Drawable background = generalItemView.getBackground();
                    if (background != null) {
                        Drawable.ConstantState constantState = background.getConstantState();
                        if (constantState != null) {
                            itemHlinearLayout.setBackground(constantState.newDrawable());
                        }
                    }
                }
                itemNameText.setTextColor(generalTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (versionCode >= 1380) { //7.0.0
            View lineView = new View(activity);
            lineView.setBackgroundColor(isDarkMode ? 0xFF2E2E2E : 0xFFD5D5D5);
            settingsItemLinearLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
            settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 55)));
        } else {
            settingsItemLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 50)));
        }

        settingsItemRootLLayout.addView(settingsItemLinearLayout);
        settingsItemRootLLayout.setTag(BuildConfig.APPLICATION_ID);

        itemView.addHeaderView(settingsItemRootLLayout);
    }
}
