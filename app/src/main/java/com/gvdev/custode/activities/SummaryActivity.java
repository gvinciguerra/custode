package com.gvdev.custode.activities;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.gvdev.custode.CustodeUtils;
import com.gvdev.custode.LocationService;
import com.gvdev.custode.R;
import com.gvdev.custode.SmsUpdateReceiver;
import com.gvdev.custode.views.RoundedContactBadge;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Un'activity che mostra una mappa con la posizione dell'utente e scorciatoie per contattare
 * i numeri preferiti.
 */
public class SummaryActivity extends AppCompatActivity implements
        LocationListener,
        OnMapReadyCallback,
        View.OnClickListener,
        DialogInterface.OnClickListener,
        GoogleMap.OnMyLocationButtonClickListener {

    private boolean followUser;
    private GoogleMap googleMap;
    private TextView summaryTextView;
    private List<String> favoriteContacts;
    private LocationManager locationManager;
    private LinearLayout contactsLinearLayout;
    private SmsUpdateReceiver smsUpdateReceiver;
    private FloatingActionButton shutdownButton;
    private BroadcastReceiver sentSmsStatusReceiver;
    private ColorStateList summaryTextViewDefaultColor;
    private BitmapDescriptor mapMarkerBitmapDescriptor;
    private final ArrayList<MarkerOptions> allMarkersList = new ArrayList<>(1);
    private final ArrayList<MarkerOptions> pendingMarkersList = new ArrayList<>(1);

    private static final String CONTACT_INDEX_EXTRA = "i";
    private static final String SMS_STATUS_SENT_ACTION = "com.gvdev.custode.SMS_STATUS_SENT_ACTION";
    private static final String SMS_STATUS_DELIVERED_ACTION = "com.gvdev.custode.SMS_STATUS_DELIVERED_ACTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);

        summaryTextView = (TextView) findViewById(R.id.summary_text_view);
        summaryTextViewDefaultColor = summaryTextView.getTextColors();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        contactsLinearLayout = (LinearLayout) findViewById(R.id.contacts_linear_layout);
        shutdownButton = (FloatingActionButton) findViewById(R.id.shutdown_button);
        shutdownButton.setOnClickListener(this);

        int flags = WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        getWindow().addFlags(flags);

        // TODO: Sostituisci con BitmapDescriptorFactory.fromResource(R.drawable.ic_message) quando https://code.google.com/p/gmaps-api-issues/issues/detail?id=9011 sarà corretto
        Drawable vectorDrawable = ContextCompat.getDrawable(this, R.drawable.ic_message);
        int w = vectorDrawable.getIntrinsicWidth();
        int h = vectorDrawable.getIntrinsicHeight();
        vectorDrawable.setBounds(0, 0, w, h);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.draw(canvas);
        mapMarkerBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bm);
        // :ODOT

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);

        smsUpdateReceiver = new SmsUpdateReceiver();
        registerReceiver(smsUpdateReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        sentSmsStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int contactIndex = intent.getIntExtra(CONTACT_INDEX_EXTRA, -1);
                if (contactIndex < 0 || contactIndex >= favoriteContacts.size())
                    return;

                boolean smsDelivered = intent.getAction().equals(SMS_STATUS_DELIVERED_ACTION);
                RoundedContactBadge contactBadge = (RoundedContactBadge) contactsLinearLayout.getChildAt(contactIndex);
                if (smsDelivered)
                    contactBadge.setContactStatus(RoundedContactBadge.ContactStatus.DELIVERED);
                else {
                    boolean smsSent = getResultCode() == Activity.RESULT_OK;
                    contactBadge.setContactStatus(smsSent ? RoundedContactBadge.ContactStatus.SENT : RoundedContactBadge.ContactStatus.ERROR);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(SMS_STATUS_SENT_ACTION);
        filter.addAction(SMS_STATUS_DELIVERED_ACTION);
        registerReceiver(sentSmsStatusReceiver, filter);

        loadContactsIfNeeded();
        if (savedInstanceState == null)
            alertAllContacts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsUpdateReceiver);
        unregisterReceiver(sentSmsStatusReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("markers", allMarkersList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<MarkerOptions> savedMarkers = savedInstanceState.getParcelableArrayList("markers");
        if (savedMarkers != null)
            for (MarkerOptions marker : savedMarkers)
                addMarker(marker);
    }

    @Override
    protected void onStart() {
        super.onStart();
        shutdownButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.expand));
        findViewById(R.id.summary_card_view).startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_up));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.removeUpdates(this);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        switch (i) {
            case DialogInterface.BUTTON_POSITIVE:
                finish();
                Intent intent = new Intent(SummaryActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;

            case DialogInterface.BUTTON_NEUTRAL:
                alertAllContacts();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.shutdown_button:
                onBackPressed();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        boolean replyAlways = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsActivity.PREFERENCES_REPLY_LOCATION_REQUESTS_ALWAYS_KEY, false);
        new AlertDialog.Builder(SummaryActivity.this)
                .setTitle(R.string.stop_alarm_dialog_title)
                .setMessage(replyAlways ? R.string.stop_alarm_dialog_message_1 : R.string.stop_alarm_dialog_message_2)
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, this)
                .setNeutralButton(R.string.stop_alarm_dialog_neutral_button, this)
                .setIcon(R.drawable.ic_power_settings)
                .show();
    }

    private void alertAllContacts() {
        for (int i = 0; i < contactsLinearLayout.getChildCount(); i++) {
            RoundedContactBadge contactBadge = (RoundedContactBadge) contactsLinearLayout.getChildAt(i);
            contactBadge.setContactStatus(RoundedContactBadge.ContactStatus.NONE);
        }

        Location location = LocationService.getBestLastKnownLocation(this);
        String gmapsUrl = LocationService.getGoogleMapsUrl(location);
        if (gmapsUrl == null) {
            summaryTextView.setText(R.string.summary_negative_text);
            summaryTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            return;
        }
        summaryTextView.setText(R.string.summary_positive_text);
        summaryTextView.setTextColor(summaryTextViewDefaultColor);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultBody = getString(R.string.default_sms_body);
        String smsBody = prefs.getString(SettingsActivity.PREFERENCES_SMS_BODY_KEY, defaultBody) + " " + gmapsUrl;

        SmsManager smsManager = SmsManager.getDefault();
        for (int i = 0; i < favoriteContacts.size(); i++) {
            // (!) requestCode==i deve essere diverso in ogni chiamata PendingIntent.getBroadcast
            // per avere più PendingIntent attivi nello stesso tempo (v. Overview di PendingIntent).
            // Se requestCode fosse costante, verrebbe notificato soltanto lo stato dell'ultimo SMS
            // inviato.

            Intent intent1 = new Intent(SMS_STATUS_SENT_ACTION);
            intent1.putExtra(CONTACT_INDEX_EXTRA, i);
            PendingIntent sentPI = PendingIntent.getBroadcast(this, i, intent1, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intent2 = new Intent(SMS_STATUS_DELIVERED_ACTION);
            intent2.putExtra(CONTACT_INDEX_EXTRA, i);
            PendingIntent deliverPI = PendingIntent.getBroadcast(this, i, intent2, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

            smsManager.sendTextMessage(favoriteContacts.get(i), null, smsBody, sentPI, null);
            // TODO: Molti operatori fanno pagare la conferma di ricezione. Quindi per ora deliverPI non viene usato.
        }

        MarkerOptions marker = new MarkerOptions()
                .anchor(0, 1)
                .icon(mapMarkerBitmapDescriptor)
                .title(DateFormat.getTimeInstance().format(new Date()))
                .position(new LatLng(location.getLatitude(), location.getLongitude()));
        addMarker(marker);
    }

    private void loadContactsIfNeeded() {
        List<String> updatedContacts = new ArrayList<>(CustodeUtils.getFavoriteContacts(this));

        if (favoriteContacts != null && favoriteContacts.containsAll(updatedContacts))
            return;
        favoriteContacts = updatedContacts;

        int badgeSide = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(badgeSide, badgeSide);
        layoutParams.setMargins(10, 0, 10, 0);
        contactsLinearLayout.removeAllViews();

        for (String number : favoriteContacts) {
            RoundedContactBadge contactBadge = new RoundedContactBadge(this);
            contactBadge.assignContactFromPhone(number, false);
            Bitmap contactPhoto = CustodeUtils.getContactPhoto(this, number);
            if (contactPhoto == null)
                contactBadge.setImageToDefault();
            else
                contactBadge.setImageBitmap(contactPhoto);
            contactsLinearLayout.addView(contactBadge, layoutParams);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        SummaryActivity.this.googleMap = googleMap;
        googleMap.setTrafficEnabled(false);
        googleMap.setBuildingsEnabled(false);
        googleMap.setOnMyLocationButtonClickListener(this);

        UiSettings mapUi = googleMap.getUiSettings();
        mapUi.setMapToolbarEnabled(false);
        mapUi.setAllGesturesEnabled(true);
        mapUi.setTiltGesturesEnabled(false);
        mapUi.setMyLocationButtonEnabled(true);

        for (MarkerOptions markerOptions : pendingMarkersList)
            addMarker(markerOptions);
        pendingMarkersList.clear();

        followUser = true;
        if (ActivityCompat.checkSelfPermission(SummaryActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            googleMap.setMyLocationEnabled(true);

        Location location = LocationService.getBestLastKnownLocation(SummaryActivity.this);
        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        }
    }

    private void addMarker(MarkerOptions marker) {
        if (googleMap == null)
            pendingMarkersList.add(marker);
        else {
            allMarkersList.add(marker);
            googleMap.addMarker(marker);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        followUser = true;
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getFragmentManager().findFragmentById(R.id.map_fragment).getView();

        boolean mapTouched = view != null
                && ev.getX() > view.getX() && ev.getX() < view.getRight()
                && ev.getY() > view.getY() && ev.getY() < view.getBottom();
        if (ev.getActionMasked() == MotionEvent.ACTION_MOVE && mapTouched)
            followUser = false;

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (followUser && location != null && googleMap != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

}
