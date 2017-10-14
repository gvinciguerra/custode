package com.gvdev.custode;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.gvdev.custode.activities.SettingsActivity;

import java.util.Set;

/**
 * Un BroadcastReceiver che per ogni SMS ricevuto dall'utente controlla se (1) è stato spedito da
 * uno dei contatti preferiti, (2) contiene MAGIC_SMS_BODY nel testo e (3) non è stato superato il
 * limite di SMS inviati. Se le tre condizioni sono verificate risponde al mittente con la
 * posizione del dispositivo.
 */
public class SmsUpdateReceiver extends BroadcastReceiver {

    private Set<String> favoriteContacts;
    public static final String MAGIC_SMS_BODY = "gps";
    private static final String DELIVERED_SMS_ACTION = "com.gvdev.custode.DELIVERED_SMS_ACTION";

    public SmsUpdateReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") && intent.getExtras() != null) {
            favoriteContacts = CustodeUtils.getFavoriteContacts(context);

            Object[] data = (Object[]) intent.getExtras().get("pdus");
            if (data != null)
                for (Object pdu : data) {
                    SmsMessage message;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        String format = intent.getExtras().getString("format");
                        message = SmsMessage.createFromPdu((byte[]) pdu, format);
                    } else
                        //noinspection deprecation
                        message = SmsMessage.createFromPdu((byte[]) pdu);
                    if (message != null)
                        processSMS(context, message);
                }
        }
    }

    private void processSMS(Context context, SmsMessage message) {
        final String smsBody = message.getDisplayMessageBody();
        final String smsSender = message.getDisplayOriginatingAddress();

        if (!smsBody.toLowerCase().contains(MAGIC_SMS_BODY.toLowerCase()))
            return;

        boolean smsFromFavoriteContact = false;
        for (String favoriteContact : favoriteContacts)
            if (PhoneNumberUtils.compare(smsSender, favoriteContact)) {
                smsFromFavoriteContact = true;
                break;
            }
        if (!smsFromFavoriteContact)
            return;

        String sendBody = LocationService.getGoogleMapsUrl(LocationService.getBestLastKnownLocation(context));
        if (sendBody == null)
            return;

        final String contactName = CustodeUtils.getContactName(context, smsSender);
        final Bitmap contactPhoto = CustodeUtils.getContactPhoto(context, smsSender);
        if (!incrementSentSmsCounter(context)) {
            String text = context.getString(R.string.sms_limit_reached_notification_text, contactName);
            String title = context.getString(R.string.sms_limit_reached_notification_title);
            sendNotification(context, title, text, contactPhoto);
            return;
        }

        Intent deliveryIntent = new Intent(DELIVERED_SMS_ACTION);
        PendingIntent deliverPI = PendingIntent.getBroadcast(context.getApplicationContext(), 0, deliveryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String title = context.getString(R.string.sms_sent_notification_title);
                String text = context.getString(R.string.sms_sent_notification_text, contactName);
                sendNotification(context, title, text, contactPhoto);
            }
        }, new IntentFilter(DELIVERED_SMS_ACTION));

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(smsSender, null, sendBody, null, deliverPI);
    }

    private void sendNotification(Context context, String title, String text, Bitmap contactPhoto) {
        boolean lollipop = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
        if (lollipop && contactPhoto != null) {
            int side = Math.min(contactPhoto.getWidth(), contactPhoto.getHeight());
            contactPhoto = CustodeUtils.getRoundedBitmap(contactPhoto, side, side);
        }
        Notification notification = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setContentText(text)
                .setContentTitle(title)
                .setLargeIcon(contactPhoto)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setColor(ContextCompat.getColor(context, R.color.light_blue_A400))
                .setSmallIcon(lollipop ? R.drawable.ic_notification : R.mipmap.ic_launcher)
                .build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }

    /** Restituisce true se e solo se il limite orario degli SMS non è stato superato. In tal caso incrementa il contatore. */
    private boolean incrementSentSmsCounter(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int smsLimit = prefs.getInt(SettingsActivity.PREFERENCES_SMS_LIMIT_KEY, 10);
        int smsCount = prefs.getInt(SettingsActivity.PREFERENCES_SMS_LIMIT_COUNT_KEY, 0);
        long oldestSmsDate = prefs.getLong(SettingsActivity.PREFERENCES_SMS_LIMIT_DATE_KEY, 0);
        boolean isSmsCounterInvalid = System.currentTimeMillis() - oldestSmsDate >= 60 * 60 * 1000;
        boolean isSmsLimitReached = smsCount >= smsLimit;
        boolean canSendSms = isSmsCounterInvalid || !isSmsLimitReached;

        if (isSmsCounterInvalid)
            prefs.edit()
                    .putInt(SettingsActivity.PREFERENCES_SMS_LIMIT_COUNT_KEY, 1)
                    .putLong(SettingsActivity.PREFERENCES_SMS_LIMIT_DATE_KEY, System.currentTimeMillis())
                    .apply();
        else if (!isSmsLimitReached)
            prefs.edit().putInt(SettingsActivity.PREFERENCES_SMS_LIMIT_COUNT_KEY, ++smsCount).apply();

        return canSendSms;
    }

}
