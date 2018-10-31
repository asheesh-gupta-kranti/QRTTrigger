package poc.android.com.qrttrigger;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

/**
 * Created by ashutoshmishra on 31/10/18.
 */

public class BacgroundService extends Service {
    private static final String TAG = "MyLocationService";
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1; // 1 min
    private static final float LOCATION_DISTANCE = 2f; // 10 meters
    public static String CHANNEL_ONE_ID = "com.qrtservices";
    public static final int foregroundServiceId = 100;

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            Toast.makeText(BacgroundService.this, "Location:" + location.getLatitude() + "," + location.getLongitude(), Toast.LENGTH_SHORT).show();
            mLastLocation.set(location);
            postLocation(location.getLatitude(), location.getLongitude());
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    /*
    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    */

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.PASSIVE_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {

        Log.e(TAG, "onCreate");

        initializeLocationManager();

        if (mLocationManager.getAllProviders() != null ) {

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                // permission not granted
                return;
            }
            if (mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
                    showNotification();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //Or  Register the listener with Location Manager's gps provider
            if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                try {
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
                    showNotification();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

//        try {
//            mLocationManager.requestLocationUpdates(
//                    LocationManager.GPS_PROVIDER,
//                    LOCATION_INTERVAL,
//                    LOCATION_DISTANCE,
//                    mLocationListeners[0]
//            );
//        } catch (java.lang.SecurityException ex) {
//            Log.i(TAG, "fail to request location update, ignore", ex);
//        } catch (IllegalArgumentException ex) {
//            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
//        }

        /*try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_INTERVAL,
                    LOCATION_DISTANCE,
                    mLocationListeners[1]
            );
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }*/
    }

    private void showNotification(){
        Notification notification = null;
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    "Location service", IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            notification = new Notification.Builder(getApplicationContext())
                    .setChannelId(CHANNEL_ONE_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("We are monitoring your location.")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();
        }else{
            notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("We are monitoring your location.")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .build();

        }

        // Start foreground service
        startForeground(foregroundServiceId, notification);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex);
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager - LOCATION_INTERVAL: " + LOCATION_INTERVAL + " LOCATION_DISTANCE: " + LOCATION_DISTANCE);
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void postLocation(double lat, double lng) {

        String tripId = MainActivity.getTripId(this);
        if (tripId.isEmpty())
            return;

        String url = MainActivity.baseUrl + "/api/tracktriggers";
        Log.d("url", url);
        JSONObject payload = new JSONObject();
        try {
            payload.put("tripId", tripId);
            JSONObject location = new JSONObject();
            location.put("lat", lat);
            location.put("lng", lng);
            payload.put("passengerLocation", location);
            payload.put("timestamp", MainActivity.getDateString(new Date(), "yyyy-MM-dd'T'HH:mm:ss'Z'"));
        }catch (Exception ex){
            ex.printStackTrace();
        }


        Log.d("payload", payload.toString());
        UTF8JsonObjectRequest request = new UTF8JsonObjectRequest(Request.Method.POST, url, payload, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("Trip response", response.toString());

//                Toast.makeText(BacgroundService.this, "Successful", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", "" + error);

//                Toast.makeText(BacgroundService.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> header = new HashMap<>();
                header.put("content-type",
                        "application/json");

                return header;
            }
        };

        RetryPolicy retryPolicy = new DefaultRetryPolicy(
                AppController.VOLLEY_TIMEOUT,
                AppController.VOLLEY_MAX_RETRIES,
                AppController.VOLLEY_BACKUP_MULT);
        request.setRetryPolicy(retryPolicy);
        AppController.getInstance().addToRequestQueue(request);




    }
}