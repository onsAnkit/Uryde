package uryde.passenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import uryde.passenger.model.GooglePlacesResultData;
import uryde.passenger.searching.PathJSONParser;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.GPSTracker;
import uryde.passenger.util.HttpConnection;
import uryde.passenger.util.PrefsHelper;
import static java.lang.Math.abs;

public class JourneyStarted extends AppCompatActivity implements OnMapReadyCallback {

    private Pubnub pubnub;
    private GPSTracker tracker;
    private PrefsHelper mHelper;
    private ImageView driverPic;
    private GoogleMap mGoogleMap;
    private RatingBar driverRate;
    private ProgressDialog mDialog;
    private Marker markerMapStarted;
    private String dropLat, dropLong;
    public static boolean visibility;
    private ConnectionDetector detector;
    private LocationManager locationManager;
    private double currentLongitude, currentLatitude;
    private static final String TAG = JourneyStarted.class.getSimpleName();
    private TextView distance, time, networkText, title, carNumber, bookingId, driverName, carName,carColor;

    private IntentFilter mFilter;
    private Timer myTimer_publish;
    private String pickLat, pickLong;
    private RelativeLayout networkBar;
    private BroadcastReceiver mReceiver;
    private TimerTask myTimerTask_publish;
    private boolean isMarkerRotating;
    private String bearing= "0.0";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey_started);
        tracker = new GPSTracker(JourneyStarted.this);
        currentLatitude = tracker.getLatitude();
        currentLongitude = tracker.getLongitude();

        init();
        pubnub = new Pubnub(Constants.PUBNUB_PUBLISH_KEY, Constants.PUBNUB_SUBSCRIBE_KEY, "", true);

        mFilter = new IntentFilter();
        mFilter.addAction(Constants.NEW_APPOINTMENT_BROADCAST);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra("status");
                if (status.equals("2")) {
                    Log.d(TAG, status);
                    mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                    mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                    mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                    startActivity(new Intent(JourneyStarted.this, Invoice.class));
                    overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                    finish();
                }
            }
        };
    }

    /**
     * init method used to initialization
     */
    private void init() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mHelper = new PrefsHelper(JourneyStarted.this);
        detector = new ConnectionDetector(JourneyStarted.this);
        mDialog = new ProgressDialog(JourneyStarted.this);

        mDialog.setCancelable(false);
        time = (TextView) findViewById(R.id.time);
        title = (TextView) findViewById(R.id.title);
        carName = (TextView) findViewById(R.id.car_name);
        distance = (TextView) findViewById(R.id.distance);
        carColor = (TextView) findViewById(R.id.car_color);
        bookingId = (TextView) findViewById(R.id.booking_id);
        carNumber = (TextView) findViewById(R.id.car_number);
        driverName = (TextView) findViewById(R.id.driver_name);
        driverPic = (ImageView) findViewById(R.id.driver_image);
        networkText = (TextView) findViewById(R.id.network_text);
        driverRate = (RatingBar) findViewById(R.id.driver_rating);
        networkBar = (RelativeLayout) findViewById(R.id.network_bar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, locationListener);

        time.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        title.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        carName.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        distance.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        carColor.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        bookingId.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        carNumber.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        driverName.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
        networkText.setTypeface(CommonMethods.headerFont(JourneyStarted.this));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        markerMapStarted = googleMap.addMarker(new MarkerOptions().flat(true).anchor(0.5f, 0.5f).position(latLng).title("First Point").icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red)));
    }

    /**
     * method used to get appointment details
     *
     * @param rideStatus
     * @param appointmentId contain appointment id
     */
    private void getAppointmentDetails(final String rideStatus, final String appointmentId) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GET_APPOINTMENT, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        String key = mObject.getString("response_invalid");
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String appointment = dataObject.getString("appointment");
                            JSONObject appObject = new JSONObject(appointment);
                            bookingId.setText(getString(R.string.booking_id_2131) + " " + appObject.getString("app_appointment_id"));
                            String appStatus = appObject.getString("status");
                            String driver_name = appObject.getString("driver_fname");
                            String driver_pic = appObject.getString("driver_pic");
                            String vehicle_reg_no = appObject.getString("vehicle_reg_no");
                            String vehicle_make_title = appObject.getString("vehicle_make_title");
                            String vehicle_model_title = appObject.getString("vehicle_model_title");
                            pickLat = appObject.getString("pick_latitude");
                            pickLong = appObject.getString("pick_longitude");
                            dropLat = appObject.getString("drop_latitude");
                            dropLong = appObject.getString("drop_longitude");
                            String walletBalance = appObject.getString("wallet_balance");
                            String driver_rating = appObject.getString("driver_rating");

                            driverName.setText(driver_name);
                            carNumber.setText(vehicle_reg_no);
                            carName.setText(vehicle_make_title + " - " + vehicle_model_title);
                            if (!driver_rating.equals("null")) {
                                driverRate.setRating(Float.valueOf(driver_rating));
                            } else {
                                driverRate.setRating(0.0f);
                            }

                            mHelper.savePref("pick_lat", pickLat);
                            mHelper.savePref("pick_long", pickLong);
                            mHelper.savePref("drop_lat", dropLat);
                            mHelper.savePref("drop_long", dropLong);
                            mHelper.savePref("driver_image", driver_pic);
                            mHelper.savePref(Constants.WALLET_BALANCE, walletBalance);

                            if (!driver_pic.equals("")) {
                                Glide.with(JourneyStarted.this).load(Constants.IMAGE_BASE_URL_DRIVER + driver_pic).asBitmap().placeholder(R.drawable.default_user_pic)
                                        .centerCrop().into(new BitmapImageViewTarget(driverPic) {
                                    @Override
                                    protected void setResource(Bitmap resource) {
                                        RoundedBitmapDrawable circularBitmapDrawable =
                                                RoundedBitmapDrawableFactory.create(JourneyStarted.this.getResources(), resource);
                                        circularBitmapDrawable.setCircular(true);
                                        driverPic.setImageDrawable(circularBitmapDrawable);
                                    }
                                });
                            }
                            if (appStatus.equals("3")) {
                                title.setText("Journey Started");
                                mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                                mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                                mHelper.savePref(Constants.BEGIN_JOURNEY, true);
                                if (!mHelper.getPref("driver_lat", "").equals("") && !mHelper.getPref("driver_long", "").equals("")) {

                                    mGoogleMap.clear();
                                    LatLng latLng = new LatLng(Double.parseDouble(mHelper.getPref("driver_lat", "")), Double.parseDouble(mHelper.getPref("driver_long", "")));
                                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                                    markerMapStarted = mGoogleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(new LatLng(currentLatitude, currentLongitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red)));

                                    String url = getMapsApiDirectionsFromToUrl();
                                    ReadTask downloadTask = new ReadTask();
                                    downloadTask.execute(url);

                                    /*if (!isFirsttime) {
                                        setupMap();
                                    }*/

                                } else {
                                    Log.d(TAG, "blank lat long");
                                }


                            } else if (appStatus.equals("2")) {
                                mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                                mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                                mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                                startActivity(new Intent(JourneyStarted.this, Invoice.class));
                                overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                            }
                        } else if (key.equals("1")) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(JourneyStarted.this);
                            alert.setTitle(getString(R.string.message));
                            alert.setMessage(message);
                            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub
                                    mHelper.clearAllPref();
                                    Intent intent = new Intent(JourneyStarted.this, Splash.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                    overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                                }
                            });
                            alert.show();
                        } else {
                            CommonMethods.showAlert(JourneyStarted.this, getString(R.string.message), message);
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(JourneyStarted.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(JourneyStarted.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("app_appointment_id", appointmentId);

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(JourneyStarted.this).add(mRequest);
        } else {
            CommonMethods.showAlert(JourneyStarted.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }

    }

    /**
     * method used to set location listener
     */
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            try {
                if (markerMapStarted != null) {
                    if (!mHelper.getPref("driver_lat", "").equals("") || !mHelper.getPref("driver_long", "").equals("")) {
                        Location driverLocation = new Location("starting_point");
                        driverLocation.setLatitude(Double.valueOf(mHelper.getPref("driver_lat", "0.0")));
                        driverLocation.setLongitude(Double.valueOf(mHelper.getPref("driver_long", "0.0")));

                        markerMapStarted.setPosition(new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude()));
                        markerMapStarted.setRotation(Float.parseFloat(bearing));

                    }else {
                    /*CameraPosition currentPlace = new CameraPosition.Builder(mGoogleMap.getCameraPosition())
                            .target(new LatLng(location.getLatitude(), location.getLongitude())).bearing(location.getBearing())
                            .zoom(17).build();
                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                    markerMapStarted.setRotation(location.getBearing());
                    markerMapStarted.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));*/

                        markerMapStarted.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                        markerMapStarted.setRotation(location.getBearing());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
    };

    /**
     * method used to check network state
     */
    public void checkNetworkState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!detector.isConnectingToInternet()) {
                    // not connected with internet
                    networkBar.setVisibility(View.VISIBLE);
                } else if (!ConnectionDetector.isConnectedFast(JourneyStarted.this)) {
                    // low internet connection
                    networkBar.setVisibility(View.VISIBLE);
                    networkText.setText(getString(R.string.lownetwork));
                } else {
                    // connected with internet
                    networkBar.setVisibility(View.GONE);
                }

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        visibility = true;
        if (mReceiver != null) {
            registerReceiver(mReceiver, mFilter);
        }
        myTimer_publish = new Timer();
        myTimerTask_publish = new TimerTask() {
            @Override
            public void run() {
                checkNetworkState();
            }
        };
        myTimer_publish.schedule(myTimerTask_publish, 000, 2000);

        new BackgroundSubscribeMyChannel().execute();

        getAppointmentDetails(mHelper.getPref(Constants.APPOINTMENT_STATUS, ""), mHelper.getPref(Constants.APPOINTMENT_ID, ""));
        new CallGooglePlayServices().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        visibility = false;
        unregisterReceiver(mReceiver);
        if (myTimer_publish != null) {
            myTimer_publish.cancel();
            myTimer_publish = null;
        }

        if (myTimerTask_publish != null) {
            myTimerTask_publish.cancel();
            myTimerTask_publish = null;
        }
        new BackgroundUnSubscribeAll().execute();
    }

    public void pubNubSubscribe(String[] channels) {

        for (int i = 0; i < channels.length; i++) {
        }

        try {
            pubnub.subscribe(mHelper.getPref(Constants.PUBNUB_CHANNEL_TYPE, ""), new Callback() {

                        @Override
                        public void connectCallback(String channel, Object message) {
                        }

                        @Override
                        public void disconnectCallback(String channel, Object message) {
                        }

                        public void reconnectCallback(String channel, Object message) {
                        }

                        @Override
                        public void successCallback(String channel, final Object message) {
                            Log.d(TAG, message.toString());
                            try {
                                Handler refresh = new Handler(Looper.getMainLooper());
                                refresh.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        getDriverDetails(message.toString());
                                    }
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                                Handler refresh = new Handler(Looper.getMainLooper());
                                Log.d(TAG, e.toString());
                                refresh.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        CommonMethods.showAlert(JourneyStarted.this, getString(R.string.attention), getString(R.string.something_wrong));
                                    }
                                });
                            }

                        }

                        @Override
                        public void errorCallback(String channel, PubnubError error) {
                        }
                    }
            );

        } catch (PubnubException e) {
            Log.d(TAG, e.toString());
        }
    }

    private void getDriverDetails(String message) {
        Log.d(TAG + " Driver Location:- ", message);

        try {
            JSONObject mObject = new JSONObject(message);
            String driverLat = mObject.getString("lt");
            String driverLong = mObject.getString("lg");
            String status = mObject.getString("st");
            bearing = mObject.getString("bearing");

            mHelper.savePref("driver_lat", driverLat);
            mHelper.savePref("driver_long", driverLong);

            if (status.equals("3")) {
//                UpdateDriverLocation_JourneyStarted(driverLat, driverLong)

            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }

    class BackgroundSubscribeMyChannel extends AsyncTask<String, Void, String> {
        String[] new_channels;

        @Override
        protected String doInBackground(String... params) {
            new_channels = new String[1];
            new_channels[0] = mHelper.getPref(Constants.PUBNUB_CHANNEL_TYPE, "");
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pubNubSubscribe(new_channels);
        }
    }

    class BackgroundUnSubscribeAll extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            pubnub.unsubscribeAll();
            return null;
        }
    }

    private class CallGooglePlayServices extends AsyncTask<String, Void, GooglePlacesResultData> {
        @Override
        protected GooglePlacesResultData doInBackground(String... params) {
            // TODO Auto-generated method stub

            Log.d(TAG + " Pick Lat-- ", mHelper.getPref("pick_lat", "") + " --Pick Long " + mHelper.getPref("pick_long", ""));
            Log.d(TAG + " Driver Lat-- ", mHelper.getPref("driver_lat", "") + " --Driver Long " + mHelper.getPref("driver_long", ""));

            String url = null;
            if (mHelper.getPref(Constants.DRIVER_ON_THE_WAY, false)) {
                url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + mHelper.getPref("driver_lat", "") + ","
                        + mHelper.getPref("driver_long", "") + "&destination=" + mHelper.getPref("pick_lat", "")
                        + "," + mHelper.getPref("pick_long", "") + "&key=AIzaSyBzODqsp14ssicSubR5G_vljN4ObOp8WuU";
//+ Constants.SEARCH_ADDRESS_API_KEY

                System.out.println("google url=" + url);
            } else if (mHelper.getPref(Constants.DRIVER_ARRIVED, false) || mHelper.getPref(Constants.BEGIN_JOURNEY, false)) {
                url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + mHelper.getPref("pick_lat", "") + ","
                        + mHelper.getPref("pick_long", "") + "&destination=" + mHelper.getPref("drop_lat", "") + ","
                        + mHelper.getPref("drop_long", "") + "&key=AIzaSyBzODqsp14ssicSubR5G_vljN4ObOp8WuU";
//+ Constants.SEARCH_ADDRESS_API_KEY
                System.out.println("google url=" + url);
            }

            String data = null;
            try {
                // Fetching the data from web service in background
                if (url != null)
                    data = CommonMethods.callhttpRequest(url);
            } catch (Exception e) {
                System.out.println("Background Task" + e.toString());
            }

            GooglePlacesResultData parseData = null;
            if (data != null) {
                System.out.println("CallGooglePlayServices data=" + data);
                Gson gson = new Gson();
                parseData = gson.fromJson(data, GooglePlacesResultData.class);
            }
            return parseData;
        }

        @Override
        protected void onPostExecute(GooglePlacesResultData result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);

            if (result != null) {
                if (result.getRoutes() != null && result.getRoutes().size() > 0) {
                    System.out.println("CallGooglePlayServices getDistance="
                            + result.getRoutes().get(0).getLegs().get(0).getDistance().getText());
                    System.out.println("CallGooglePlayServices getDuration="
                            + result.getRoutes().get(0).getLegs().get(0).getDuration().getText());
                    distance.setText(getResources().getString(R.string.dist) + " - "
                            + result.getRoutes().get(0).getLegs().get(0).getDistance().getText());
                    time.setText(getResources().getString(R.string.eta) + " - "
                            + result.getRoutes().get(0).getLegs().get(0).getDuration().getText());
                }
            }
        }
    }

    private void UpdateDriverLocation_JourneyStarted(String driverLat, String driverLong) {
        if (driverLat.equals("") || driverLong.equals("")) {
            Log.d(TAG, "Some error");
        } else {
            mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
            mHelper.savePref(Constants.DRIVER_ARRIVED, false);
            mHelper.savePref(Constants.BEGIN_JOURNEY, true);
            double driver_current_latitude = Double.parseDouble(driverLat);
            double driver_cuttent_longitude = Double.parseDouble(driverLong);

            mHelper.savePref("driver_lat", String.valueOf(driver_current_latitude));
            mHelper.savePref("driver_long", String.valueOf(driver_cuttent_longitude));

            new CallGooglePlayServices().execute();
           /* try {
                Location driverLocation = new Location("starting_point");
                driverLocation.setLatitude(driver_current_latitude);
                driverLocation.setLongitude(driver_cuttent_longitude);

                Location destLocation = new Location("drop_point");
                destLocation.setLatitude(Double.valueOf(dropLat));
                destLocation.setLongitude(Double.valueOf(dropLong));

                CameraPosition currentPlace = new CameraPosition.Builder(mGoogleMap.getCameraPosition())
                        .target(new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude())).bearing(driverLocation.getBearing()).zoom(17).build();

                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

            Toast.makeText(RideInfo.this, driverLocation.getBearing() + "", Toast.LENGTH_SHORT).show();
            CameraPosition currentPlace = new CameraPosition.Builder(mGoogleMap.getCameraPosition())
                    .target(latLng).bearing(driverLocation.getBearing()).zoom(17).build();

            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

            markerMapArrived.setRotation(driverLocation.getBearing());
            markerMapArrived.setPosition(latLng);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, e.toString());
            }*/
        }
    }

    private String getMapsApiDirectionsFromToUrl() {
        LatLng latLngDrop = new LatLng(Double.parseDouble(mHelper.getPref("drop_lat", "")),
                Double.parseDouble(mHelper.getPref("drop_long", "")));

        mGoogleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLngDrop).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_markers_dropoff)));

        String waypoints = "origin=" + mHelper.getPref("pick_lat", "") + "," + mHelper.getPref("pick_long", "") + "&" + "destination="
                + mHelper.getPref("drop_lat", "") + "," + mHelper.getPref("drop_long", "");

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + params;
        return url;
    }

    class ReadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                System.out.println("Background Task" + e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {

            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            // traversing through routes
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                polyLineOptions.addAll(points);
                polyLineOptions.width(5);
                polyLineOptions.color(Color.RED);
            }
            if (polyLineOptions != null)
                mGoogleMap.addPolyline(polyLineOptions);
        }
    }

    private double bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        if (!isMarkerRotating) {
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
            final long duration = 200;
            final float angle  = toRotation%360;

            final Interpolator interpolator = new LinearInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    isMarkerRotating = true;

                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);

                    //float rot = t * angle + (1 - t) * startRotation;

                    float factor = (angle - startRotation);

                    if (abs(factor) > 270)
                        factor = angle < startRotation ? angle : startRotation;

                    float rot = t * factor + startRotation;

                    float bearing = -rot > 180 ? rot / 2 : rot;

                    if(bearing>0 && bearing <360)
                        marker.setRotation(bearing);

                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        isMarkerRotating = false;
                    }
                }
            });
        }
    }

}
