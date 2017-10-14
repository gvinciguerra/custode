package com.gvdev.custode.activities;


import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.gvdev.custode.R;
import com.gvdev.custode.SmsUpdateReceiver;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {

    private GeneralPreferenceFragment generalPreferenceFragment;

    public static final String PREFERENCES_PIN_KEY = "pin";
    public static final String PREFERENCES_CONTACTS_KEY = "contacts";
    public static final String PREFERENCES_SMS_BODY_KEY = "sms_body";
    public static final String PREFERENCES_SMS_LIMIT_KEY = "sms_limit";
    public static final String PREFERENCES_SMS_LIMIT_COUNT_KEY = "sms_limit_count";
    public static final String PREFERENCES_SMS_LIMIT_DATE_KEY = "sms_limit_date";
    public static final String PREFERENCES_REPLY_LOCATION_REQUESTS_ALWAYS_KEY = "reply_location_request_always";

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            SwitchPreference replyPref = (SwitchPreference) findPreference(PREFERENCES_REPLY_LOCATION_REQUESTS_ALWAYS_KEY);
            replyPref.setSummaryOn(getString(R.string.pref_summary_on_reply_location_request, SmsUpdateReceiver.MAGIC_SMS_BODY));
            replyPref.setSummaryOff(getString(R.string.pref_summary_off_reply_location_request, SmsUpdateReceiver.MAGIC_SMS_BODY));

            findPreference(PREFERENCES_SMS_LIMIT_KEY).setSummary(getSmsLimitSummary());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                NavUtils.navigateUpFromSameTask(getActivity());
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            if (context instanceof SettingsActivity)
                ((SettingsActivity) context).setGeneralPreferenceFragment(this);
        }

        public String getSmsLimitSummary() {
            int limit = getPreferenceManager().getSharedPreferences().getInt(PREFERENCES_SMS_LIMIT_KEY, 1);
            return getResources().getQuantityString(R.plurals.pref_summary_sms_limit, limit, limit);
        }

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREFERENCES_REPLY_LOCATION_REQUESTS_ALWAYS_KEY)) {
            boolean replyAlways = sharedPreferences.getBoolean(PREFERENCES_REPLY_LOCATION_REQUESTS_ALWAYS_KEY, false);
            int state = replyAlways ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            ComponentName receiver = new ComponentName(this, SmsUpdateReceiver.class);
            this.getPackageManager().setComponentEnabledSetting(receiver, state, PackageManager.DONT_KILL_APP);
        } else if (key.equals(PREFERENCES_SMS_LIMIT_KEY) && generalPreferenceFragment != null) {
            String newSummary = generalPreferenceFragment.getSmsLimitSummary();
            generalPreferenceFragment.findPreference(PREFERENCES_SMS_LIMIT_KEY).setSummary(newSummary);
        }
    }

    private void setGeneralPreferenceFragment(GeneralPreferenceFragment generalPreferenceFragment) {
        this.generalPreferenceFragment = generalPreferenceFragment;
    }

}
