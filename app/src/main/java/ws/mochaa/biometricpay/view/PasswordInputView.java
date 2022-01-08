package ws.mochaa.biometricpay.view;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ws.mochaa.biometricpay.R;

import ws.mochaa.biometricpay.Constant;
import ws.mochaa.biometricpay.Lang;
import ws.mochaa.biometricpay.util.DpUtils;

/**
 * Created by Jason on 2017/9/9.
 */

public class PasswordInputView extends DialogFrameLayout {

    public static final String DEFAULT_HIDDEN_PASS = "123zxc456asdxxx";

    private EditText mInputView;

    public PasswordInputView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public PasswordInputView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PasswordInputView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        LinearLayout rootLLayout = new LinearLayout(context);
        rootLLayout.setOrientation(LinearLayout.VERTICAL);

        mInputView = new EditText(context);
        mInputView.setTextColor(Color.BLACK);
        mInputView.setPadding(0, 0, 0, 0);
        mInputView.setBackgroundColor(Color.TRANSPARENT);
        mInputView.setFocusable(true);
        mInputView.setFocusableInTouchMode(true);
        mInputView.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        String packageName = context.getPackageName();
        if (Constant.PACKAGE_NAME_ALIPAY.equals(packageName)
                || Constant.PACKAGE_NAME_TAOBAO.equals(packageName)
                || Constant.PACKAGE_NAME_QQ.equals(packageName)
        ) {
            mInputView.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
        } else {
            mInputView.setInputType(InputType.TYPE_CLASS_NUMBER
                    | InputType.TYPE_NUMBER_VARIATION_PASSWORD
            );
        }

        LinearLayout.LayoutParams layoutParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int defHMargin = DpUtils.dip2px(context, 15);
        int defTMargin = DpUtils.dip2px(context, 20);
        int defBMargin = DpUtils.dip2px(context, 4);
        layoutParam.setMargins(defHMargin, defTMargin, defHMargin, defBMargin);

        View lineView = new View(context);
        lineView.setBackgroundColor(0xFF45C01A);

        rootLLayout.addView(mInputView, layoutParam);
        rootLLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 1)));

        LayoutParams rootLLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootLLayoutParams.setMargins(defHMargin, defTMargin, defHMargin, DpUtils.dip2px(context, 20));
        this.addView(rootLLayout, rootLLayoutParams);

        withPositiveButtonText(Lang.getString(R.id.ok));
    }

    @NonNull
    public String getInput() {
        Editable ediable = mInputView.getText();
        if (ediable == null) {
            return "";
        }
        return ediable.toString();
    }

    public void setDefaultText(String text) {
        mInputView.setText(text);
        int len = text.length();
        mInputView.setSelection(len, len);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(mInputView, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.enter_password);
    }
}
