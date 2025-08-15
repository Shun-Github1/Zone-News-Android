package com.anssy.znewspro.selfview.popup;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.RadioGroup;


import com.anssy.znewspro.R;
import com.anssy.znewspro.ui.MainActivity;
import com.anssy.znewspro.utils.ThemeManager;
import com.hjq.language.LocaleContract;
import com.hjq.language.MultiLanguages;

import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2024年11月30日 16:29:51
 */

public class SettingPopupWindow extends BasePopupWindow {
    public  SettingPopupWindow(Context context) {
        super(context);
        setContentView(R.layout.settings_popup_menu);
        // Ensure backdrop is transparent and content uses theme surface
        setBackgroundColor(context.getResources().getColor(R.color.transparent));
    }

    @Override
    protected Animation onCreateShowAnimation() {

        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.FROM_TOP).toShow();
    }

    @Override
    protected Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.TO_TOP).toDismiss();
    }

    @Override
    public void onViewCreated(View contentView) {
        super.onViewCreated(contentView);
        RadioGroup themeRg = findViewById(R.id.theme_rg);
        RadioGroup langRg = findViewById(R.id.lang_rg);

        // Preselect current theme
        String currentTheme = ThemeManager.INSTANCE.getCurrentTheme(getContext());
        if ("light".equals(currentTheme)) {
            themeRg.check(R.id.light_rb);
        } else if ("dark".equals(currentTheme)) {
            themeRg.check(R.id.dark_rb);
        } else {
            themeRg.check(R.id.system_rb);
        }

        contentView.findViewById(R.id.apply_btn).setOnClickListener(v -> {
            boolean needsRestart = false;

            int themeId = themeRg.getCheckedRadioButtonId();
            String themeMode = "system";
            if (themeId == R.id.light_rb) themeMode = "light";
            else if (themeId == R.id.dark_rb) themeMode = "dark";

            String cur = ThemeManager.INSTANCE.getCurrentTheme(getContext());
            if (!cur.equals(themeMode)) {
                ThemeManager.INSTANCE.saveTheme(getContext(), themeMode);
                ThemeManager.INSTANCE.applyTheme(themeMode);
                needsRestart = true;
            }

            int langId = langRg.getCheckedRadioButtonId();
            boolean langChanged = false;
            if (langId == R.id.eng_rb) {
                langChanged = MultiLanguages.setAppLanguage(getContext(), LocaleContract.getEnglishLocale());
            } else if (langId == R.id.traditional_rb) {
                langChanged = MultiLanguages.setAppLanguage(getContext(), LocaleContract.getTraditionalChineseLocale());
            } else if (langId == R.id.simple_rb) {
                langChanged = MultiLanguages.setAppLanguage(getContext(), LocaleContract.getSimplifiedChineseLocale());
            }
            if (langChanged) needsRestart = true;

            if (needsRestart) {
                Intent intent = new Intent(getContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                getContext().startActivity(intent);
            }

            dismiss();
        });
    }
}
