package uryde.passenger;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.Property;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import com.google.android.gms.maps.model.MapStyleOptions;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
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
import java.util.TimeZone;
import java.util.Timer;

import uryde.passenger.adapter.CancelReasonAdapter;
import uryde.passenger.model.GooglePlacesResultData;
import uryde.passenger.navigation.Navigator;
import uryde.passenger.searching.PathJSONParser;
import uryde.passenger.services.TimerService;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.GPSTracker;
import uryde.passenger.util.HttpConnection;
import uryde.passenger.util.PrefsHelper;

import static java.lang.Math.abs;

public class RideInfo extends BaseActivity implements View.OnClickListener, OnMapReadyCallback {

    private Pubnub pubnub;
    private Dialog dialog;
    private TextView time;
    private TextView title;
    private TextView carName;
    private TextView distance;
    private TextView bookingId;
    private TextView carNumber;
    private GPSTracker tracker;
    private TextView driverName;
    private ImageView carImage;
    private ImageView driverPic;
    private PrefsHelper mHelper;
    private RatingBar driverRate;
    private GoogleMap mGoogleMap;
    private ProgressDialog mDialog;
    private TextView dropOffAddress;
    private LinearLayout buttonView;
    private String dropLat, dropLong;
    public static boolean visibility;
    private ConnectionDetector detector;
    private IntentFilter mFilter, filter;
    private Timer myTimer_publish, myTimer;
    private String msg = "", driver_mobile = "";
    private BroadcastReceiver mReceiver, receiver;
    private double currentLatitude, currentLongitude;
    private Marker markerMapOnTheWay, markerMapArrived;

    public static final String TAG = RideInfo.class.getName();
    private TextView timer;
    private PowerManager.WakeLock mWakeLock;
    private RelativeLayout relative_container;
    private Navigator mDropNavigation, mPickUpNavigation;
    private boolean isPickUp = false;
    private ProgressBar myProgress;
    private static Handler handler;

    private static MoveThread moveThread;
    private double newLongitude = 0.0, newLatitude = 0.0;
    private float start_rotation = 1.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_info);
        mHelper = new PrefsHelper(RideInfo.this);
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
//        relative_container.setVisibility(View.GONE);
        visibility = true;
        tracker = new GPSTracker(RideInfo.this);
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
                msg = intent.getStringExtra("message");
                if (status.equals("2")) {
                    mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                    mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                    mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                    mHelper.savePref("isTimerRunning", false);
                    stopService(new Intent(RideInfo.this, TimerService.class));
                    startActivity(new Intent(RideInfo.this, Invoice.class));
                    overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                    finish();
                } else if (dialog.isShowing()) {
                    dialog.dismiss();
                    getAppointmentDetails(status, mHelper.getPref(Constants.APPOINTMENT_ID, ""));
                } else {
                    getAppointmentDetails(status, mHelper.getPref(Constants.APPOINTMENT_ID, ""));
                }
            }
        };

        filter = new IntentFilter();
        filter.addAction(Constants.CANCEL_APPOINTMENT_BROADCAST);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {

                    String action = intent.getStringExtra("response");
                    String message = intent.getStringExtra("message");
                    if (action.equals("8")) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(RideInfo.this);
                        alert.setTitle(getString(R.string.message));
                        alert.setMessage(message);
                        alert.setCancelable(false);
                        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                goToLandingActivity();
                            }
                        });
                        alert.show();

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

    }

    /**
     * init method used to initialization
     */
    private void init() {
        dialog = new Dialog(RideInfo.this);
        mDialog = new ProgressDialog(RideInfo.this);
        detector = new ConnectionDetector(RideInfo.this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        time = findViewById(R.id.time);
        title = findViewById(R.id.title);
        carName = findViewById(R.id.car_name);
        distance = findViewById(R.id.distance);
        ImageView back = findViewById(R.id.back);
        bookingId = findViewById(R.id.booking_id);
        carNumber = findViewById(R.id.car_number);
        buttonView = findViewById(R.id.button_view);
        driverName = findViewById(R.id.driver_name);
        driverPic = findViewById(R.id.driver_image);
        driverRate = findViewById(R.id.driver_rating);
        carImage = findViewById(R.id.driver_car_image);
        TextView carColor = findViewById(R.id.car_color);
        Button cancelRide = findViewById(R.id.cancel_ride);
        Button chatDriver = findViewById(R.id.chat_driver);
        Button callDriver = findViewById(R.id.call_driver);
        dropOffAddress = findViewById(R.id.drop_off_address);
        RelativeLayout bottomView = findViewById(R.id.bottom_view);
        timer = findViewById(R.id.timer);
        RelativeLayout selectDropOffAddress = findViewById(R.id.show_drop_off);

        myProgress = findViewById(R.id.myProgress);
        relative_container = findViewById(R.id.relative_container);

        handler = new Handler();
        moveThread = new MoveThread();
        time.setTypeface(CommonMethods.headerFont(RideInfo.this));
        title.setTypeface(CommonMethods.headerFont(RideInfo.this));
        carName.setTypeface(CommonMethods.headerFont(RideInfo.this));
        distance.setTypeface(CommonMethods.headerFont(RideInfo.this));
        carColor.setTypeface(CommonMethods.headerFont(RideInfo.this));
        carNumber.setTypeface(CommonMethods.headerFont(RideInfo.this));
        bookingId.setTypeface(CommonMethods.headerFont(RideInfo.this));
        callDriver.setTypeface(CommonMethods.headerFont(RideInfo.this));
        chatDriver.setTypeface(CommonMethods.headerFont(RideInfo.this));
        driverName.setTypeface(CommonMethods.headerFont(RideInfo.this));
        cancelRide.setTypeface(CommonMethods.headerFont(RideInfo.this));
        dropOffAddress.setTypeface(CommonMethods.headerFont(RideInfo.this));

        back.setVisibility(View.GONE);

        back.setOnClickListener(this);
        chatDriver.setOnClickListener(this);
        callDriver.setOnClickListener(this);
        cancelRide.setOnClickListener(this);
        bottomView.setOnClickListener(this);

        selectDropOffAddress.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back) {
            finish();
        }
        if (view.getId() == R.id.call_driver) {
            if (driver_mobile.equals("")) {
                Toast.makeText(RideInfo.this, R.string.number_not_available, Toast.LENGTH_SHORT).show();
            } else {
                selectChoice(driver_mobile);
            }
        }
        if (view.getId() == R.id.cancel_ride) {
            try {
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.cancel_ride);
            } catch (Exception e) {
                e.printStackTrace();
            }
            final CancelReasonAdapter mAdapter = new CancelReasonAdapter(RideInfo.this, getResources().getStringArray(R.array.cancel_array));

            ListView cancelList = (ListView) dialog.findViewById(R.id.cancel_reason_list);
            Button submit = (Button) dialog.findViewById(R.id.submit);
            submit.setVisibility(View.VISIBLE);
            cancelList.setAdapter(mAdapter);

            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mAdapter.getSelectedPosition() == -1) {
                        Toast.makeText(RideInfo.this, R.string.cancel_reason_error, Toast.LENGTH_SHORT).show();
                    } else if (mAdapter.getSelectedPosition() == 0) {
                        dismissDialog("2");
                    } else if (mAdapter.getSelectedPosition() == 1) {
                        dismissDialog("3");
                    } else if (mAdapter.getSelectedPosition() == 2) {
                        dismissDialog("4");
                    } else if (mAdapter.getSelectedPosition() == 3) {
                        dismissDialog("5");
                    } else if (mAdapter.getSelectedPosition() == 4) {
                        dismissDialog("6");
                    }
                }
            });
            dialog.show();
        }
        if (view.getId() == R.id.show_drop_off) {
            Toast.makeText(RideInfo.this, getString(R.string.come_soon), Toast.LENGTH_SHORT).show();
        }
        if (view.getId() == R.id.bottom_view) {
            view.getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (tracker.canGetLocation()) {
            if (tracker.getLatitude() == 0.0 && tracker.getLongitude() == 0.0) {
                Toast.makeText(RideInfo.this, R.string.no_location_view, Toast.LENGTH_SHORT).show();
            } else {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                googleMap.setIndoorEnabled(false);
                LatLng latLng = new LatLng(currentLatitude, currentLongitude);


                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }

                try {
                    boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(RideInfo.this, R.raw.style_json));
                    if (!success) {
                        Log.e(TAG, "Style parsing failed.");
                    }
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }

                googleMap.setMyLocationEnabled(false);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP));
//                markerMapArrived = googleMap.addMarker(new MarkerOptions().flat(true).position(latLng).title("First Point").icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red)));
            }
        } else {
            tracker.showSettingsAlert();
        }
    }

    /**
     * method used to get appointment details
     *
     * @param rideStatus    contain status
     * @param appointmentId contain appointment id
     */
    private void getAppointmentDetails(final String rideStatus, final String appointmentId) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GET_APPOINTMENT, new Response.Listener<String>() {
                @SuppressLint("SetTextI18n")
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
                            String driver_name = appObject.getString("driver_name");
                            driver_mobile = appObject.getString("driver_mobile");
                            String driver_pic = appObject.getString("driver_pic");
                            String drop_address = appObject.getString("drop_address");
                            String pick_address = appObject.getString("pick_address");
                            String vehicle_reg_no = appObject.getString("vehicle_reg_no");
                            String vehicle_make_title = appObject.getString("vehicle_make_title");
                            String driverCarImage = appObject.getString("taxi_front_view");
                            String vehicle_model_title = appObject.getString("vehicle_model_title");
                            String pickLat = appObject.getString("pick_latitude");
                            String pickLong = appObject.getString("pick_longitude");
                            dropLat = appObject.getString("drop_latitude");
                            dropLong = appObject.getString("drop_longitude");
                            String walletBalance = appObject.getString("wallet_balance");
                            String driver_rating = appObject.getString("driver_rating");

                            driverName.setText(driver_name);
                            carNumber.setText(vehicle_reg_no);
                            carName.setText(vehicle_make_title + " - " + vehicle_model_title);
                            if (!driver_rating.equals("") && !driver_rating.equals("null")) {
                                driverRate.setRating(Float.valueOf(driver_rating));
                            } else {
                                driverRate.setRating(0.0f);
                            }

                            mHelper.savePref("pick_lat", pickLat);
                            mHelper.savePref("pick_long", pickLong);
                            mHelper.savePref("drop_lat", dropLat);
                            mHelper.savePref("drop_long", dropLong);
                            mHelper.savePref("driver_image", driver_pic);
                            mHelper.savePref("app_status", appStatus);
                            mHelper.savePref(Constants.WALLET_BALANCE, walletBalance);

                            if (!driver_pic.equals("")) {
                                Glide.with(RideInfo.this).load(Constants.IMAGE_BASE_URL_DRIVER + driver_pic).asBitmap().placeholder(R.drawable.default_user_pic)
                                        .centerCrop().into(new BitmapImageViewTarget(driverPic) {
                                    @Override
                                    protected void setResource(Bitmap resource) {
                                        RoundedBitmapDrawable circularBitmapDrawable =
                                                RoundedBitmapDrawableFactory.create(RideInfo.this.getResources(), resource);
                                        circularBitmapDrawable.setCircular(true);
                                        driverPic.setImageDrawable(circularBitmapDrawable);
                                    }
                                });
                            }

                            if (!CommonMethods.isServiceRunning(TimerService.class, RideInfo.this)) {
                                if (Integer.parseInt(appStatus) > 3) {
                                    startService(new Intent(RideInfo.this, TimerService.class));
                                }
                            }

                            if (!mHelper.getPref(Constants.TIMER_VALUE, "0").equals("00")) {
                                if (Integer.parseInt(appStatus) > 3) {
                                    myProgress.setProgress(0);
                                    timer.setText("00");
                                    mHelper.savePref(Constants.TIMER_VALUE, "");
                                }
                            }

                            if (!driverCarImage.equals("")) {
                                Glide.with(RideInfo.this).load(Constants.DRIVER_CAR_IMAGE_URL + driverCarImage).asBitmap().placeholder(R.drawable.ic_car)
                                        .centerCrop().into(new BitmapImageViewTarget(carImage) {
                                    @Override
                                    protected void setResource(Bitmap resource) {
                                        RoundedBitmapDrawable circularBitmapDrawable =
                                                RoundedBitmapDrawableFactory.create(RideInfo.this.getResources(), resource);
                                        circularBitmapDrawable.setCircular(true);
                                        carImage.setImageDrawable(circularBitmapDrawable);
                                    }
                                });
                            }
                            switch (appStatus) {
                                case "8":
                                    AlertDialog.Builder alert = new AlertDialog.Builder(RideInfo.this);
                                    alert.setTitle(getString(R.string.message));
                                    alert.setMessage(message);
                                    alert.setCancelable(false);
                                    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            goToLandingActivity();
                                        }
                                    });
                                    alert.show();
                                    break;
                                case "5":
                                    title.setText(R.string.driver_onthe_way);
                                    mHelper.savePref(Constants.DRIVER_ON_THE_WAY, true);
                                    mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                                    mHelper.savePref(Constants.BEGIN_JOURNEY, false);
                                    dropOffAddress.setText(pick_address);

                                    /*if (!mHelper.getPref("driver_lat", "").equals("") && !mHelper.getPref("driver_long", "").equals("")) {
                                        LatLng latLng = new LatLng(Double.parseDouble(mHelper.getPref("driver_lat", "")), Double.parseDouble(mHelper.getPref("driver_long", "")));
                                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP));
                                        markerMapOnTheWay = mGoogleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red))
                                                .rotation(0).anchor(0.5f, 0.5f).flat(true));

                                        String url = getMapsApiDirectionsUrl(mHelper.getPref("driver_lat", ""), mHelper.getPref("driver_long", ""));
                                        ReadTask downloadTask = new ReadTask();
                                        downloadTask.execute(url);
                                    } else {
                                        Log.d(TAG, "blank lat long");
                                    }*/
                                    break;
                                case "4":
                                    title.setText(R.string.driver_arrived);
                                    mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                                    mHelper.savePref(Constants.DRIVER_ARRIVED, true);
                                    mHelper.savePref(Constants.BEGIN_JOURNEY, false);
                                    dropOffAddress.setText(drop_address);
                                    isPickUp = true;
                                    goForPickup();

                                    break;
                                case "3":
                                    title.setText(R.string.journey_started);
                                    buttonView.setVisibility(View.GONE);
                                    mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                                    mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                                    mHelper.savePref(Constants.BEGIN_JOURNEY, true);

                                    isPickUp = false;
                                    relative_container.setVisibility(View.GONE);
                                    stopService(new Intent(RideInfo.this, TimerService.class));
                                    startJourney();

                                    break;
                                case "2":
                                case "1":
                                    mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                                    mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                                    mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                                    stopDropNavigation();
                                    mHelper.savePref("isTimerRunning", false);
                                    mHelper.savePref("app_status", appStatus);
                                    stopService(new Intent(RideInfo.this, TimerService.class));
                                    startActivity(new Intent(RideInfo.this, Invoice.class));
                                    overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                                    finish();
                                    break;
                            }
                        } else if (key.equals("1")) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(RideInfo.this);
                            alert.setTitle(getString(R.string.message));
                            alert.setMessage(message);
                            alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mHelper.clearAllPref();
                                    Intent intent = new Intent(RideInfo.this, Splash.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                    overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                                }
                            });
                            alert.show();
                        } else {
                            CommonMethods.showAlert(RideInfo.this, getString(R.string.message), message);
                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(RideInfo.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener()

            {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(RideInfo.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            })

            {
                @Override
                protected Map<String, String> getParams() {
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
            Volley.newRequestQueue(RideInfo.this).

                    add(mRequest);
        } else {
            CommonMethods.showAlert(RideInfo.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }

    }

    /**
     * method used to go for pickup point
     */
    private void goForPickup() {
        mGoogleMap.clear();
        LatLng latLng = new LatLng(Double.parseDouble(mHelper.getPref("driver_lat", "0.0")),
                Double.parseDouble(mHelper.getPref("driver_long", "0.0")));

        LatLng mPickupLat = new LatLng(Double.parseDouble(mHelper.getPref("pick_lat", "0.0")),
                Double.parseDouble(mHelper.getPref("pick_long", "0.0")));

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP));
        markerMapArrived = mGoogleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red)));

        startPickupNavigation(markerMapArrived, mPickupLat, latLng);

    }

    /**
     * method used to start pickup point navigation
     *
     * @param markerMapOnTheWay contain marker
     * @param pickup            contain pickup location
     * @param destination       contain destination location
     */
    private void startPickupNavigation(Marker markerMapOnTheWay, LatLng pickup, LatLng destination) {
        mPickUpNavigation = new Navigator(this, mGoogleMap, true);
        mPickUpNavigation.startNavigation(markerMapOnTheWay, destination, pickup);
    }

    /**
     * method used to stop pickup navigation
     */
    private void stopPickupNavigation() {
        if (isPickUp) {
            relative_container.setVisibility(View.GONE);
            mPickUpNavigation.stopNavigation();
        }
    }

    /**
     * method used to start journey
     */
    private void startJourney() {
        mGoogleMap.clear();
        stopPickupNavigation();
        LatLng mDriverLat = new LatLng(Double.parseDouble(mHelper.getPref("driver_lat", "0.0")),
                Double.parseDouble(mHelper.getPref("driver_long", "0.0")));

        LatLng mPickupLat = new LatLng(Double.parseDouble(dropLat), Double.parseDouble(dropLong));

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mDriverLat, Constants.ZOOM_MAP));
        markerMapArrived = mGoogleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(mDriverLat).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red)));

        startDropNavigation(markerMapArrived, mDriverLat, mPickupLat);

    }

    /**
     * method used to start drop navigation
     *
     * @param markerMapArrived contain marker
     * @param mPickupLat       contain pickup lat
     * @param mDriverLat       contain pickup location
     */
    private void startDropNavigation(Marker markerMapArrived, LatLng mPickupLat, LatLng mDriverLat) {
        mDropNavigation = new Navigator(this, mGoogleMap, false);
        mDropNavigation.startNavigation(markerMapArrived, mPickupLat, mDriverLat);
    }

    /**
     * method used to stop pickup navigation
     */
    private void stopDropNavigation() {
        try {
            newLatitude = 0.0;
            newLongitude = 0.0;

            mDropNavigation.stopNavigation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * method used to initialise marker
     *
     * @param latLng contain lat long points
     */
    private void initialiseMyMarker(Marker marker, LatLng latLng) {
        if (marker != null) {
            marker.remove();
        }
        marker = mGoogleMap.addMarker(new MarkerOptions().flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red))
                .anchor(0.5f, 0.5f).position(latLng));
    }

    @Override
    public void onResume() {
        super.onResume();
        visibility = true;
        if (mReceiver != null) {
            registerReceiver(mReceiver, mFilter);
        }
        if (receiver != null) {
            registerReceiver(receiver, filter);
        }
        registerReceiver(countReceiver, new IntentFilter(TimerService.COUNTDOWN_BR));
        new BackgroundSubscribeMyChannel().execute();

        getAppointmentDetails(mHelper.getPref(Constants.APPOINTMENT_STATUS, ""), mHelper.getPref(Constants.APPOINTMENT_ID, ""));


    }

    @Override
    public void onPause() {
        super.onPause();
        visibility = false;
        unregisterReceiver(mReceiver);
        unregisterReceiver(receiver);
        unregisterReceiver(countReceiver);

        if (myTimer_publish != null) {
            myTimer_publish.cancel();
            myTimer_publish = null;
        }

        if (myTimer != null) {
            myTimer.cancel();
            myTimer = null;
        }

        new BackgroundUnSubscribeAll().execute();

    }

    /**
     * method used to get visibility status of activity
     *
     * @return true or false
     */
    public static boolean visibilityStatus() {
        return visibility;
    }

    @Override
    protected void onDestroy() {
        this.mWakeLock.release();
        super.onDestroy();
        visibility = false;
        new BackgroundUnSubscribeAll().execute();
    }

    /**
     * method used to call user
     *
     * @param phoneNo contain phone number
     */
    private void selectChoice(final String phoneNo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(phoneNo);

        builder.setPositiveButton("CALL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNo));
                if (ActivityCompat.checkSelfPermission(RideInfo.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivity(dialIntent);
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.setCancelable(false);
        alert.show();
    }

    /**
     * Async Task used to draw path on map
     */
    @SuppressLint("StaticFieldLeak")
    private class CallGooglePlayServices extends AsyncTask<String, Void, GooglePlacesResultData> {
        @Override
        protected GooglePlacesResultData doInBackground(String... params) {


            String url = null;
            if (mHelper.getPref(Constants.DRIVER_ON_THE_WAY, false) || mHelper.getPref(Constants.DRIVER_ARRIVED, false)) {
                url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + mHelper.getPref("driver_lat", "") + ","
                        + mHelper.getPref("driver_long", "") + "&destination=" + mHelper.getPref("pick_lat", "")
                        + "," + mHelper.getPref("pick_long", "") + "&key=" + Constants.MAPS_DIRECTION;

                System.out.println("google url=" + url);
            } else if (mHelper.getPref(Constants.BEGIN_JOURNEY, false)) {
                url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + mHelper.getPref("driver_lat", "") + ","
                        + mHelper.getPref("driver_long", "") + "&destination=" + mHelper.getPref("drop_lat", "") + ","
                        + mHelper.getPref("drop_long", "") + "&key=" + Constants.MAPS_DIRECTION;
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
                Gson gson = new Gson();
                parseData = gson.fromJson(data, GooglePlacesResultData.class);
            }
            return parseData;
        }

        @Override
        protected void onPostExecute(GooglePlacesResultData result) {

            super.onPostExecute(result);

            if (result != null) {
                if (result.getRoutes() != null && result.getRoutes().size() > 0) {

                    distance.setText(String.format("%s - %s", getResources().getString(R.string.dist), result.getRoutes().get(0).getLegs().get(0).getDistance().getText()));
                    time.setText(String.format("%s - %s", getResources().getString(R.string.eta), result.getRoutes().get(0).getLegs().get(0).getDuration().getText()));
                }
            }
        }
    }

    /**
     * To get the directions url between pickup location and driver current
     * location
     */
    private String getMapsApiDirectionsUrl(String driver_lat, String driver_long) {

        LatLng latLng = new LatLng(Double.parseDouble(mHelper.getPref("pick_lat", "")),
                Double.parseDouble(mHelper.getPref("pick_long", "")));
        mGoogleMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng)
                // .title("Booking Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.home_markers_pickup)));

        LatLng latLng1 = new LatLng(Double.parseDouble(mHelper.getPref("pick_lat", "")),
                Double.parseDouble(mHelper.getPref("pick_long", "")));
        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng1, Constants.ZOOM_MAP));

        String waypoints = "origin=" + mHelper.getPref("pick_lat", "") + "," + mHelper.getPref("pick_long", "") + "&" + "destination="
                + driver_lat + "," + driver_long;

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor;
        String output = "json";
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + params;
    }

    /**
     * method used to read the url go for parse it
     */
    @SuppressLint("StaticFieldLeak")
    private class ReadTask extends AsyncTask<String, Void, String> {
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

    /**
     * plotting the directions in google map
     */
    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
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
                polyLineOptions.geodesic(true);
            }
            if (polyLineOptions != null)
                mGoogleMap.addPolyline(polyLineOptions);
        }
    }

    /**
     * method used to cancel request
     *
     * @param cancelStatus contain cancel reason
     */
    private void CancelRequest(final String cancelStatus) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.CANCEL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        String invalidResponse = mObject.getString("response_invalid");
                        if (status == 1) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(RideInfo.this);
                            alert.setTitle(getString(R.string.message));
                            alert.setMessage(message);
                            alert.setCancelable(false);
                            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    goToLandingActivity();
                                }
                            });
                            alert.show();
                        } else if (invalidResponse.equals("1")) {
                            showAlert(RideInfo.this, getString(R.string.message), message);
                        } else {
                            CommonMethods.showAlert(RideInfo.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(RideInfo.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(RideInfo.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("app_appointment_id", mHelper.getPref(Constants.APPOINTMENT_ID, ""));
                    mParams.put("cancel_status", cancelStatus);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("request_date", CommonMethods.getDateAndTime());
                    mParams.put("user_timezone", TimeZone.getDefault().getID());

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            Volley.newRequestQueue(RideInfo.this).add(mRequest);
        } else {
            CommonMethods.showAlert(RideInfo.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to dismiss the dialog
     *
     * @param cancelStatus contain cancel status
     */
    public void dismissDialog(String cancelStatus) {
        dialog.dismiss();
        CancelRequest(cancelStatus);
    }

    /**
     * phubnub subscribe method
     *
     * @param channels
     */
    public void pubNubSubscribe(String[] channels) {
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
                                        CommonMethods.showAlert(RideInfo.this, getString(R.string.attention), getString(R.string.something_wrong));
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

    /**
     * method used to get driver details from pubnub
     *
     * @param message contain pubnub message
     */
    private void getDriverDetails(String message) {
        try {
            JSONObject mObject = new JSONObject(message);
            String driverLat = mObject.getString("lt");
            String driverLong = mObject.getString("lg");
            String bearing = mObject.getString("bearing");
            String status = mObject.getString("st");
            String appointment_id = mObject.getString("appointment_id");

            mHelper.savePref("driver_lat", driverLat);
            mHelper.savePref("driver_long", driverLong);
            if (appointment_id.equals(mHelper.getPref(Constants.APPOINTMENT_ID, ""))) {
                switch (status) {
                    case "5":
                        UpdateDriverLocation_DriverOnTheWay(driverLat, driverLong, bearing);
                        break;
                    case "4":
                        UpdateDriverLocation_DriverArrived(driverLat, driverLong, bearing);
//                        timer.setVisibility(View.GONE);
//                        relative_container.setVisibility(View.GONE);
                        break;
                    case "3":
                        UpdateDriverLocation_JourneyStarted(driverLat, driverLong, bearing);
                        timer.setVisibility(View.GONE);
                        relative_container.setVisibility(View.GONE);
                        break;
                }
            } else {
                Log.d(TAG, "appointment id not match.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }

    /**
     * class used to subscribe pubNub channel
     */
    @SuppressLint("StaticFieldLeak")
    private class BackgroundSubscribeMyChannel extends AsyncTask<String, Void, String> {
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
//            createGeofences(28.5355,77.3910);
            Log.d(TAG, "onPostExecute:");
        }
    }

    /**
     * unsubscribing the all channels
     */
    @SuppressLint("StaticFieldLeak")
    private class BackgroundUnSubscribeAll extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            pubnub.unsubscribeAll();
            return null;
        }
    }

    /**
     * method used to goTo landing activity
     */
    void goToLandingActivity() {
        mHelper.savePref("isTimerRunning", false);
        mHelper.savePref(Constants.APPOINTMENT_ID, "0");
        mHelper.savePref(Constants.TIMER_VALUE, "");
        stopService(new Intent(RideInfo.this, TimerService.class));
        Intent intent = new Intent(RideInfo.this, LandingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

    }

    /**
     * method used to show alert dialog
     *
     * @param string get alert message
     */
    private void showAlert(Context context, String title, String string) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title);
        alert.setMessage(string);
        alert.setCancelable(false);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHelper.clearAllPref();
                Intent intent = new Intent(RideInfo.this, Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }

    /**
     * Updating driver location when Driver On The Way
     */
    private void UpdateDriverLocation_DriverOnTheWay(String driverLt, String driverLg, String bearing) {
        try {
            if (driverLt.equals("") || driverLg.equals("")) {
                Log.d(TAG, "Some Error");
            } else {
                mHelper.savePref(Constants.DRIVER_ON_THE_WAY, true);
                mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                double driver_current_latitude = Double.parseDouble(driverLt);
                double driver_cuttent_longitude = Double.parseDouble(driverLg);

                float driver_cuttent_bearing = 0.0f;
                if (!bearing.equals("")) {
                    driver_cuttent_bearing = Float.parseFloat(bearing);
                }

                System.out.println("markerMapOnTheWay inside channel contains LAT:" + driverLt);
                System.out.println("markerMapOnTheWay inside channel contains LON:" + driverLg);

                mHelper.savePref("driver_lat", String.valueOf(driver_current_latitude));
                mHelper.savePref("driver_long", String.valueOf(driver_cuttent_longitude));
                mHelper.savePref("bearing", String.valueOf(driver_cuttent_bearing));

                System.out.println("INSIDE DRIVER ON THE WAY:4 lat=" + driver_current_latitude + "  lng=" + driver_cuttent_longitude);

                new CallGooglePlayServices().execute();

                try {
                    LatLng latLng = new LatLng(driver_current_latitude, driver_cuttent_longitude);

                    // Get the current location
                    Location driverLocation = new Location("starting_point");
                    driverLocation.setLatitude(driver_current_latitude);
                    driverLocation.setLongitude(driver_cuttent_longitude);

                    CameraPosition currentPlace = new CameraPosition.Builder(mGoogleMap.getCameraPosition()).target(latLng)
                            .bearing(driver_cuttent_bearing).tilt(Constants.TILT).zoom(15).build();
                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

//            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP));
//            markerMapOnTheWay.setRotation(rotation_angle);
                    /*if (markerMapOnTheWay != null) {
                        markerMapOnTheWay.setPosition(latLng);
                    } else {
                        mGoogleMap.clear();
                        markerMapOnTheWay = mGoogleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.home_caricon_red)).anchor(0.5f, 0.5f).rotation(0).flat(false));

                        String url = getMapsApiDirectionsUrl(mHelper.getPref("driver_lat", ""), mHelper.getPref("driver_long", ""));
                        ReadTask downloadTask = new ReadTask();
                        downloadTask.execute(url);
                    }*/
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, e.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updating driver location when Driver Arrived
     */
    private void UpdateDriverLocation_DriverArrived(String driverLat, String driverLong, String bearing) {
        try {
            if (driverLat.equals("") || driverLong.equals("")) {
                Log.d(TAG, "Some Error");
            } else {
                mHelper.savePref(Constants.DRIVER_ARRIVED, true);
                mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                float driver_cuttent_bearing = 0.0f;
                if (!bearing.equals("")) {
                    driver_cuttent_bearing = Float.parseFloat(bearing);
                }
                double driver_current_latitude = Double.parseDouble(driverLat);
                double driver_cuttent_longitude = Double.parseDouble(driverLong);

                mHelper.savePref("bearing", String.valueOf(driver_cuttent_bearing));
                mHelper.savePref("driver_lat", String.valueOf(driver_current_latitude));
                mHelper.savePref("driver_long", String.valueOf(driver_cuttent_longitude));

                System.out.println("INSIDE DRIVER REACHED:4 current lat=" + driver_current_latitude + " lng=" + driver_cuttent_longitude);

                new CallGooglePlayServices().execute();

                CameraPosition currentPlace = new CameraPosition.Builder(mGoogleMap.getCameraPosition())
                        .target(new LatLng(driver_current_latitude, driver_cuttent_longitude))
                        .bearing(driver_cuttent_bearing).build();
                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace), 1000, null);

                try {
                    if (markerMapArrived == null) {
                        initialiseMyMarker(markerMapArrived, new LatLng(driver_current_latitude, driver_cuttent_longitude));
                        goForPickup();

                    } else {
                        markerMapArrived.setPosition(new LatLng(driver_current_latitude, driver_cuttent_longitude));
                    }

                    markerMapArrived.setRotation(driver_cuttent_bearing);
                    float zoom = mGoogleMap.getCameraPosition().zoom;

                    if (zoom < 12.0f) {
                        zoom = 16.0f;
                    }
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(driver_current_latitude, driver_cuttent_longitude), zoom), 1000, null);

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, e.toString());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updating driver location when Driver Journey Started
     */
    private void UpdateDriverLocation_JourneyStarted(String driverLat, String driverLong, String bearing) {
        try {
            if (driverLat.equals("") || driverLong.equals("")) {
                Log.d(TAG, "Some error");
            } else {
                double driver_current_latitude = Double.parseDouble(driverLat);
                double driver_cuttent_longitude = Double.parseDouble(driverLong);
                float driver_cuttent_bearing = 0.0f;

                if (!bearing.equals("")) {
                    driver_cuttent_bearing = Float.parseFloat(bearing);
                }

                mHelper.savePref("bearing", String.valueOf(driver_cuttent_bearing));
                mHelper.savePref("driver_lat", String.valueOf(driver_current_latitude));
                mHelper.savePref("driver_long", String.valueOf(driver_cuttent_longitude));

                new CallGooglePlayServices().execute();

                /*float zoom = mGoogleMap.getCameraPosition().zoom;

                if (zoom < 12.0f) {
                    zoom = 16.0f;
                }

                CameraPosition currentPlace = new CameraPosition.Builder(mGoogleMap.getCameraPosition()).target(new LatLng(driver_current_latitude, driver_cuttent_longitude))
                        .bearing(driver_cuttent_bearing).zoom(zoom).build();
                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace), 1000, null);

                try {
                    if (markerMapArrived == null) {
                        initialiseMyMarker(markerMapArrived, new LatLng(driver_current_latitude, driver_cuttent_longitude));
                        startJourney();
                    } else {
                        markerMapArrived.setRotation(driver_cuttent_bearing);
                        markerMapArrived.setPosition(new LatLng(driver_current_latitude, driver_cuttent_longitude));
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                    startJourney();
                    Log.d(TAG, e.toString());
                }*/

                if (newLatitude == 0.0) {
                    newLatitude = driver_current_latitude;
                    newLongitude = driver_cuttent_longitude;

                    updateMarkerOnMap(driver_current_latitude, driver_cuttent_longitude, driver_cuttent_bearing);
                } else {
                    updateMarkerOnMap(driver_current_latitude, driver_cuttent_longitude, driver_cuttent_bearing);

                }

                newLatitude = driver_current_latitude;
                newLongitude = driver_cuttent_longitude;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * create a broadcast receiver
     */
    private BroadcastReceiver countReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent);
        }
    };

    @SuppressLint("SetTextI18n")
    private void updateGUI(Intent intent) {
        if (intent.getIntExtra("finish", 0) != 1) {
            mHelper.savePref("isTimerRunning", true);
            timer.setText(intent.getStringExtra("countdown"));
            timer.setVisibility(View.VISIBLE);
            relative_container.setVisibility(View.VISIBLE);
        } else {
            stopService(new Intent(RideInfo.this, TimerService.class));
            myProgress.setProgress(0);
            mHelper.savePref(Constants.TIMER_VALUE, "");
            relative_container.setVisibility(View.VISIBLE);
            timer.setText("00");
        }
    }

    /**
     * method used to update marker on map
     *
     * @param driver_current_latitude  contain driver latitude
     * @param driver_current_longitude contain driver longitude
     * @param driver_current_bearing   contain bearing of driver lat long
     */
    private void updateMarkerOnMap(double driver_current_latitude, double driver_current_longitude, float driver_current_bearing) {
        Location mSLocation = new Location("source");
        mSLocation.setLatitude(newLatitude);
        mSLocation.setLongitude(newLongitude);

        Location mLocation = new Location("destination");
        mLocation.setLatitude(driver_current_latitude);
        mLocation.setLongitude(driver_current_longitude);

        rotateMarker(markerMapArrived, (float) bearingBetweenLocations(new LatLng(newLatitude, newLongitude),
                new LatLng(driver_current_latitude, driver_current_longitude)));
        moveThread.setNewPoint(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        animateMarkerToICS(markerMapArrived, new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));

    }

    static void animateMarkerToICS(Marker marker, LatLng finalPosition) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);
        animator.setDuration(3000);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                handler.post(moveThread);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        animator.start();
    }

    public static LatLng interpolate(float fraction, LatLng a, LatLng b) {
        // function to calculate the in between values of old latlng and new latlng.
        // To get more accurate tracking(Car will always be in the road even when the latlng falls away from road), use roads api from Google apis.
        // As it has quota limits I didn't have used that method.
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lngDelta = b.longitude - a.longitude;

        // Take the shortest path across the 180th meridian.
        if (Math.abs(lngDelta) > 180) {
            lngDelta -= Math.signum(lngDelta) * 360;
        }
        double lng = lngDelta * fraction + a.longitude;
        return new LatLng(lat, lng);
    }

    private class MoveThread implements Runnable {
        LatLng newPoint;
        float zoom = 16;

        void setNewPoint(LatLng latLng) {
            this.newPoint = latLng;
        }

        @Override
        public void run() {
            final CameraUpdate point = CameraUpdateFactory.newLatLngZoom(newPoint, zoom);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mGoogleMap.animateCamera(point);
                }
            });
        }
    }

    public void moveVehicle(final Marker myMarker, final Location finalPosition) {

        final LatLng startPosition = myMarker.getPosition();

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 3000;
        final boolean hideMarker = false;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + (finalPosition.getLatitude()) * t,
                        startPosition.longitude * (1 - t) + (finalPosition.getLongitude()) * t);

                animateMarkerToICS(markerMapArrived, currentPosition);

//                myMarker.setPosition(currentPosition);

                // Repeat till progress is completeelse
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                    // handler.postDelayed(this, 100);
                } else {
                    if (hideMarker) {
                        myMarker.setVisible(false);
                    } else {
                        myMarker.setVisible(true);
                    }
                }
            }
        });
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = marker.getRotation();
        final long duration = 200;
        final float angle = toRotation % 360;

        final Interpolator interpolator = new LinearInterpolator();


        handler.post(new Runnable() {
            @Override
            public void run() {

                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                //float rot = t * angle + (1 - t) * startRotation;

                float factor = (angle - startRotation);

                if (abs(factor) > 270)
                    factor = angle < startRotation ? angle : startRotation;

                float rot = t * factor + startRotation;

                float bearing = -rot > 180 ? rot / 2 : rot;

                if (bearing > 0 && bearing < 360)
                    marker.setRotation(bearing);

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
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


}

