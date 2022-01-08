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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import ws.mochaa.biometricpay.util.ApplicationUtils;
import ws.mochaa.biometricpay.util.Config;
import ws.mochaa.biometricpay.util.DpUtils;
import ws.mochaa.biometricpay.util.NotifyUtils;
import ws.mochaa.biometricpay.util.StyleUtils;
import ws.mochaa.biometricpay.util.TaobaoVersionControl;
import ws.mochaa.biometricpay.util.Task;
import ws.mochaa.biometricpay.util.ViewUtils;
import ws.mochaa.biometricpay.util.drawable.XDrawable;
import ws.mochaa.biometricpay.util.log.L;
import ws.mochaa.biometricpay.view.AlipayPayView;
import ws.mochaa.biometricpay.view.DialogFrameLayout;
import ws.mochaa.biometricpay.view.SettingsView;

public class TaobaoBasePlugin {

    private AlertDialog mFingerPrintAlertDialog;
    private boolean mPwdActivityDontShowFlag;
    private int mPwdActivityReShowDelayTimeMsec;

    private LinearLayout mItemHlinearLayout;
    private LinearLayout mLineTopCon;
    private View mLineBottomView;

    private AppCompatActivity mCurrentActivity;

    private boolean mIsViewTreeObserverFirst;
    private int mTaobaoVersionCode = 0;

    private int getTaobaoVersionCode(Context context) {
        if (mTaobaoVersionCode != 0) {
            return mTaobaoVersionCode;
        }
        mTaobaoVersionCode = ApplicationUtils.getPackageVersionCode(context, Constant.PACKAGE_NAME_TAOBAO);
        return mTaobaoVersionCode;
    }

    public void onActivityCreated(AppCompatActivity activity) {
        L.d("activity", activity);
        handleSettingsMenuInjection(activity);
    }

    public void onActivityResumed(AppCompatActivity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            mCurrentActivity = activity;
            int versionCode = getTaobaoVersionCode(activity);
            if (activityClzName.contains(versionCode >= 301 ? ".PayPwdDialogActivity" : ".MspContainerActivity")
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
                    if (ViewUtils.findViewByName(activity, "com.taobao.taobao", "mini_spwd_input") == null
                            && ViewUtils.findViewByName(activity, "com.taobao.taobao", "simplePwdLayout") == null
                            && ViewUtils.findViewByName(activity, "com.taobao.taobao", "input_et_password") == null) {
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
                    TaobaoVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = TaobaoVersionControl.getDigitPasswordKeyPad(mTaobaoVersionCode);
                    View key1View = ViewUtils.findViewByName(activity, digitPasswordKeyPad.modulePackageName, digitPasswordKeyPad.key1);
                    if (key1View != null) {
                        showFingerPrintDialog(activity);
                        return;
                    }

                    //try again
                    Task.onMain(2000, () -> showFingerPrintDialog(activity));
                });
            } else if (versionCode >= 323 /* 9.20.0 */ && handleSettingsMenuInjection(activity)) {
            }
        } catch (Exception e) {
            L.e(e);
        }
    }

    protected boolean handleSettingsMenuInjection(AppCompatActivity activity) {
        try {
            final String activityClzName = activity.getClass().getName();
            if (BuildConfig.DEBUG) {
                L.d("activity", activity, "clz", activityClzName);
            }
            if (activityClzName.contains(".NewTaobaoSettingActivity") // <- 这个命名真是SB, 再改版一次估计你要叫 NewNewTaobaoSettingActivity 了吧
                    || activityClzName.contains(".TaobaoSettingActivity")) {
                Task.onMain(250, () -> doSettingsMenuInject(activity)); // 100 -> 250, 这个改版的页面加载性能还没上一版好
                Task.onMain(1000, () -> doSettingsMenuInject(activity)); // try again
                return true;
            }
        } catch (Exception e) {
            L.e(e);
        }
        return false;
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
                AlertDialog dialog = mFingerPrintAlertDialog;
                if (dialog != null) {
                    dialog.dismiss();
                }
                activity.onBackPressed();
            }).withOnDismissListener(v -> {
                if (!mPwdActivityDontShowFlag) {
                    Task.onMain(mPwdActivityReShowDelayTimeMsec, () -> activity.getWindow().getDecorView().setAlpha(1));
                }
            });
            Task.onMain(100, () -> mFingerPrintAlertDialog = alipayPayView.showInDialog());
        } catch (OutOfMemoryError e) {
        }
    }

    /**
     * 感觉是淘宝出bug了, 控件的id 和Resources 获取到的id 不符
     */
    @Nullable
    private View findTaobaoVSettingsPageItemView(@NonNull ViewGroup rootView) {
        List<View> childViewList = new ArrayList<>();
        ViewUtils.getChildViews(rootView, childViewList);
        String packageName = rootView.getContext().getPackageName();
        String identifier = packageName + ":id/v_setting_page_item} ";
        ViewUtils.sortViewListByYPosition(childViewList);
        for (View childView : childViewList) {
            if (ViewUtils.getViewInfo(childView).contains(identifier)) {
                if (childView.isShown()) {
                    return childView;
                }
            }
        }
        return null;
    }

    private void doSettingsMenuInject(final AppCompatActivity activity) {
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView();
        if (ViewUtils.findViewByText(rootView, Lang.getString(R.id.app_settings_name)) != null) {
            return;
        }
        View itemView = ViewUtils.findViewByName(activity, activity.getPackageName(), "v_setting_page_item");
        if (itemView == null) {
            itemView = findTaobaoVSettingsPageItemView(rootView);
        }
        LinearLayout linearLayout = (LinearLayout) itemView.getParent();
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

        if (mLineTopCon == null) {
            mLineTopCon = new LinearLayout(activity);
        } else {
            ViewUtils.removeFromSuperView(mLineTopCon);
        }
        mLineTopCon.setPadding(DpUtils.dip2px(activity, 12), 0, 0, 0);
        mLineTopCon.setBackgroundColor(Color.WHITE);

        View lineTopView = new View(activity);
        lineTopView.setBackgroundColor(0xFFDFDFDF);
        mLineTopCon.addView(lineTopView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));

        if (mItemHlinearLayout == null) {
            mItemHlinearLayout = new LinearLayout(activity);
        } else {
            ViewUtils.removeFromSuperView(mItemHlinearLayout);
            mItemHlinearLayout.removeAllViews();
        }
        mItemHlinearLayout.setOrientation(LinearLayout.HORIZONTAL);
        mItemHlinearLayout.setWeightSum(1);
        mItemHlinearLayout.setBackground(new XDrawable.Builder().defaultColor(Color.WHITE).create());
        mItemHlinearLayout.setGravity(Gravity.CENTER_VERTICAL);
        mItemHlinearLayout.setClickable(true);
        mItemHlinearLayout.setOnClickListener(view -> new SettingsView(activity).showInDialog());


        TextView itemNameText = new TextView(activity);

        int defHPadding = DpUtils.dip2px(activity, 13);
        itemNameText.setTextColor(0xFF051B28);
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

        //try use Taobao style
        try {
            View generalView = ViewUtils.findViewByText(activity.getWindow().getDecorView(), "通用", "General");
            L.d("generalView", generalView);
            if (generalView instanceof TextView) {
                TextView generalTextView = (TextView) generalView;
                float scale = itemNameText.getTextSize() / generalTextView.getTextSize();
                itemNameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, generalTextView.getTextSize());
                itemSummerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemSummerText.getTextSize() / scale);
                itemNameText.setTextColor(generalTextView.getCurrentTextColor());
            }
        } catch (Exception e) {
            L.e(e);
        }

        mItemHlinearLayout.addView(itemNameText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        mItemHlinearLayout.addView(itemSummerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (mLineBottomView == null) {
            mLineBottomView = new View(activity);
        } else {
            ViewUtils.removeFromSuperView(mLineBottomView);
        }
        mLineBottomView.setBackgroundColor(0xFFDFDFDF);

        linearLayout.addView(mLineTopCon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.addView(mItemHlinearLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(activity, 44)));
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lineParams.bottomMargin = DpUtils.dip2px(activity, 10);
        linearLayout.addView(mLineBottomView, lineParams);

        for (int i = 0; i < childViewCount; i++) {
            View view = childViewList.get(i);
            ViewGroup.LayoutParams params = childViewParamsList.get(i);
            linearLayout.addView(view, params);
        }
    }

    private void inputDigitPassword(AppCompatActivity activity, String password) {
        TaobaoVersionControl.DigitPasswordKeyPad digitPasswordKeyPad = TaobaoVersionControl.getDigitPasswordKeyPad(mTaobaoVersionCode);
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
        View pwdEditText = ViewUtils.findViewByName(activity, "com.taobao.taobao", "input_et_password");
        if (pwdEditText instanceof EditText) {
            if (!pwdEditText.isShown()) {
                return null;
            }
            return (EditText) pwdEditText;
        }
        ViewGroup viewGroup = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(viewGroup, "", outList);
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
        View okView = ViewUtils.findViewByName(activity, "com.taobao.taobao", "button_ok");
        if (okView != null) {
            if (!okView.isShown()) {
                return null;
            }
            return okView;
        }
        ViewGroup viewGroup = (ViewGroup) activity.getWindow().getDecorView();
        List<View> outList = new ArrayList<>();
        ViewUtils.getChildViews(viewGroup, "确认", outList);
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(viewGroup, "付款", outList);
        }
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(viewGroup, "確認", outList);
        }
        if (outList.isEmpty()) {
            ViewUtils.getChildViews(viewGroup, "Pay", outList);
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
}
