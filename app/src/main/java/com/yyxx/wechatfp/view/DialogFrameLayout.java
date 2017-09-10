package com.yyxx.wechatfp.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.yyxx.wechatfp.listener.OnDismissListener;

/**
 * Created by Jason on 2017/9/9.
 */

public abstract class DialogFrameLayout extends FrameLayout implements DialogInterface.OnDismissListener {

    private OnDismissListener mDismissListener;

    public DialogFrameLayout(@NonNull Context context) {
        super(context);
    }

    public DialogFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void showInDialog() {
        showInDialog(false);
    }

    public void showInDialog(boolean showConfirmBtn) {
        String title = getDialogTitle();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(this).setOnDismissListener(this);
        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }
        if (showConfirmBtn) {
            builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
        }
        AlertDialog dialog;
        dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        dialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        OnDismissListener listener = mDismissListener;
        if (listener != null) {
            listener.onDismiss(this);
        }
    }

    public DialogFrameLayout withOnDismissListener(OnDismissListener listener) {
        mDismissListener = listener;
        return this;
    }

    public String getDialogTitle() {
        return null;
    }
}