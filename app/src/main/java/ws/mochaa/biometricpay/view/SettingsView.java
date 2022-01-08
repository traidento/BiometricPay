package ws.mochaa.biometricpay.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import ws.mochaa.biometricpay.BuildConfig;
import ws.mochaa.biometricpay.Constant;
import ws.mochaa.biometricpay.Lang;
import ws.mochaa.biometricpay.R;
import ws.mochaa.biometricpay.adapter.PreferenceAdapter;
import ws.mochaa.biometricpay.util.Config;
import ws.mochaa.biometricpay.util.DpUtils;

/**
 * Created by Jason on 2017/9/9.
 */

public class SettingsView extends DialogFrameLayout implements AdapterView.OnItemClickListener {

    private final List<PreferenceAdapter.Data> mSettingsDataList = new ArrayList<>();
    private PreferenceAdapter mListAdapter;
    private ListView mListView;

    public SettingsView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public SettingsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SettingsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LinearLayout rootVerticalLayout = new LinearLayout(context);
        rootVerticalLayout.setOrientation(LinearLayout.VERTICAL);

        View lineView = new View(context);
        lineView.setBackgroundColor(Color.TRANSPARENT);

        int defHPadding = DpUtils.dip2px(context, 0);
        int defVPadding = DpUtils.dip2px(context, 12);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        mListView.setOnItemClickListener(this);
        mListView.setPadding(defHPadding, defVPadding, defHPadding, defVPadding);
        mListView.setDivider(new ColorDrawable(Color.TRANSPARENT));

        if (context.getPackageName().equals(Constant.PACKAGE_NAME_WECHAT)) {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_switch), Lang.getString(R.id.settings_sub_title_switch_wechat), true, Config.from(context).isOn()));
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_password), Lang.getString(R.id.settings_sub_title_password_wechat)));
        } else {
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_switch), Lang.getString(R.id.settings_sub_title_switch_alipay), true, Config.from(context).isOn()));
            mSettingsDataList.add(new PreferenceAdapter.Data(Lang.getString(R.id.settings_title_password), Lang.getString(R.id.settings_sub_title_password_alipay)));
        }

        mListAdapter = new PreferenceAdapter(mSettingsDataList);

        rootVerticalLayout.addView(lineView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DpUtils.dip2px(context, 2)));
        rootVerticalLayout.addView(mListView);

        this.addView(rootVerticalLayout);
    }

    @Override
    public String getDialogTitle() {
        return Lang.getString(R.id.app_settings_name) + " " + BuildConfig.VERSION_NAME;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mListView.setAdapter(mListAdapter);
    }

    @Override
    public int dialogWindowHorizontalInsets() {
        return DpUtils.dip2px(getContext(), 26);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PreferenceAdapter.Data data = mListAdapter.getItem(position);
        final Context context = getContext();
        final Config config = Config.from(context);
        if (Lang.getString(R.id.settings_title_switch).equals(data.title)) {
            if (data.selectionState || checkPasswordAndNotify(context)) {
                data.selectionState = !data.selectionState;
                config.setOn(data.selectionState);
                mListAdapter.notifyDataSetChanged();
            }
        } else if (Lang.getString(R.id.settings_title_password).equals(data.title)) {
            PasswordInputView passwordInputView = new PasswordInputView(context);
            if (!TextUtils.isEmpty(config.getPassword())) {
                passwordInputView.setDefaultText(PasswordInputView.DEFAULT_HIDDEN_PASS);
            }
            passwordInputView.withOnDismissListener(v -> {
                PasswordInputView inputView = (PasswordInputView) v;
                String inputText = inputView.getInput();
                if (TextUtils.isEmpty(inputText)) {
                    config.setPassword("");
                    return;
                }
                if (PasswordInputView.DEFAULT_HIDDEN_PASS.equals(inputText)) {
                    return;
                }
                config.setPassword(inputText);
            }).showInDialog();
        }
    }

    private boolean checkPasswordAndNotify(Context context) {
        String pwd = Config.from(context).getPassword();
        if (TextUtils.isEmpty(pwd)) {
            String packageName = context.getPackageName();
            if (Constant.PACKAGE_NAME_WECHAT.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_wechat), Toast.LENGTH_SHORT).show();
            } else if (Constant.PACKAGE_NAME_ALIPAY.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_alipay), Toast.LENGTH_SHORT).show();
            } else if (Constant.PACKAGE_NAME_TAOBAO.equals(packageName)) {
                Toast.makeText(context, Lang.getString(R.id.toast_password_not_set_taobao), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
}
