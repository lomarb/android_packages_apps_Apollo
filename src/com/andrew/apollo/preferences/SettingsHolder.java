/**
 * 
 */

package com.andrew.apollo.preferences;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.activities.MusicLibrary;
import com.andrew.apollo.utils.ImageUtils;
import com.andrew.apollo.utils.ThemeUtils;

import static com.andrew.apollo.Constants.APOLLO;
import static com.andrew.apollo.Constants.THEME_PACKAGE_NAME;
import static com.andrew.apollo.Constants.THEME_PREVIEW;
/**
 * @author Andrew Neal FIXME - Work on the IllegalStateException thrown when
 *         using PreferenceFragment and theme chooser
 */
@SuppressWarnings("deprecation")
public class SettingsHolder extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This should be called first thing
        super.onCreate(savedInstanceState);

        // Load settings XML
        int preferencesResId = R.xml.settings;
        addPreferencesFromResource(preferencesResId);
        
        // Init delete cache option
        deleteCache();
        
        // Load the theme chooser
        initThemeChooser();
        
        //Enable up button
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    

    /**
     * Removes all of the cache entries.
     */
    private void deleteCache() {
        final Preference deleteCache = findPreference("delete_cache");
        deleteCache.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                new AlertDialog.Builder(SettingsHolder.this).setMessage(R.string.delete_warning)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        	@Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                 try {
									ImageUtils.deleteCache(getApplicationContext());
								} catch (Exception e) {
									e.printStackTrace();
								}
                            }
                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
                return true;
            }
        });
    }
    

    /**
     * @param v
     */
    public void applyTheme(View v) {
        ThemePreview themePreview = (ThemePreview)findPreference(THEME_PREVIEW);
        String packageName = themePreview.getValue().toString();
        ThemeUtils.setThemePackageName(this, packageName);
        Intent intent = new Intent();
        intent.setClass(this, MusicLibrary.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * @param v
     */
    public void getThemes(View v) {
        Uri marketUri = Uri
                .parse("https://market.android.com/search?q=ApolloThemes&c=apps&featured=APP_STORE_SEARCH");
        Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(marketUri);
        startActivity(marketIntent);
        finish();
    }

    /**
     * Set up the theme chooser
     */
    public void initThemeChooser() {
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        String themePackage = sp.getString(THEME_PACKAGE_NAME, APOLLO);
        ListPreference themeLp = (ListPreference)findPreference(THEME_PACKAGE_NAME);
        themeLp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ThemePreview themePreview = (ThemePreview)findPreference(THEME_PREVIEW);
                themePreview.setTheme(newValue.toString());
                return false;
            }
        });

        Intent intent = new Intent("com.andrew.apollo.THEMES");
        intent.addCategory("android.intent.category.DEFAULT");
        PackageManager pm = getPackageManager();
        List<ResolveInfo> themes = pm.queryIntentActivities(intent, 0);
        String[] entries = new String[themes.size() + 1];
        String[] values = new String[themes.size() + 1];
        entries[0] = APOLLO;
        values[0] = APOLLO;
        for (int i = 0; i < themes.size(); i++) {
            String appPackageName = (themes.get(i)).activityInfo.packageName.toString();
            String themeName = (themes.get(i)).loadLabel(pm).toString();
            entries[i + 1] = themeName;
            values[i + 1] = appPackageName;
        }
        themeLp.setEntries(entries);
        themeLp.setEntryValues(values);
        ThemePreview themePreview = (ThemePreview)findPreference(THEME_PREVIEW);
        themePreview.setTheme(themePackage);
    }
}
