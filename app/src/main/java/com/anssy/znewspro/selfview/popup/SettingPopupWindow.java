package com.anssy.znewspro.selfview.popup;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.RadioGroup;


import com.anssy.znewspro.R;
import com.anssy.znewspro.utils.ThemeManager;
import com.anssy.znewspro.utils.AppIconManager;


import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

/**
 * @Description Settings popup for theme selection only
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
        RadioGroup appIconRg = findViewById(R.id.app_icon_rg);

        // Preselect current theme
        String currentTheme = ThemeManager.INSTANCE.getCurrentTheme(getContext());
        if ("light".equals(currentTheme)) {
            themeRg.check(R.id.light_rb);
        } else if ("dark".equals(currentTheme)) {
            themeRg.check(R.id.dark_rb);
        } else {
            themeRg.check(R.id.system_rb);
        }

        // Preselect current app icon
        String currentIcon = AppIconManager.INSTANCE.getCurrentIconType(getContext());
        if ("default".equals(currentIcon)) {
            appIconRg.check(R.id.default_icon_rb);
        } else {
            appIconRg.check(R.id.alternate_icon_rb);
        }

        contentView.findViewById(R.id.apply_btn).setOnClickListener(v -> {
            boolean needsRestart = false;

            // Handle theme changes
            int themeId = themeRg.getCheckedRadioButtonId();
            String themeMode = "system";
            if (themeId == R.id.light_rb) themeMode = "light";
            else if (themeId == R.id.dark_rb) themeMode = "dark";

            String curTheme = ThemeManager.INSTANCE.getCurrentTheme(getContext());
            if (!curTheme.equals(themeMode)) {
                ThemeManager.INSTANCE.saveTheme(getContext(), themeMode);
                ThemeManager.INSTANCE.applyTheme(themeMode);
                needsRestart = true;
            }

            // Handle app icon changes
            int iconId = appIconRg.getCheckedRadioButtonId();
            String iconType = "default";
            if (iconId == R.id.alternate_icon_rb) iconType = "alternate";

            String curIcon = AppIconManager.INSTANCE.getCurrentIconType(getContext());
            if (!curIcon.equals(iconType)) {
                AppIconManager.INSTANCE.changeAppIcon(getContext(), iconType);
                needsRestart = true;
            }

            if (needsRestart && getContext() instanceof Activity) {
                ((Activity) getContext()).recreate();
            }

            dismiss();
        });
    }
}
