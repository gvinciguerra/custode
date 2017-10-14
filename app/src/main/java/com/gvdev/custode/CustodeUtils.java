package com.gvdev.custode;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.WindowManager;

import com.gvdev.custode.activities.SettingsActivity;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

public class CustodeUtils {

    /** Restituisce l'immagine del contatto per un numero di telefono. */
    public static Bitmap getContactPhoto(Context context, String phoneNumber) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        String contactId;
        if (cursor != null && cursor.moveToFirst()) {
            contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            cursor.close();
        }
        else
            return null;

        Bitmap photo = null;
        try {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId)));
            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
        } catch (IOException ignored) {

        }
        return photo;
    }

    /** Restituisce il nome del contatto per un numero di telefono. */
    public static String getContactName(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        String contactName = phoneNumber;
        if (cursor != null && cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            cursor.close();
        }
        return contactName;
    }

    /** Restituisce l'insieme dei numeri di contatti preferiti dell'utente. */
    public static Set<String> getFavoriteContacts(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getStringSet(SettingsActivity.PREFERENCES_CONTACTS_KEY, new HashSet<String>());
    }

    public static void setScreenBrightness(Activity activity, float value) {
        WindowManager.LayoutParams layoutParams = activity.getWindow().getAttributes();
        layoutParams.screenBrightness = value;
        activity.getWindow().setAttributes(layoutParams);
    }

    public static String getSavedPin(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(SettingsActivity.PREFERENCES_PIN_KEY, null);
    }

    public static String SHA1(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(text.getBytes("UTF-8"), 0, text.length());
            byte[] sha1hash = md.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : sha1hash) {
                String h = Integer.toHexString(0xFF & b);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (Exception ignored) {

        }
        return null;
    }

     // Da https://android.googlesource.com/platform/packages/apps/ContactsCommon/+/master/src/com/android/contacts/common/util/BitmapUtil.java
     public static Bitmap getRoundedBitmap(Bitmap input, int targetWidth, int targetHeight) {
         if (input == null) {
             return null;
         }
         final Bitmap.Config inputConfig = input.getConfig();
         final Bitmap result = Bitmap.createBitmap(targetWidth, targetHeight,
                 inputConfig != null ? inputConfig : Bitmap.Config.ARGB_8888);
         final Canvas canvas = new Canvas(result);
         final Paint paint = new Paint();
         canvas.drawARGB(0, 0, 0, 0);
         paint.setAntiAlias(true);
         final RectF dst = new RectF(0, 0, targetWidth, targetHeight);
         canvas.drawOval(dst, paint);

         // Specifies that only pixels present in the destination (i.e. the drawn oval) should
         // be overwritten with pixels from the input bitmap.
         paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

         final int inputWidth = input.getWidth();
         final int inputHeight = input.getHeight();

         // Choose the largest scale factor that will fit inside the dimensions of the
         // input bitmap.
         final float scaleBy = Math.min((float) inputWidth / targetWidth,
                 (float) inputHeight / targetHeight);

         final int xCropAmountHalved = (int) (scaleBy * targetWidth / 2);
         final int yCropAmountHalved = (int) (scaleBy * targetHeight / 2);

         final Rect src = new Rect(
                 inputWidth / 2 - xCropAmountHalved,
                 inputHeight / 2 - yCropAmountHalved,
                 inputWidth / 2 + xCropAmountHalved,
                 inputHeight / 2 + yCropAmountHalved);

         canvas.drawBitmap(input, src, dst, paint);
         return result;
     }

}
