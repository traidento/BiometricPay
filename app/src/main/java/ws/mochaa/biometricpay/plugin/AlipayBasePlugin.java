package ws.mochaa.biometricpay.plugin;

import static java.lang.Integer.parseInt;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import ws.mochaa.biometricpay.BuildConfig;
import ws.mochaa.biometricpay.Constant;
import ws.mochaa.biometricpay.Lang;
import ws.mochaa.biometricpay.R;
import ws.mochaa.biometricpay.util.AlipayVersionControl;
import ws.mochaa.biometricpay.util.ApplicationUtils;
import ws.mochaa.biometricpay.util.Config;
import ws.mochaa.biometricpay.util.DpUtils;
import ws.mochaa.biometricpay.util.NotifyUtils;
import ws.mochaa.biometricpay.util.StyleUtils;
import ws.mochaa.biometricpay.util.Task;
import ws.mochaa.biometricpay.util.ViewUtils;
import ws.mochaa.biometricpay.util.drawable.XDrawable;
import ws.mochaa.biometricpay.util.log.L;
import ws.mochaa.biometricpay.view.AlipayPayView;
import ws.mochaa.biometricpay.view.DialogFrameLayout;
import ws.mochaa.biometricpay.view.SettingsView;

public class AlipayBasePlugin {


    private AlertDialog mFingerPrintAlertDialog;
    private boolean mPwdActivityDontShowFlag;
    private int mPwdActivityReShowDelayTimeMsec;

    private AppCompatActivity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mAlipayVersionCode;

    private int getAlipayVersionCode(Context context) {
        if (mAlipayVersionCode != 0) {
            return mAlipayVersionCode;
        }
        mAlipayVersionCode = ApplicationUtils.getPackageVersionCode(context, Constant.PACKAGE_NAME_ALIPAY);
        return mAlipayVersionCode;
    }

    protected void onActivityCreated(AppCompatActivity activity) {
        L.d("activity", activity);
        try {
            final String activityClzName = activity.getClass().getName();
            mCurrentActivity = activity;
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".MySettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject_10_1_38(activity));
            } else if (activityClzName.contains(".UserSettingActivity")) {
                Task.onMain(100, () -> doSettingsMenuInject(activity));
            } else if (activityClzName.contains(".PayPwdDialogActivity")
                    || activityClzName.contains(".MspContainerActivity")
                    || activityClzName.contains(".FlyBirdWindowActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                mIsViewTreeObserverFirst = true;
                activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    if (mCurrentActivity == null) {
                        return;
                    }
                    if (activity.isDestroyed()) {
                        return;
                    }
                    if (mCurrentActivity != activity) {
                        return;
                    }
                    if (ViewUtils.findViewByName(activity, (getAlipayVersionCode(activity) >= 352 /* 10.2.13.7000 */ ? "com.alipay.android.safepaysdk" : "com.alipay.android.app"), "simplePwdLayout") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.safepaybase", "mini_linSimplePwdComponent") == null
                            && ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password") == null) {
                        return;
                    }
                    if (mIsViewTreeObserverFirst) {
                        mIsViewTreeObserverFirst = false;
                        showFingerPrintDialog(activity);
                    }
                });

            } else if (activityClzName.contains("PayPwdHalfActivity")) {
                L.d("found");
                final Config config = Config.from(activity);
                if (!config.isOn()) {
                    return;
                }
                activity.getWindow().getDecorView().setAlpha(0);
                Task.onMain(1500, () -> {
                    int versionCode = getAlipayVersionCode(activity);
                    AlipayVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
                    View key1View = ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1);
                    if (key1View != null) {
                        showFingerPrintDialog(activity);
                        return;
                    }

                    //try again
                    Task.onMain(2000, () -> showFingerPrintDialog(activity));
                });
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    public void initFingerPrintLock(final Context context, final Runnable onSuccessUnlockCallback) {
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
                    onSuccessUnlockCallback.run();
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
                    .setTitle("Alipay")
                    .setSubtitle("Biometric Verification")
                    .setConfirmationRequired(true)
                    .build();
            mBiometricPrompt.authenticate(promptInfo);
        } else {
            NotifyUtils.notifyFingerprint(context, Lang.getString(R.id.toast_fingerprint_not_enable));
        }
    }

    public void showFingerPrintDialog(final AppCompatActivity activity) {
        try {
            if (getAlipayVersionCode(activity) >= 224) {
                if (activity.getClass().getName().contains(".MspContainerActivity")) {
                    View payTextView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "支付宝支付密码", "支付寶支付密碼", "Alipay Payment Password");
                    L.d("payTextView", payTextView);
                    if (payTextView == null) {
                        return;
                    }
                }
            }

            hidePreviousPayDialog();
            activity.getWindow().getDecorView().setAlpha(0);
            mPwdActivityDontShowFlag = false;
            mPwdActivityReShowDelayTimeMsec = 0;
            initFingerPrintLock(activity, () -> {
                String pwd = Config.from(activity).getPassword();
                if (TextUtils.isEmpty(pwd)) {
                    Toast.makeText(activity, Lang.getString(R.id.toast_password_not_set_alipay), Toast.LENGTH_SHORT).show();
                    return;
                }

                Runnable onCompleteRunnable = () -> {
                    mPwdActivityReShowDelayTimeMsec = 1000;
                    AlertDialog dialog = mFingerPrintAlertDialog;
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                };

                if (!tryInputGenericPassword(activity, pwd)) {
                    boolean tryAgain = false;
                    try {
                        assert pwd != null;
                        inputDigitPassword(activity, pwd);
                    } catch (NullPointerException e) {
                        tryAgain = true;
                    } catch (Exception e) {
                        Toast.makeText(activity, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                        L.e(e);
                    }
                    if (tryAgain) {
                        Task.onMain(1000, () -> {
                            try {
                                inputDigitPassword(activity, pwd);
                            } catch (NullPointerException e) {
                                Toast.makeText(activity, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                                L.d("inputDigitPassword NPE", e);
                            } catch (Exception e) {
                                Toast.makeText(activity, Lang.getString(R.id.toast_password_auto_enter_fail), Toast.LENGTH_LONG).show();
                                L.e(e);
                            }
                            onCompleteRunnable.run();
                        });
                        return;
                    }
                }
                onCompleteRunnable.run();
            });
            DialogFrameLayout alipayPayView = new AlipayPayView(activity).withOnCloseImageClickListener(v -> {
                mPwdActivityDontShowFlag = true;
                AlertDialog dialog1 = mFingerPrintAlertDialog;
                if (dialog1 != null) {
                    dialog1.dismiss();
                }
                activity.onBackPressed();
            }).withOnDismissListener(v -> {
                if (!mPwdActivityDontShowFlag) {
                    Task.onMain(mPwdActivityReShowDelayTimeMsec, () -> activity.getWindow().getDecorView().setAlpha(1));
                }
            });
            Task.onMain(100, () -> mFingerPrintAlertDialog = alipayPayView.showInDialog());
        } catch (OutOfMemoryError ignored) {
        }
    }

    private void doSettingsMenuInject_10_1_38(final AppCompatActivity activity) {
        int listViewId = activity.getResources().getIdentifier("setting_list", "id", "com.alipay.android.phone.openplatform");

        ListView listView = activity.findViewById(listViewId);

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFEEEEEE);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).pressedColor(0xFFD9D9D9).create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtils.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF999999);

        //try use Alipay style
        try {
            View settingsView = ViewUtils.findViewByName(activity, "com.alipay.mobile.antui", "item_left_text");
            L.d("settingsView", settingsView);
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFEEEEEE);

        LinearLayout rootLinearLayout = new LinearLayout(activity);
        rootLinearLayout.setOrientation(LinearLayout.VERTICAL);
        rootLinearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        rootLinearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 45)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtils.dip2px(activity, 20);
        rootLinearLayout.addView(lineBottomView, lineParams);

        listView.addHeaderView(rootLinearLayout);
    }

    private void doSettingsMenuInject(final AppCompatActivity activity) {
        int logout_id = activity.getResources().getIdentifier("logout", "id", "com.alipay.android.phone.openplatform");

        View logoutView = activity.findViewById(logout_id);
        LinearLayout linearLayout = (LinearLayout) logoutView.getParent();
        linearLayout.setPadding(0, 0, 0, 0);
        List<ViewGroup.LayoutParams> childViewParamsList = new ArrayList<>();
        List<View> childViewList = new ArrayList<>();
        int childViewCount = linearLayout.getChildCount();
        for (int i = 0; i < childViewCount; i++) {
            View view = linearLayout.getChildAt(i);
            childViewList.add(view);
            childViewParamsList.add(view.getLayoutParams());
        }

        linearLayout.removeAllViews();

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFDFDFDF);

        LinearLayout itemHlinearLayout = new LinearLayout(activity);
        itemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemHlinearLayout.setWeightSum(1);
        itemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).create());
        itemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemHlinearLayout.setClickable(true);
        itemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());

        int defHPadding = DpUtils.dip2px(activity, 15);

        TextView itemNameText = new TextView(activity);
        StyleUtils.apply(itemNameText);
        itemNameText.setText(Lang.getString(R.id.app_settings_name));
        itemNameText.setGravity(Gravity.CENTER_VERTICAL);
        itemNameText.setPadding(defHPadding, 0, 0, 0);
        itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, StyleUtils.TEXT_SIZE_BIG);

        TextView itemSummerText = new TextView(activity);
        StyleUtils.apply(itemSummerText);
        itemSummerText.setText(BuildConfig.VERSION_NAME);
        itemSummerText.setGravity(Gravity.CENTER_VERTICAL);
        itemSummerText.setPadding(0, 0, defHPadding, 0);
        itemSummerText.setTextColor(0xFF888888);

        //try use Alipay style
        try {
            View settingsView = ViewUtils.findViewByName(activity, "com.alipay.mobile.ui", "title_bar_title");
            L.d("settingsView", settingsView);
            if (settingsView instanceof TextView) {
                TextView settingsTextView = (TextView) settingsView;
                float scale = itemNameText.getTextSize() / settingsTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, settingsTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(settingsTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        itemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        itemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View lineBottomView = new View(activity);
        lineBottomView.setBackgroundColor(0xFFDFDFDF);

        linearLayout.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        linearLayout.addView(itemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 50)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtils.dip2px(activity, 20);
        linearLayout.addView(lineBottomView, lineParams);

        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    private void inputDigitPassword(AppCompatActivity activity, String password) {
        int versionCode = getAlipayVersionCode(activity);
        AlipayVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = AlipayVersionControl.getDigitPasswordKeyPad(versionCode);
        View[] ks = new View[]{
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key2),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key3),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key4),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key5),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key6),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key7),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key8),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key9),
                ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key0),
        };
        char[] chars = password.toCharArray();
        for (char c : chars) {
            View v;
            int digit = parseInt(String.valueOf(c), 10);
            if (digit >= 1 && digit <= 9) {
                v = ks[digit - 1];
            } else {
                v = ks[9];
            }
            assert v != null;
            ViewUtils.performActionClick(v);
        }
    }

    private boolean tryInputGenericPassword(AppCompatActivity activity, String password) {

        EditText pwdEditText = findPasswordEditText(activity);
        L.d("pwdEditText", pwdEditText);
        if (pwdEditText == null) {
            return false;
        }
        View confirmPwdBtn = findConfirmPasswordBtn(activity);
        L.d("confirmPwdBtn", confirmPwdBtn);
        if (confirmPwdBtn == null) {
            return false;
        }
        pwdEditText.setText(password);
        confirmPwdBtn.performClick();
        return true;
    }

    private EditText findPasswordEditText(AppCompatActivity activity) {
        View pwdEditText = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "input_et_password");
        L.d("pwdEditText1", pwdEditText);
        if (pwdEditText instanceof EditText) {
            if (!pwdEditText.isShown()) {
                return null;
            }
            return (EditText) pwdEditText;
        }
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, "", outList);
        for (View view : outList) {
            if (view instanceof EditText) {
                if (view.getId() != -1) {
                    continue;
                }
                if (!view.isShown()) {
                    continue;
                }
                return (EditText) view;
            }
        }
        return null;
    }

    private View findConfirmPasswordBtn(AppCompatActivity activity) {
        View okView = ViewUtils.findViewByName(activity, "com.alipay.android.phone.mobilecommon.verifyidentity", "button_ok");
        L.d("okView", okView);
        if (okView != null) {
            if (!okView.isShown()) {
                return null;
            }
            return okView;
        }
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, "付款", outList);
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(rootView, "Pay", outList);
        }
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(rootView, "确定", outList);
        }
        for (View view : outList) {
            if (view.getId() != -1) {
                continue;
            }
            if (!view.isShown()) {
                continue;
            }
            return (View) view.getParent();
        }
        return null;
    }

    private void hidePreviousPayDialog() {
        AlertDialog dialog = mFingerPrintAlertDialog;
        L.d("hidePreviousPayDialog", mFingerPrintAlertDialog);
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
