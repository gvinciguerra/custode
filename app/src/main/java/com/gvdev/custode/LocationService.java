package com.gvdev.custode;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Un service che traccia la posizione dell'utente e invia un local broadcast intent ad ogni
 * cambiamento significativo.
 *
 * Il broadcast conterrà un extra con nome EXTRA_LOCATION e con valore un oggetto Location.
 *
 * Se lanciato con Intent.setAction(LocationService.GEOCODE_ON_ACTION) aggiunge anche un extra
 * con nome EXTRA_GEOCODE e valore una stringa con l'indirizzo dell'utente.
 */
public class LocationService extends Service implements LocationListener {

    static final public String EXTRA_GEOCODE = "com.gvdev.custode.geocode";
    static final public String EXTRA_LOCATION = "com.gvdev.custode.location";
    static final public String GEOCODE_ON_ACTION = "com.gvdev.custode.geocode-on";
    static final public String GEOCODE_OFF_ACTION = "com.gvdev.custode.geocode-off";
    static final public String LOCATION_CHANGED_ACTION = "com.gvdev.custode.location-changed";

    private boolean mustGeocode = false;
    private Geocoder geocoder;
    private String locationString;
    private LocationManager locationManager;
    private LocalBroadcastManager localBroadcastManager;

    @Override
    public void onCreate() {
        geocoder = new Geocoder(LocationService.this, Locale.getDefault());
        locationString = getResources().getString(R.string.unknown_location);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onDestroy() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.removeUpdates(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            final int minTime = (int) TimeUnit.MINUTES.toMillis(4);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, 150, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, 150, this);
            if (intent.getAction() != null) {
                if (intent.getAction().equals(GEOCODE_ON_ACTION))
                    mustGeocode = true;
                else if (intent.getAction().equals(GEOCODE_OFF_ACTION))
                    mustGeocode = false;
            }
            sendLocalBroadcastIntent(getBestLastKnownLocation(this));
        }
        return START_REDELIVER_INTENT;
    }


    private void sendLocalBroadcastIntent(final Location location) {
        if (location == null)
            return;

        if (mustGeocode)
            new GeocodeAsyncTask().execute(location);
        else {
            Intent intent = new Intent(LOCATION_CHANGED_ACTION);
            intent.putExtra(EXTRA_LOCATION, location);
            localBroadcastManager.sendBroadcast(intent);
        }
    }

    /**
     * Restituice la posizione più accurata e più recente del dispositivo, scelta tra tutti i location provider disponibili.
     */
    public static Location getBestLastKnownLocation(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getAllProviders();
        Location bestLocation = null;

        for (String provider : providers) {
            try {
                Location location = locationManager.getLastKnownLocation(provider);
                if (bestLocation == null || location != null
                        && location.getElapsedRealtimeNanos() > bestLocation.getElapsedRealtimeNanos()
                        && location.getAccuracy() > bestLocation.getAccuracy())
                    bestLocation = location;
            } catch (SecurityException ignored) {
            }
        }

        return bestLocation;
    }

    /**
     * Restituisce un url a Google Maps con la posizione indicata.
     */
    public static String getGoogleMapsUrl(Location location) {
        if (location == null)
            return null;
        return String.format(Locale.US, "http://maps.google.com/?q=%.5f,%.5f", location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onLocationChanged(final Location location) {
        sendLocalBroadcastIntent(location);
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class GeocodeAsyncTask extends AsyncTask<Location, Void, Address> {
        Location location;

        @Override
        protected Address doInBackground(Location... params) {
            try {
                location = params[0];
                List<Address> addr = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                return (addr == null || addr.isEmpty()) ? null : addr.get(0);
            } catch (IOException ignored) {

            }
            return null;
        }

        @Override
        protected void onPostExecute(Address address) {
            if (address == null)
                return;

            StringBuilder builder = new StringBuilder();
            if (address.getMaxAddressLineIndex() >= 0)
                builder.append(address.getAddressLine(0));
            if (address.getLocality() != null) {
                builder.append(", ");
                builder.append(address.getLocality());
            }
            locationString = builder.toString();

            Intent intent = new Intent(LOCATION_CHANGED_ACTION);
            intent.putExtra(EXTRA_LOCATION, location);
            intent.putExtra(EXTRA_GEOCODE, locationString);
            localBroadcastManager.sendBroadcast(intent);
        }

    }
}
