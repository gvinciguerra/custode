package com.gvdev.custode.activities;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextSwitcher;

import com.gvdev.custode.CustodeUtils;
import com.gvdev.custode.LocationService;
import com.gvdev.custode.R;
import com.gvdev.custode.views.CustodeButtonView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private Vibrator vibrator;
    private long touchDownTime;
    private TextSwitcher hintTextView;
    private Runnable onTouchUpRunnable;
    private Runnable onTouchDownRunnable;
    private CustodeButtonView custodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hintTextView = (TextSwitcher) findViewById(R.id.hint_text_view);
        custodeView = (CustodeButtonView) findViewById(R.id.button_view);
        custodeView.setOnTouchListener(this);
        onTouchUpRunnable = new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(MainActivity.this, PinCountdownActivity.class);
                startActivity(myIntent);
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);
            }
        };
        onTouchDownRunnable = new Runnable() {
            @Override
            public void run() {
                CustodeUtils.setScreenBrightness(MainActivity.this, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF);
                Intent i = new Intent(MainActivity.this, LocationService.class);
                i.setAction(LocationService.GEOCODE_OFF_ACTION);
                startService(i);
            }
        };
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        hintTextView.setText(getString(R.string.hold_your_finger_1));
        custodeView.setAnimationEffect(CustodeButtonView.AnimationEffect.CALM);

        ArrayList<String> deniedPermissions = new ArrayList<>();
        deniedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        deniedPermissions.add(Manifest.permission.READ_CONTACTS);
        deniedPermissions.add(Manifest.permission.SEND_SMS);
        Iterator<String> iterator = deniedPermissions.iterator();
        while (iterator.hasNext())
            if (ActivityCompat.checkSelfPermission(this, iterator.next()) == PackageManager.PERMISSION_GRANTED)
                iterator.remove();
        if (deniedPermissions.size() > 0)
            ActivityCompat.requestPermissions(this, deniedPermissions.toArray(new String[deniedPermissions.size()]), 0);
    }

    @Override
    protected void onPause() {
        super.onPause();

        boolean pausedWhileAlarmActive = custodeView.getAnimationEffect() != CustodeButtonView.AnimationEffect.CALM;
        if (pausedWhileAlarmActive) {
            custodeView.removeCallbacks(onTouchUpRunnable);
            onTouchUpRunnable.run();
        } else
            stopService(new Intent(this, LocationService.class));
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view != custodeView)
            return false;

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!checkRequiredServicesAndSettings())
                    return true;

                custodeView.removeCallbacks(onTouchUpRunnable);
                custodeView.removeCallbacks(onTouchDownRunnable);
                custodeView.postDelayed(onTouchDownRunnable, 4000);

                boolean eventDidStopAlarm = custodeView.getAnimationEffect() == CustodeButtonView.AnimationEffect.WARNING;
                if (eventDidStopAlarm)
                    vibrator.cancel();
                else
                    touchDownTime = System.currentTimeMillis();

                invalidateOptionsMenu();
                hintTextView.setText("");
                custodeView.setKeepScreenOn(true);
                custodeView.setAnimationEffect(CustodeButtonView.AnimationEffect.RIPPLE);
                break;


            case MotionEvent.ACTION_UP:
                if (custodeView.getAnimationEffect() != CustodeButtonView.AnimationEffect.RIPPLE)
                    return true;

                custodeView.setKeepScreenOn(false);
                custodeView.removeCallbacks(onTouchDownRunnable);
                CustodeUtils.setScreenBrightness(this, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);

                boolean ignoreShortTouch = System.currentTimeMillis() - touchDownTime < 1500;
                if (ignoreShortTouch) {
                    hintTextView.setText(getString(R.string.hold_your_finger_1));
                    custodeView.setAnimationEffect(CustodeButtonView.AnimationEffect.CALM);
                    invalidateOptionsMenu();
                    break;
                }

                custodeView.setAnimationEffect(CustodeButtonView.AnimationEffect.WARNING);
                custodeView.postDelayed(onTouchUpRunnable, 3000);
                long[] sos = new long[]{0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 1000};
                vibrator.vibrate(sos, -1);
                hintTextView.setText(getString(R.string.hold_your_finger_2));
                break;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (custodeView.getAnimationEffect() != CustodeButtonView.AnimationEffect.CALM)
            return false;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_setting:
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                this.startActivity(intent);
                return true;

            case R.id.menu_start_alarm:
                if (!checkRequiredServicesAndSettings())
                    return true;

                Location l = LocationService.getBestLastKnownLocation(this);
                boolean isLastKnownLocationOld = l == null || SystemClock.elapsedRealtimeNanos() - l.getElapsedRealtimeNanos() < TimeUnit.MINUTES.toNanos(2);
                if (isLastKnownLocationOld) {
                    Intent serviceIntent = new Intent(this, LocationService.class);
                    serviceIntent.setAction(LocationService.GEOCODE_OFF_ACTION);
                    startService(serviceIntent);
                }
                new AlertDialog.Builder(this)
                        .setMessage(R.string.start_alarm_dialog_message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                startActivity(new Intent(MainActivity.this, SummaryActivity.class));
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                stopService(new Intent(MainActivity.this, LocationService.class));
                            }
                        })
                        .create()
                        .show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (custodeView.getAnimationEffect() == CustodeButtonView.AnimationEffect.CALM)
            super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int grantResult : grantResults)
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage(R.string.permission_denied_dialog_message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                final Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                                MainActivity.this.startActivity(intent);
                            }
                        })
                        .show();
                break;
            }
    }

    private boolean checkRequiredServicesAndSettings() {
        String savedPin = CustodeUtils.getSavedPin(this);
        boolean noPinSet = savedPin == null || savedPin.isEmpty();
        if (noPinSet) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.no_pin_dialog_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                            intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                            intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create()
                    .show();
            return false;
        }

        boolean noFavoriteContacts = CustodeUtils.getFavoriteContacts(this).size() == 0;
        if (noFavoriteContacts) {
            new AlertDialog.Builder(this)
                    .setMessage(com.gvdev.custode.R.string.no_favorite_contacts_dialog_message)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(MainActivity.this, ContactsPickerActivity.class));
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .create()
                    .show();
            return false;
        }

        boolean gpsOff = !((LocationManager) getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsOff) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_disabled_dialog_message)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            dialogInterface.cancel();
                        }
                    })
                    .create()
                    .show();
            return false;
        }

        return true;
    }
}
