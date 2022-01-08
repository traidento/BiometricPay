package ws.mochaa.biometricpay.util.paydialog;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ws.mochaa.biometricpay.util.Tools;
import ws.mochaa.biometricpay.util.ViewUtils;
import ws.mochaa.biometricpay.util.log.L;

public class WeChatPayDialog {

    public ViewGroup passwordLayout;
    public EditText inputEditText;
    public View keyboardView;
    @Nullable
    public TextView usePasswordText;
    @Nullable
    public TextView titleTextView;

    @Nullable
    public static WeChatPayDialog findFrom(ViewGroup rootView) {
        try {
            WeChatPayDialog payDialog = new WeChatPayDialog();

            List<View> childViews = new ArrayList<>();
            ViewUtils.getChildViews(rootView, childViews);
            for (View view : childViews) {
                if (view == null) {
                    continue;
                }
                if (view.getClass().getName().endsWith(".EditHintPasswdView")) {
                    L.d("mPasswordLayout:" + view);
                    if (view instanceof ViewGroup) {
                        payDialog.passwordLayout = (ViewGroup) view;
                    }
                } else if (view.getClass().getName().endsWith(".TenpaySecureEditText")) {
                    L.d("密码输入框:" + view);
                    if (view instanceof EditText) {
                        payDialog.inputEditText = (EditText) view;
                    }
                } else if (view.getClass().getName().endsWith(".MyKeyboardWindow")) {
                    L.d("密码键盘:" + view);
                    if (view.getParent() != null) {
                        payDialog.keyboardView = (View) view.getParent();
                    }
                }
            }

            if (payDialog.passwordLayout == null) {
                Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat passwordLayout NOT FOUND]  " + ViewUtils.viewsDesc(childViews));
                return null;
            }

            if (payDialog.inputEditText == null) {
                Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat inputEditText NOT FOUND]  " + ViewUtils.viewsDesc(childViews));
                return null;
            }

            if (payDialog.keyboardView == null) {
                Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat keyboardView NOT FOUND]  " + ViewUtils.viewsDesc(childViews));
                return null;
            }

            payDialog.usePasswordText = (TextView) ViewUtils.findViewByText(rootView,
                    "使用密码", "使用密碼", "Password",
                    "使用指纹", "使用指紋", "Fingerprint");
            L.d("payDialog.usePasswordText", payDialog.usePasswordText); // 6.5.16 app:id/dh0
            if (payDialog.usePasswordText == null) {
                Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat usePasswordText NOT FOUND]  " + ViewUtils.viewsDesc(childViews));
            }

            payDialog.titleTextView = (TextView) ViewUtils.findViewByText(rootView,
                    "请验证指纹", "請驗證指紋", "Verify fingerprint",
                    "请输入支付密码", "請輸入付款密碼", "Enter payment password");
            L.d("payDialog.titleTextView", payDialog.titleTextView); // 6.5.16 app:id/dgz
            if (payDialog.titleTextView == null) {
                Tools.doUnSupportVersionUpload(rootView.getContext(), "[WeChat titleTextView NOT FOUND]  " + ViewUtils.viewsDesc(childViews));
            }
            return payDialog;
        } catch (Exception e) {
            L.e(e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "PayDialog{" +
                "passwordLayout=" + passwordLayout +
                ", inputEditText=" + inputEditText +
                ", keyboardView=" + keyboardView +
                ", usePasswordText=" + usePasswordText +
                ", titleTextView=" + titleTextView +
                '}';
    }
}