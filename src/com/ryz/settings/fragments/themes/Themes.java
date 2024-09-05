/*
 * Copyright (C) 2019-2024 The Evolution X Project
 * Copyright (C) 2021-2024 The RyzOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ryz.settings.fragments.themes;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.android.internal.util.android.ThemeUtils;

import java.util.List;

import com.ryz.settings.preferences.GlobalSettingListPreference;
import com.ryz.settings.preferences.SystemSettingListPreference;
import com.ryz.settings.utils.DeviceUtils;
import com.ryz.settings.utils.SystemRestartUtils;
import com.ryz.settings.utils.SystemUtils;

@SearchIndexable
public class Themes extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Themes";

    private static final String KEY_PGB_STYLE = "progress_bar_style";

    private static final String KEY_LOCK_SOUND = "lock_sound";
    private static final String KEY_UNLOCK_SOUND = "unlock_sound";
    private static final String KEY_ICONS_CATEGORY = "themes_icons_category";
    private static final String KEY_NAVBAR_ICON = "android.theme.customization.navbar";
    private static final String KEY_SIGNAL_ICON = "android.theme.customization.signal_icon";

    private static final String[] PROGRESS_BAR_OVERLAYS = {
            "com.android.theme.progressbar.blocky_thumb",
            "com.android.theme.progressbar.minimal_thumb",
            "com.android.theme.progressbar.outline_thumb",
            "com.android.theme.progressbar.shishu"
    };

    private GlobalSettingListPreference mLockSound;
    private GlobalSettingListPreference mUnlockSound;
    private PreferenceCategory mIconsCategory;
    private Preference mNavbarIcon;
    private Preference mSignalIcon;
    private SystemSettingListPreference mProgressBarPref;
    private ThemeUtils mThemeUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ryz_settings_themes);
        mThemeUtils = ThemeUtils.getInstance(getActivity());

        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        final Resources resources = context.getResources();

        mLockSound = (GlobalSettingListPreference) findPreference(KEY_LOCK_SOUND);
        mLockSound.setOnPreferenceChangeListener(this);
        mUnlockSound = (GlobalSettingListPreference) findPreference(KEY_UNLOCK_SOUND);
        mUnlockSound.setOnPreferenceChangeListener(this);
        mIconsCategory = (PreferenceCategory) findPreference(KEY_ICONS_CATEGORY);
        mNavbarIcon = (Preference) findPreference(KEY_NAVBAR_ICON);
        mSignalIcon = (Preference) findPreference(KEY_SIGNAL_ICON);
        mProgressBarPref = findPreference(KEY_PGB_STYLE);
        mProgressBarPref.setOnPreferenceChangeListener(this);

        if (!DeviceUtils.deviceSupportsMobileData(context)) {
            mIconsCategory.removePreference(mSignalIcon);
        }

        if (DeviceUtils.isEdgeToEdgeEnabled(context)) {
            mIconsCategory.removePreference(mNavbarIcon);
        }
    }

    private void updateStyle(String key, String category, String target,
            int defaultValue, String[] overlayPackages, boolean restartSystemUI) {
        final int style = Settings.System.getIntForUser(
                getContext().getContentResolver(),
                key,
                defaultValue,
                UserHandle.USER_CURRENT
        );
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(getContext());
        }
        mThemeUtils.setOverlayEnabled(category, target, target);
        if (style > 0 && style <= overlayPackages.length) {
            mThemeUtils.setOverlayEnabled(category, overlayPackages[style - 1], target);
        }
        if (restartSystemUI) {
            SystemRestartUtils.restartSystemUI(getContext());
        }
    }
    
    private void updateProgressBarStyle() {
        updateStyle(KEY_PGB_STYLE, "android.theme.customization.progress_bar", "android", 0, PROGRESS_BAR_OVERLAYS, false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        final ContentResolver resolver = context.getContentResolver();
        // Ensure newValue is a valid integer before parsing
        int value = 0;
        if (newValue instanceof String) {
            try {
                value = Integer.parseInt((String) newValue);
            } catch (NumberFormatException e) {
                // Handle the case where newValue is not an integer (like a file path)
                if (preference == mLockSound || preference == mUnlockSound) {
                    SystemUtils.showSystemUiRestartDialog(context);
                    return true;
                }
                return false;
            }
        }
        if (preference == mProgressBarPref) {
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    KEY_PGB_STYLE, value, UserHandle.USER_CURRENT);
            updateProgressBarStyle();
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RYZTUNE;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider(R.xml.ryz_settings_themes) {

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                final Resources resources = context.getResources();

                if (!DeviceUtils.deviceSupportsMobileData(context)) {
                    keys.add(KEY_SIGNAL_ICON);
                }
                return keys;
            }
        };
}
