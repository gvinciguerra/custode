package com.gvdev.custode.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.gvdev.custode.CustodeUtils;
import com.gvdev.custode.LocationService;
import com.gvdev.custode.R;
import com.gvdev.custode.views.PinCodeView;

/**
 * Un'activity che mostra un tastierino per l'inserimento del pin, un countdown e che alla scadenza
 * lancia SummaryActivity.
 */
public class PinCountdownActivity extends AppCompatActivity implements KeyboardView.OnKeyboardActionListener {

    private String pin;
    private String savedPin;
    private PinCodeView pinCodeView;
    private AlarmManager alarmManager;
    private TextView locationTextView;
    private TextView countdownTextView;
    private CountDownTimer countDownTimer;
    private PendingIntent alarmPendingIntent;
    private BroadcastReceiver locationBroadcastReceiver;
    private static final int COUNTDOWN_DURATION = 15000;

    private class PinCountDownTimer extends CountDownTimer {

        public PinCountDownTimer() {
            super(COUNTDOWN_DURATION, 1000);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            int secondsLeft = (int) millisUntilFinished / 1000;
            String secondsLeftString = getResources().getQuantityString(R.plurals.you_have_x_seconds, secondsLeft, secondsLeft);
            countdownTextView.setText(secondsLeftString);
        }

        @Override
        public void onFinish() {
            // Non fa nulla qui. L'avvio di SummaryActivity (che avvisa i contatti) è fatto
            // da AlarmManager (v. scheduleSystemAlarm()) in quanto CountDownTimer viene
            // congelato se l'utente esce dall'activity o blocca lo schermo.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        pinCodeView = (PinCodeView) findViewById(R.id.pin_code_view);
        locationTextView = (TextView) findViewById(R.id.location_text_view);
        countdownTextView = (TextView) findViewById(R.id.countdown_text_view);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        locationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(LocationService.EXTRA_GEOCODE)) {
                    String s = intent.getStringExtra(LocationService.EXTRA_GEOCODE);
                    locationTextView.setText(s);
                }
            }
        };

        KeyboardView keyboardView = (KeyboardView) findViewById(R.id.keyboard_view);
        keyboardView.setPreviewEnabled(false);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setKeyboard(new Keyboard(this, R.xml.keyboard));

        int flags = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        int visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().addFlags(flags);
        getWindow().getDecorView().setSystemUiVisibility(visibility);

        final int finalVisibility = visibility;
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int v) {
                // Torna sempre in modalità fullscreen
                if ((v & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                    getWindow().getDecorView().setSystemUiVisibility(finalVisibility);
            }
        });

        savedPin = CustodeUtils.getSavedPin(this);
        countDownTimer = new PinCountDownTimer();
        countDownTimer.start();
        scheduleSystemAlarm();
    }

    @Override
    protected void onStart() {
        super.onStart();
        pin = "";
        pinCodeView.setPinLength(0);

        Intent i = new Intent(this, LocationService.class);
        i.setAction(LocationService.GEOCODE_ON_ACTION);
        startService(i);
        LocalBroadcastManager.getInstance(this).registerReceiver((locationBroadcastReceiver), new IntentFilter(LocationService.LOCATION_CHANGED_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationBroadcastReceiver);
        Intent i = new Intent(this, LocationService.class);
        stopService(i);
    }

    private void checkPin() {
        if (savedPin.equals(CustodeUtils.SHA1(pin))) {
            countDownTimer.cancel();
            alarmManager.cancel(alarmPendingIntent);
            Toast.makeText(this, R.string.alarm_off, Toast.LENGTH_SHORT).show();
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.pull_in_left, R.anim.push_out_right);
        } else {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
            pin = "";
            pinCodeView.setPinLength(0);
            pinCodeView.startShakeAnimation();

            Toast toast = Toast.makeText(this, getString(R.string.incorrect_pin), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, locationTextView.getBottom());
            toast.show();
        }
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode == 0) // Tasto vuoto
            return;
        if (primaryCode != Keyboard.KEYCODE_DELETE)
            pin = pin + Character.toString((char) primaryCode);
        else if (pin.length() > 0)
            pin = pin.substring(0, pin.length() - 1);

        pinCodeView.setPinLength(pin.length());
        if (pin.length() == 4) {
            pinCodeView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    checkPin();
                }
            }, 250);
        }
    }

    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }

    private void scheduleSystemAlarm() {
        alarmPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, SummaryActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        int type = AlarmManager.ELAPSED_REALTIME_WAKEUP;
        long trigger = SystemClock.elapsedRealtime() + COUNTDOWN_DURATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            alarmManager.setExactAndAllowWhileIdle(type, trigger, alarmPendingIntent);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            alarmManager.setExact(type, trigger, alarmPendingIntent);
        else
            alarmManager.set(type, trigger, alarmPendingIntent);
    }

}
