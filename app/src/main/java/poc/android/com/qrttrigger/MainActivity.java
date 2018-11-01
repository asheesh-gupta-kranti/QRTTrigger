package poc.android.com.qrttrigger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String baseUrl = "http://ec2-13-232-185-241.ap-south-1.compute.amazonaws.com:3000";
    private FusedLocationProviderClient mFusedLocationClient;

    private Button btnStartTrip, btnTrigger;
    private ProgressBar progressBar;
    private  Location myLocation;
    private String tripId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartTrip = findViewById(R.id.btn_start_trip);
        btnTrigger = findViewById(R.id.btn_trigger);
        progressBar = findViewById(R.id.progress_bar);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnStartTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                startTripPost();
            }
        });

        btnTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                triggerPost();
            }
        });
    }


    private void startTripPost() {

        try {

            JSONObject payload = new JSONObject();
            tripId = "t"+ Calendar.getInstance().getTimeInMillis();
            payload.put("orgId", "uber");
            payload.put("tripId", tripId);
            payload.put("tripStartTime", getDateString(new Date(), "yyyy-MM-dd'T'HH:mm:ss'Z'"));
            payload.put("tripStatus", "OK");
            JSONObject tripStartLoc = new JSONObject();
            tripStartLoc.put("lat", 27.086);
            tripStartLoc.put("lng", 71.833);
            payload.put("tripStartLoc", tripStartLoc);

            String url = baseUrl + "/api/trips";
            Log.d("payload", payload.toString());
            UTF8JsonObjectRequest request = new UTF8JsonObjectRequest(Request.Method.POST, url, payload, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("Trip response", response.toString());
                    Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                  progressBar.setVisibility(View.GONE);
                  initCurrentLocation();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("error", "" + error);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
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
        } catch (Exception ex) {
            Log.d("error", ex.getMessage());
        }

    }

    public static String getDateString(Date date, String dateFormate) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormate);
        return simpleDateFormat.format(date);
    }

    private void initCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Please enable Location permission.", Toast.LENGTH_SHORT).show();
            return;
        }

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            btnTrigger.setEnabled(true);
                            myLocation = location;
                        } else {

                            final LocationManager locationManager = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE);

                            //Location Listener is an interface. It will be called every time when the location manager reacted.
                            LocationListener locationListener = new LocationListener() {
                                public void onLocationChanged(Location location) {

                                    // This method is called when a new location is found by the network location provider or Gps provider.
                                    locationManager.removeUpdates(this);
                                    if (location != null) {
                                        btnTrigger.setEnabled(true);
                                        myLocation = location;
                                    }
                                }

                                public void onStatusChanged(String provider, int status, Bundle extras) {
                                   System.out.println("initCurrentLocation LocationManager onStatusChnaged");
                                }

                                public void onProviderEnabled(String provider) {
                                    System.out.println("initCurrentLocation LocationManager onProviderEnabled");
                                }

                                public void onProviderDisabled(String provider) {
                                    System.out.println("initCurrentLocation LocationManager onProviderDisabled");
                                }
                            };

                            // Register the listener with Location Manager's network provider
                            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            assert locationManager != null;
                            if (locationManager.getAllProviders() != null) {
                                if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                                    try {
                                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                                    } catch (Exception e) {
                                    }
                                }
                                //Or  Register the listener with Location Manager's gps provider
                                if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                                    try {
                                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println
                                ("OnFailureListener");
                    }
                });

    }

    private void triggerPost() {

        if (myLocation == null){
            return;
        }

        try {
            String payloadStr = "{\n" +
                    "  \"passengerName\": \"test1\",\n" +
                    "  \"passengerPhone\": \"test1\",\n" +
                    "  \"passengerEmerName\": \"test1\",\n" +
                    "  \"passengerEmerPhone\": \"test1\",\n" +
                    "  \"driverName\": \"test1\",\n" +
                    "  \"driverPhone\": \"test1\",\n" +
                    "  \"triggerStatus\": \"HELP\"\n" +
                    "}";

            JSONObject payload = new JSONObject(payloadStr);


            payload.put("triggeredTimestamp", getDateString(new Date(), "yyyy-MM-dd'T'HH:mm:ss'Z'"));

            JSONObject triggerdLocation = new JSONObject();
            triggerdLocation.put("lat", myLocation.getLatitude());
            triggerdLocation.put("lng", myLocation.getLongitude());
            payload.put("triggerdLocation", triggerdLocation);
            payload.put("tripId", tripId);

            String url = baseUrl + "/api/triggers";

            Log.d("payload", payload.toString());
            UTF8JsonObjectRequest request = new UTF8JsonObjectRequest(Request.Method.POST, url, payload, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("Trip response", response.toString());

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("error", "" + error.getLocalizedMessage());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
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
        } catch (Exception ex) {
            Log.d("error", ex.getMessage());
        }

    }
}
