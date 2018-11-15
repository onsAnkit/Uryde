package uryde.passenger.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.gson.Gson;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import uryde.passenger.GeofenceTransitionsIntentService;
import uryde.passenger.R;
import uryde.passenger.RideInfo;
import uryde.passenger.SendRequest;
import uryde.passenger.adapter.CarPagerAdapter;
import uryde.passenger.adapter.CarsDetailAdapter;
import uryde.passenger.cardPayment.ChangeCard;
import uryde.passenger.model.CarDetail;
import uryde.passenger.model.GeocodingResponse;
import uryde.passenger.rideLater.RideLaterActivity;
import uryde.passenger.searching.SearchAddressGooglePlacesActivity;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.GPSTracker;
import uryde.passenger.util.PookieEventBus;
import uryde.passenger.util.PrefsHelper;

public class BookYourRide extends Fragment implements OnMapReadyCallback, View.OnClickListener {

    public static final String TAG = BookYourRide.class.getName();
    private static boolean visibility = false;
    public GoogleMap mGoogleMap;
    JSONArray mArray;
    private Pubnub pubnub;
    private int paymentType = 0;
    private GPSTracker tracker;
    private int bookingType = 0;
    private PrefsHelper mHelper;
    private RecyclerView carsList;
    private ProgressDialog mDialog;
    private LinearLayout notesLayout;
    private List<CarDetail> mCarsList;
    private CarsDetailAdapter mAdapter;
    private ConnectionDetector detector;
    private IntentFilter mMessageFilter;
    private ImageView pinDrop, driverCar;
    private Button fareEstimate, addNotes;
    private BroadcastReceiver mMessageReceiver;
    private double currentLongitude, currentLatitude;
    private boolean goForDropOff = false, goForPickUp = false, isSetDropoffLocation;
    private TextView emptyView, pickupLocationAddress, dropoffLocationAddress, driverFound, driverDistance;
    private String selectedCarImage, baseFareType = "", typeCityID = "", typeID = "", mPICKUP_ADDRESS = "", mDROPOFF_ADDRESS = "", to_longitude = "0.0", to_latitude = "0.0",
            from_longitude = "0.0", from_latitude = "0.0", formattedAddress = "", locationName = "", notesForDriver = "", driverDis = "", promoCodeId = "0";
    private TimerTask myTimerTask;
    private Timer myTimer;
    private PendingIntent mGeofencePendingIntent;
    private ArrayList mGeofenceList = new ArrayList();
    private Geofence fence;
    private ViewPager carpager;

    @SuppressLint("HardwareIds")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new PrefsHelper(getActivity());
        tracker = new GPSTracker(getActivity());
        if (tracker.canGetLocation()) {
            currentLatitude = tracker.getLatitude();
            currentLongitude = tracker.getLongitude();
            Log.d(TAG, currentLatitude + " -- " + currentLongitude);
        } else {
            tracker.showSettingsAlert();
        }

        mMessageFilter = new IntentFilter();
        mMessageFilter.addAction(Constants.LOCATION_UPDATE);

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                currentLatitude = intent.getDoubleExtra("latitude", 0.0);
                currentLongitude = intent.getDoubleExtra("longitude", 0.0);
                //startPublishingWithTimer(typeCityID, typeID, baseFareType, selectedCarImage);
            }
        };

        pubnub = new Pubnub(Constants.PUBNUB_PUBLISH_KEY, Constants.PUBNUB_SUBSCRIBE_KEY, "", true);
        pubnub.setUUID("PASS_URYDE" + Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID));

    }

    /**
     * pubnub publish method
     */
    public void pubNubPublish(final String channel, JSONObject message) {

        Callback callback = new Callback() {
            public void successCallback(String channel, Object response) {
            }

            public void errorCallback(String channel, PubnubError error) {
                System.out.println(error.toString());
            }
        };
        pubnub.publish(channel, message, callback);
    }

    /**
     * phubnub subscribe method
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
                            String type = "";
                            if (getActivity() == null)
                                return;

                            Log.d(TAG, message.toString());
                            try {
                                JSONObject messageObject = new JSONObject(message.toString());
                                final String rqTp = messageObject.getString("rq_tp");
                                if (rqTp.equals("3")) {
                                    if (messageObject.has("type")) {
                                        type = messageObject.getString("type");
                                    }
                                    final String msg = messageObject.getString("msg");
                                    final String dri = messageObject.getString("dri");
                                    if (messageObject.has("type")) {
                                        if (!type.equals(mHelper.getPref(Constants.VEHICLE_TYPE, ""))) {
                                            mHelper.savePref(Constants.VEHICLE_TYPE, type);
                                            Handler refresh = new Handler(Looper.getMainLooper());
                                            refresh.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    getCarDetailsAndSet(mHelper.getPref(Constants.VEHICLE_TYPE, ""), mHelper.getPref(Constants.VEHICLE_MESSAGE, ""));

                                                }
                                            });
                                        } else {
                                            Log.d(TAG, "Same data");
                                        }
                                    }
                                    if (messageObject.has("dri")) {
                                        if (!dri.equals(mHelper.getPref(Constants.VEHICLE_DRI, ""))) {
                                            mHelper.savePref(Constants.VEHICLE_DRI, dri);
                                            Handler driverRefresh = new Handler(Looper.getMainLooper());
                                            driverRefresh.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mGoogleMap.clear();
                                                    getDriverDetails(mHelper.getPref(Constants.VEHICLE_DRI, ""), rqTp);
                                                }
                                            });
                                        } else {
                                            Log.d(TAG, "Same data");
                                        }
                                    }
                                    mHelper.savePref(Constants.VEHICLE_MESSAGE, msg);

                                } else if (rqTp.equals("5")) {
                                    if (RideInfo.visibilityStatus()) {
                                        getDriverDetails(message.toString(), rqTp);
                                        PookieEventBus.getInstance().publish("driver_details", message);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Handler refresh = new Handler(Looper.getMainLooper());
                                Log.d(TAG, e.toString());
                                refresh.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home, container, false);
        visibility = true;
        init(view);

        return view;
    }

    private String categoryId="0";
    /**
     * method used to initialization
     *
     * @param view contain view
     */
    private void init(View view) {
        mCarsList = new ArrayList<>();
        mDialog = new ProgressDialog(getActivity());
        detector = new ConnectionDetector(getActivity());
        mAdapter = new CarsDetailAdapter(getActivity(), mCarsList, BookYourRide.this, 0);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        final LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);

        mDialog.setCancelable(false);
        pinDrop = view.findViewById(R.id.pin_drop);
        carsList = view.findViewById(R.id.cars_list);
        addNotes = view.findViewById(R.id.add_notes);
        emptyView = view.findViewById(R.id.empty_view);
        driverCar = view.findViewById(R.id.driver_car);
        Button rideNow = view.findViewById(R.id.ride_now);
        driverFound = view.findViewById(R.id.driver_found);
        notesLayout = view.findViewById(R.id.notes_layout);
        fareEstimate = view.findViewById(R.id.fare_estimate);
        Button rideLater = view.findViewById(R.id.ride_later);
        driverDistance = view.findViewById(R.id.driver_distance);
        RelativeLayout addAddress = view.findViewById(R.id.show_drop_off);
        dropoffLocationAddress = view.findViewById(R.id.drop_off_address);
        pickupLocationAddress = view.findViewById(R.id.show_addr_text_view);
        ImageView goToDropPosition = view.findViewById(R.id.go_to_current_drop_position);
        ImageView goToPickupPosition = view.findViewById(R.id.go_to_current_pickup_position);
        carpager = view.findViewById(R.id.car_pager);

        carpager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                View view = (View) carpager.findViewWithTag("myview" + carpager.getCurrentItem());
                RecyclerView recyclerView = view.findViewById(R.id.cars_list);
                typeCityID="";
                CarsDetailAdapter.selected_position = 0;
                try {
                    categoryId=mArray.getJSONObject(position).optString("vehiclecategory_id");
                    JSONArray subarry = mArray.getJSONObject(position).getJSONArray("sub_vechicle");
                    JSONObject mObject = subarry.getJSONObject(0);
                    startPublishingWithTimer(
                            typeCityID,
                            mObject.getString("vehicle_type_id"),
                            mObject.getString("base_fare_type"),
                            mObject.getString("base_fare_type"),categoryId);
                } catch (Exception e) {
                }
                recyclerView.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        carsList.setLayoutManager(manager);
        carsList.setItemAnimator(new DefaultItemAnimator());
        carsList.setAdapter(mAdapter);

        rideNow.setTypeface(CommonMethods.headerFont(getActivity()));
        addNotes.setTypeface(CommonMethods.headerFont(getActivity()));
        rideLater.setTypeface(CommonMethods.headerFont(getActivity()));
        emptyView.setTypeface(CommonMethods.headerFont(getActivity()));
        driverFound.setTypeface(CommonMethods.headerFont(getActivity()));
        fareEstimate.setTypeface(CommonMethods.headerFont(getActivity()));
        pickupLocationAddress.setTypeface(CommonMethods.headerFont(getActivity()));
        dropoffLocationAddress.setTypeface(CommonMethods.headerFont(getActivity()));

        rideNow.setOnClickListener(this);
        addNotes.setOnClickListener(this);
        rideLater.setOnClickListener(this);
        addAddress.setOnClickListener(this);
        fareEstimate.setOnClickListener(this);
        goToDropPosition.setOnClickListener(this);
        goToPickupPosition.setOnClickListener(this);
        pickupLocationAddress.setOnClickListener(this);

        notesLayout.setVisibility(View.GONE);

        getCarDetailsAndSet(mHelper.getPref(Constants.VEHICLE_TYPE, ""), mHelper.getPref(Constants.VEHICLE_MESSAGE, ""));

        if (!mCarsList.isEmpty()) {
            mCarsList.get(0).setSelected(true);
            typeID = mCarsList.get(0).getVehicle_type_id();
            baseFareType = mCarsList.get(0).getBase_fare_type();
            typeCityID = mCarsList.get(0).getVehicle_type_city_id();
            selectedCarImage = mCarsList.get(0).getNormal_image();
        }

        goForPickUp = true;

        new BackgroundGeocodingTaskNew().execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        visibility = true;
        getActivity().registerReceiver(mMessageReceiver, mMessageFilter);
        if (detector.isConnectingToInternet()) {
            if (myTimer != null) {
                return;
            } else {

                myTimer = new Timer();
                myTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mGoogleMap != null) {
                                        VisibleRegion visibleRegion = mGoogleMap.getProjection().getVisibleRegion();
                                        Point x1 = mGoogleMap.getProjection().toScreenLocation(visibleRegion.farRight);
                                        Point y = mGoogleMap.getProjection().toScreenLocation(visibleRegion.nearLeft);
                                        Point centerPoint = new Point(x1.x / 2, y.y / 2);
                                        LatLng centerFromPoint = mGoogleMap.getProjection().fromScreenLocation(centerPoint);
                                        double lat = centerFromPoint.latitude;
                                        double lon = centerFromPoint.longitude;

                                        if ((lat == currentLatitude && lon == currentLongitude)) {
                                            Log.d(TAG, "Same Lat Long");
                                            if (!mPICKUP_ADDRESS.equals("")) {
                                                pickupLocationAddress.setText(mPICKUP_ADDRESS);
                                            } else {
                                                new BackgroundGeocodingTaskNew().execute();
                                            }
                                        } else {
                                            if (lat != 0.0 && lon != 0.0) {
                                                currentLatitude = lat;
                                                currentLongitude = lon;
                                                from_latitude = String.valueOf(lat);
                                                from_longitude = String.valueOf(lon);

                                                if (isAdded()) {
                                                    goForPickUp = true;
                                                    new BackgroundGeocodingTaskNew().execute();
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                };

                myTimer.schedule(myTimerTask, 0, 3000);
            }
            new BackgroundSubscribeMyChannel().execute();
            from_latitude = currentLatitude + "";
            from_longitude = currentLongitude + "";
           startPublishingWithTimer(typeCityID, typeID, baseFareType, selectedCarImage,categoryId);
        } else {
            Toast.makeText(getActivity(), getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        visibility = false;
        getActivity().unregisterReceiver(mMessageReceiver);
        new BackgroundUnSubscribeAll().execute();

        if (myTimer != null) {
            myTimer.cancel();
            myTimer = null;
        }

        if (myTimerTask != null) {
            myTimerTask.cancel();
            myTimerTask = null;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (tracker.canGetLocation()) {
            if (tracker.getLatitude() == 0.0 && tracker.getLongitude() == 0.0) {
                Toast.makeText(getActivity(), R.string.no_location_view, Toast.LENGTH_SHORT).show();
            } else {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                LatLng latLng = new LatLng(currentLatitude, currentLongitude);

                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity()
                        , Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }

                try {
                    boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));
                    if (!success) {
                        Log.e(TAG, "Style parsing failed.");
                    }
                } catch (Resources.NotFoundException e) {
                    Log.e(TAG, "Can't find style. Error: ", e);
                }

                googleMap.setMyLocationEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP_FOR_HOME));

                getDriverDetails(mHelper.getPref(Constants.VEHICLE_DRI, ""), "3");

                JSONObject mObject = new JSONObject();
                /*rq_tp = 3, usr_id =,tp =,lt,lg,chn*/
                try {
                    mObject.put("rq_tp", "3");
//                    mObject.put("rq_fr", mHelper.getPref(Constants.REQUEST_SEARCH_DRIVER, 0));
                    mObject.put("usr_id", mHelper.getPref(Constants.USER_ID, ""));
                    mObject.put("lt", currentLatitude);
                    mObject.put("lg", currentLongitude);
                    mObject.put("tp", typeCityID);
                    mObject.put("cat", mArray.getJSONObject(0).optString("vehiclecategory_id"));
                    mObject.put("chn", mHelper.getPref(Constants.PUBNUB_CHANNEL_TYPE, ""));

                    Log.d(TAG + " Publish ", mObject.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                pubNubPublish(mHelper.getPref(Constants.PUBNUB_SERVER_CHANNEL, ""), mObject);
            }
        } else {
            tracker.showSettingsAlert();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ride_now:
                String driverData = mHelper.getPref(Constants.VEHICLE_DRI, "");
                Log.d(TAG, driverData);
                if (!mHelper.getPref(Constants.VEHICLE_DRI, "").equals("[]")) {
                    if (!isSetDropoffLocation) {
                        CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.select_drop_off_location));
                    } else if (paymentType == 0) {
                        bookingType = 1;
                        showPopUpForPayment();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.no_driver_found, Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.ride_later:
                if (!isSetDropoffLocation) {
                    CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.select_drop_off_location));
                } else {
                    bookingType = 2;
                    paymentType = 1;
                    goForLaterBooking();
                }

                break;
            case R.id.show_addr_text_view:
                Intent addressIntent = new Intent(getActivity(), SearchAddressGooglePlacesActivity.class);
                addressIntent.putExtra("chooser", getResources().getString(R.string.pickup_location));
                startActivityForResult(addressIntent, 18);
                getActivity().overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                break;
            case R.id.show_drop_off:
                addressIntent = new Intent(getActivity(), SearchAddressGooglePlacesActivity.class);
                addressIntent.putExtra("chooser", getResources().getString(R.string.drop_off));
                startActivityForResult(addressIntent, 16);
                getActivity().overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                break;
            case R.id.go_to_current_drop_position:
                if (tracker.canGetLocation()) {
                    isSetDropoffLocation = true;
                    goForDropOff = true;
                    tracker = new GPSTracker(getActivity());
                    LatLng latLng = new LatLng(tracker.getLatitude(), tracker.getLongitude());
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP_FOR_HOME));
                    new BackgroundGeocodingTaskNew().execute();
                } else {
                    tracker.showSettingsAlert();
                }
                break;
            case R.id.go_to_current_pickup_position:
                if (tracker.canGetLocation()) {
                    goForPickUp = true;
                    tracker = new GPSTracker(getActivity());
                    LatLng latLng = new LatLng(tracker.getLatitude(), tracker.getLongitude());
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP_FOR_HOME));
                    new BackgroundGeocodingTaskNew().execute();
                } else {
                    tracker.showSettingsAlert();
                }
                break;
        }
    }

    private void showPopUpForPayment() {
        /*final Dialog mDialog = new Dialog(getActivity());
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(R.layout.payment_dialog);

        TextView mCash = mDialog.findViewById(R.id.cash_payment);
        TextView mCard = mDialog.findViewById(R.id.card_payment);

        mCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentType = 1;
                goForSendRequest();
                mDialog.dismiss();
            }
        });

        mCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDialog.dismiss();

            }
        });

        mDialog.show();*/
        startActivityForResult(new Intent(getActivity(), ChangeCard.class), 17);
        getActivity().overridePendingTransition(R.anim.slide_up_acvtivity, R.anim.stay);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {

                from_latitude = data.getStringExtra("FROM_LATITUDE");
                from_longitude = data.getStringExtra("FROM_LONGITUDE");
                to_latitude = data.getStringExtra("TO_LATITUDE");
                to_longitude = data.getStringExtra("TO_LONGITUDE");
                String to_searchAddress = data.getStringExtra("to_SearchAddress");
//                pickupLocationAddress.setText(from_searchAddress);
                dropoffLocationAddress.setText(to_searchAddress);

            } else {
                System.out.println("on sucess inside else");
            }
        }
        if (requestCode == 16) {
            if (resultCode == Activity.RESULT_OK) {

                String latitudeString = data.getStringExtra("LATITUDE_SEARCH");
                String logitudeString = data.getStringExtra("LONGITUDE_SEARCH");
                String searchAddress = data.getStringExtra("SearchAddress");
                to_latitude = latitudeString;
                to_longitude = logitudeString;
                System.out.println("onActivityResult latitudeString...." + latitudeString + "...logitudeString..." + logitudeString);
                Log.d(TAG, searchAddress);
                if (searchAddress != null) {
                    isSetDropoffLocation = true;
                    mDROPOFF_ADDRESS = searchAddress;
                    dropoffLocationAddress.setText(searchAddress);
                    calculateFare();
                }
            }
        }

        if (requestCode == 17) {
            if (resultCode == Activity.RESULT_OK) {
                paymentType = 3;
                String cardId = data.getStringExtra(Constants.CARD_ID);
                makeCardAsDefault(cardId);
            }
        }

        if (requestCode == 18) {
            if (resultCode == Activity.RESULT_OK) {
                String latitudeString = data.getStringExtra("LATITUDE_SEARCH");
                String logitudeString = data.getStringExtra("LONGITUDE_SEARCH");
                mPICKUP_ADDRESS = data.getStringExtra("SearchAddress");

                formattedAddress = data.getStringExtra("SearchAddress");
                locationName = data.getStringExtra("ADDRESS_NAME");

                if (mPICKUP_ADDRESS != null) {

                    if (locationName != null && !locationName.isEmpty()) {
                        pickupLocationAddress.setText(locationName + " " + mPICKUP_ADDRESS);
                    } else {
                        pickupLocationAddress.setText(mPICKUP_ADDRESS);
                    }
                }

                from_latitude = latitudeString;
                from_longitude = logitudeString;
                currentLatitude = Double.parseDouble(latitudeString);
                currentLongitude = Double.parseDouble(logitudeString);

                System.out.println("onActivityResult latitudeString...." + latitudeString + "...logitudeString..." + logitudeString);
//                    currentLatitude = Double.parseDouble(latitudeString);
//                    currentLongitude = Double.parseDouble(logitudeString);
                LatLng latLng = new LatLng(Double.parseDouble(from_latitude), Double.parseDouble(from_longitude));

                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.ZOOM_MAP_FOR_HOME));

            }
        }
        if (requestCode == 22) {
            if (resultCode == Activity.RESULT_OK) {
                paymentType = 0;
                mDROPOFF_ADDRESS = "";
                dropoffLocationAddress.setText("");
                isSetDropoffLocation = false;
                notesLayout.setVisibility(View.GONE);
                if (data.getIntExtra("status", 0) == 1) {
                    Intent intent = new Intent(getActivity(), RideInfo.class);
                    intent.putExtra("city_id", typeCityID);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        }
    }

    /**
     * method used to go to send request activity
     */
    private void goForSendRequest() {
        try {
            if (typeCityID.equalsIgnoreCase("")) {

                typeCityID = mArray.getJSONObject(0).getJSONArray("sub_vechicle").getJSONObject(0).getString("vehicle_type_city_id");
            }
            if (typeID.equalsIgnoreCase("")) {
                typeID =  mArray.getJSONObject(0).getJSONArray("sub_vechicle").getJSONObject(0).getString("vehicle_type_id");
            }
        }
        catch (Exception e)
        {

        }
        Intent intent = new Intent(getActivity(), SendRequest.class);
        intent.putExtra("PICKUP_ADDRESS", mPICKUP_ADDRESS);
        intent.putExtra("DROPOFF_ADDRESS", mDROPOFF_ADDRESS);
        intent.putExtra("FromLatitude", from_latitude);
        intent.putExtra("FromLongitude", from_longitude);
        intent.putExtra("ToLatitude", to_latitude);
        intent.putExtra("ToLongitude", to_longitude);
        intent.putExtra("promoCodeId", promoCodeId);
        intent.putExtra("TypeCityId", typeCityID);
        intent.putExtra("TypeId", typeID);
        intent.putExtra("notes", notesForDriver);
        intent.putExtra("bookingType", bookingType);
        intent.putExtra("paymentType", paymentType);
        startActivityForResult(intent, 22);
    }

    /**
     * method used to go for later booking activity
     */
    private void goForLaterBooking() {
        Intent intent = new Intent(getActivity(), RideLaterActivity.class);
        intent.putExtra("PICKUP_ADDRESS", mPICKUP_ADDRESS);
        intent.putExtra("DROPOFF_ADDRESS", mDROPOFF_ADDRESS);
        intent.putExtra("FromLatitude", from_latitude);
        intent.putExtra("FromLongitude", from_longitude);
        intent.putExtra("ToLatitude", to_latitude);
        intent.putExtra("ToLongitude", to_longitude);
        intent.putExtra("TypeCityId", typeCityID);
        intent.putExtra("TypeId", typeID);
        intent.putExtra("notes", notesForDriver);
        intent.putExtra("bookingType", bookingType);
        intent.putExtra("paymentType", paymentType);
        intent.putExtra("car_image", selectedCarImage);

        startActivityForResult(intent, 23);
    }

    /**
     * method used to call pub nub
     */
    public void startPublishingWithTimer(final String tcID, String tId, String base_fare_type, String hover_image,String categoryId) {
        if (!typeCityID.equals(tcID)) {
            if (!mDROPOFF_ADDRESS.equals("")) {
                calculateFare();
            }
        }
        if (detector.isConnectingToInternet()) {
            typeCityID = tcID;
            typeID = tId;
            baseFareType = base_fare_type;
            selectedCarImage = hover_image;

            if (currentLatitude == 0.0 || currentLongitude == 0.0) {
                Log.d(TAG, getString(R.string.no_location_view));
            } else {
                JSONObject mObject = new JSONObject();
                try {
                    mObject.put("rq_tp", "3");
                    mObject.put("usr_id", mHelper.getPref(Constants.USER_ID, ""));
                    mObject.put("lt", from_latitude);
                    mObject.put("lg", from_longitude);
                    mObject.put("tp", typeCityID);
                    mObject.put("cat", categoryId);
                    mObject.put("chn", mHelper.getPref(Constants.PUBNUB_CHANNEL_TYPE, ""));

                    Log.d(TAG + " Publish ", mObject.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                pubNubPublish(mHelper.getPref(Constants.PUBNUB_SERVER_CHANNEL, ""), mObject);
            }

        } else {
            Toast.makeText(getActivity(), getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * method used to get cars detail and set to recycler view
     *
     * @param type    contain type array
     * @param message contain message vehicle available or not
     */
    public void getCarDetailsAndSet(String type, final String message) {
        mCarsList.clear();
        try {
            mArray = new JSONArray(type);
            if (mArray.length() > 0) {
                emptyView.setVisibility(View.GONE);
                carsList.setVisibility(View.VISIBLE);
                carpager.setAdapter(new CarPagerAdapter(getActivity(), mArray.length(), mArray, BookYourRide.this));
              /*  for (int i = 0; i < mArray.length(); i++) {

                  JSONArray subarry=  mArray.getJSONObject(i).getJSONArray("sub_vechicle");
                    for (int j = 0; i < subarry.length(); j++) {
                        JSONObject mObject = subarry.getJSONObject(j);
                        CarDetail carDetails = new CarDetail();
                        carDetails.setVehicle_type_city_id(mObject.getString("vehicle_type_city_id"));
                        carDetails.setVehicle_type_id(mObject.getString("vehicle_type_id"));
                        carDetails.setType_name(mObject.getString("type_name"));
                        carDetails.setMax_size(mObject.getString("max_size"));
                        carDetails.setMin_size(mObject.getString("min_size"));
                        carDetails.setBase_fare(mObject.getString("base_fare"));
                        carDetails.setMax_price_per_distance(mObject.getString("max_price_per_distance"));
                        carDetails.setBase_fare_type(mObject.getString("base_fare_type"));
                        carDetails.setMin_fare(mObject.getString("min_fare"));
                        carDetails.setPrice_per_min(mObject.getString("price_per_min"));
                        carDetails.setMin_price_per_distance(mObject.getString("min_price_per_distance"));
                        carDetails.setPrice_cancellation(mObject.getString("price_cancellation"));
                        carDetails.setType_desc(mObject.getString("type_desc"));
                        carDetails.setCity_id(mObject.getString("city_id"));
                        carDetails.setCity_name(mObject.getString("city_name"));
                        carDetails.setBasefare_minute(mObject.getString("basefare_minute"));
                        carDetails.setNormal_image(mObject.getString("normal_image"));
                        carDetails.setHover_image(mObject.getString("hover_image"));
                        carDetails.setSelected(false);
                        mCarsList.add(carDetails);

                    }

                    mAdapter.notifyDataSetChanged();

                    typeID = mCarsList.get(0).getVehicle_type_id();
                    typeCityID = mCarsList.get(0).getVehicle_type_city_id();


                }
*/

            } else {
                emptyView.setText(message);
                emptyView.setVisibility(View.VISIBLE);
                carsList.setVisibility(View.GONE);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getCarDetailsAndSet(mHelper.getPref(Constants.VEHICLE_TYPE, ""), mHelper.getPref(Constants.VEHICLE_MESSAGE, ""));
                    }
                }, Constants.MY_SOCKET_TIMEOUT_MS);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, e.toString());
        }
    }

    /**
     * method used to show car detail dialog
     */
    public void showCarDetails(String type_name, String type_desc, String min_price_per_distance, String max_price_per_distance, String min_fare,
                               String max_size, String base_fare, String price_per_min, String base_fare_type, String hover_image) {
        Dialog detailDialog = new Dialog(getActivity());
        detailDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        detailDialog.setContentView(R.layout.car_details);

        ImageView carImage = detailDialog.findViewById(R.id.car_image);
        TextView baseFare = detailDialog.findViewById(R.id.base_fare);
        TextView mintsAndMiles = detailDialog.findViewById(R.id.minutes_and_miles);
        TextView distance = detailDialog.findViewById(R.id.distance);
        TextView minFare = detailDialog.findViewById(R.id.min_fare);
        TextView maxSize = detailDialog.findViewById(R.id.max_size);

        baseFareType = base_fare_type;
        minFare.setTypeface(CommonMethods.headerFont(getActivity()));
        maxSize.setTypeface(CommonMethods.headerFont(getActivity()));
        baseFare.setTypeface(CommonMethods.headerFont(getActivity()));
        distance.setTypeface(CommonMethods.headerFont(getActivity()));
        mintsAndMiles.setTypeface(CommonMethods.headerFont(getActivity()));

        if (driverDis.equals("")) {
            distance.setText("--");
        } else {
            distance.setText(driverDis);
        }
        minFare.setText(String.format("%s%s", Constants.CURRENCY_SIGN, min_fare));
        maxSize.setText(String.format("%s%s", max_size, getString(R.string.people)));
        baseFare.setText(String.format("%s%s%s", Constants.CURRENCY_SIGN, base_fare, getString(R.string.base_fare)));

        if (base_fare_type.equals("1")) {
            mintsAndMiles.setText(String.format("%s%s%s%s%s%s%s", Constants.CURRENCY_SIGN, price_per_min, getString(R.string.minutes), getString(R.string.and), Constants.CURRENCY_SIGN, max_price_per_distance, getString(R.string.miles)));
        } else if (base_fare_type.equals("2")) {
            mintsAndMiles.setText(String.format("%s%s%s%s%s%s - %s%s", Constants.CURRENCY_SIGN, price_per_min, getString(R.string.minutes), getString(R.string.and), Constants.CURRENCY_SIGN, min_price_per_distance, max_price_per_distance, getString(R.string.miles)));
        }
        Glide.with(getActivity()).load(Constants.CAR_IMAGE_URL + hover_image).crossFade().placeholder(R.drawable.ic_masarcar_grey).into(carImage);
        detailDialog.show();
    }

    /**
     * method used to get Driver details
     *
     * @param driverData contain driver data
     * @param rqTp
     */
    public void getDriverDetails(String driverData, String rqTp) {
        if (rqTp.equals("3")) {
            if (!driverData.equals("[]")) {
                try {
                    JSONArray mArray = new JSONArray(driverData);
                    pinDrop.setVisibility(View.GONE);
                    driverFound.setText(R.string.set_pickup_location);
                    driverFound.setVisibility(View.VISIBLE);
//                    driverDistance.setVisibility(View.GONE);
                    for (int i = 0; i < mArray.length(); i++) {
                        JSONObject mObject = mArray.getJSONObject(i);
                        String driverName = mObject.getString("name");
                        String driverMobile = mObject.getString("mobile");
                        String driverLocation = mObject.getString("location");
                        JSONObject dLocation = new JSONObject(driverLocation);
                        String driverLat = dLocation.getString("latitude");
                        String driverLong = dLocation.getString("longitude");
                        String distance = mObject.getString("dis");

                        String convertDis = CommonMethods.getDecimalValue(distance);
                        driverDistance.setVisibility(View.GONE);
                        driverDistance.setText(String.format("%s \n %s", convertDis, getString(R.string.distance_unit)));

                        driverDis = convertDis + " " + getString(R.string.distance_unit);

                        createMarker(Double.valueOf(driverLat), Double.valueOf(driverLong), driverName, driverMobile, R.drawable.home_caricon_red);

                    }
                    Log.d(TAG + " Driver Data:-", mArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, e.toString());
                }
            } else {
                Log.d(TAG, "No driver found");
                mGoogleMap.clear();
                pinDrop.setVisibility(View.VISIBLE);
                driverCar.setVisibility(View.GONE);
                driverFound.setText(R.string.no_driver_found);
                driverDistance.setVisibility(View.GONE);
            }
        } else if (rqTp.equals("5")) {
            try {
                JSONObject mObject = new JSONObject(driverData);
                String driverLt = mObject.getString("lt");
                String driverLg = mObject.getString("lg");

                mHelper.savePref("driver_lat", driverLt);
                mHelper.savePref("driver_long", driverLg);
//                Log.d(TAG, "getDriverDetails: "+Double.parseDouble(driverLt));


            } catch (JSONException e) {
                e.printStackTrace();
                Log.d(TAG, e.toString());
            }

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        visibility = false;
    }

    /**
     * method used to create marker
     */
    protected void createMarker(double latitude, double longitude, String title, String snippet, int iconResID) {

        mGoogleMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(iconResID)));
//                .title(title)
//                .snippet(snippet)

    }

    @Override
    public void onStop() {
        super.onStop();
        mHelper.savePref("taxi_view", 0);
        mHelper.savePref("restaurant_market", 0);
    }

    /**
     * method used to make card as default
     *
     * @param cardId
     */
    private void makeCardAsDefault(final String cardId) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.MAKE_CARD_DEFAULT, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    mDialog.dismiss();
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            goForSendRequest();
                        } else {
                            CommonMethods.showAlert(getActivity(), getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("card_id", cardId);
                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(getActivity()).add(mRequest);
        } else {
            CommonMethods.showAlert(getActivity(), getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to calculate fare
     */
    private void calculateFare() {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.calculating_fare));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.CALCULATE_FARE, new Response.Listener<String>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String amount = dataObject.getString("amount");
                            String distanceF = dataObject.getString("distance");
                            notesLayout.setVisibility(View.VISIBLE);
                            addNotes.setText(getString(R.string.estimated_fare) + " " + Constants.CURRENCY_SIGN + new DecimalFormat("##.#").format(Double.parseDouble(amount)));
                            fareEstimate.setText(getString(R.string.distance) + " " + new DecimalFormat("##.#").format(Double.parseDouble(distanceF)) + " km");
                        } else {
                           // CommonMethods.showAlert(getActivity(), getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("pick_latitude", from_latitude);
                    mParams.put("pick_longitude", from_longitude);
                    mParams.put("drop_latitude", to_latitude);
                    mParams.put("drop_longitude", to_longitude);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("vehicle_type_city_id", typeCityID);

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(getActivity()).add(mRequest);
        } else {
            CommonMethods.showAlert(getActivity(), getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * Create a Geofence list by adding all fences you want to track
     */
    public void createGeofences(double latitude, double longitude) {

        String id = UUID.randomUUID().toString();
        fence = new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setCircularRegion(latitude, longitude, 2000) // Try changing your radius
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
//        mGeofenceList.add(fence);
        getGeofencePendingIntent();
        getGeofencingRequest();
        Log.d(TAG, "createGeofences:" + mGeofenceList.size());
    }

    private GeofencingRequest getGeofencingRequest() {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(fence);
        return builder.build();

    }

    private PendingIntent getGeofencePendingIntent() {

        // Reuse the PendingIntent if we already have it.

        Toast.makeText(getContext(), "hiiii", Toast.LENGTH_SHORT).show();

        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(getActivity(), GeofenceTransitionsIntentService.class);
        Log.d(TAG, "getGeofencePendingIntent: ");
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(getActivity(), 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);

    }

    /**
     * method used to get address from lat long
     */
    @SuppressLint("StaticFieldLeak")
    private class BackgroundGeocodingTaskNew extends AsyncTask<String, Void, String> {
        GeocodingResponse response;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pickupLocationAddress.setText(mPICKUP_ADDRESS);
        }

        @Override
        protected String doInBackground(String... params) {

            String url = "https://maps.google.com/maps/api/geocode/json?latlng=" + currentLatitude + ","
                    + currentLongitude + "&key=" + Constants.MAPS_GEOCODE;
//                    + currentLongitude + "&sensor=false";

            String stringResponse = CommonMethods.callhttpRequest(url);

            if (stringResponse != null) {
                Gson gson = new Gson();
                response = gson.fromJson(stringResponse, GeocodingResponse.class);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (response != null) {
                if (response.getStatus().equals("OK") && response.getResults() != null
                        && response.getResults().size() > 0) {
                    if (response.getResults().size() > 0
                            && !response.getResults().get(0).getFormatted_address().isEmpty()) {
                        if (goForPickUp) {
                            pickupLocationAddress.setText(response.getResults().get(0).getFormatted_address());
                            mPICKUP_ADDRESS = response.getResults().get(0).getFormatted_address();
                            from_latitude = String.valueOf(currentLatitude);
                            from_longitude = String.valueOf(currentLongitude);
                            goForPickUp = false;
                        } else if (goForDropOff) {
                            dropoffLocationAddress.setText(response.getResults().get(0).getFormatted_address());
                            mDROPOFF_ADDRESS = response.getResults().get(0).getFormatted_address();
                            to_latitude = String.valueOf(currentLatitude);
                            to_longitude = String.valueOf(currentLongitude);
                            goForDropOff = false;
                        }

                    }
                }

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (formattedAddress != null) {
                            if (locationName != null) {
                                if (goForPickUp) {
                                    pickupLocationAddress.setText(locationName + " " + formattedAddress);
                                    mPICKUP_ADDRESS = locationName + " " + formattedAddress;
                                    from_latitude = String.valueOf(currentLatitude);
                                    from_longitude = String.valueOf(currentLongitude);
                                    goForPickUp = false;
                                } else if (goForDropOff) {
                                    dropoffLocationAddress.setText(locationName + " " + formattedAddress);
                                    mDROPOFF_ADDRESS = locationName + " " + formattedAddress;
                                    to_latitude = String.valueOf(currentLatitude);
                                    to_longitude = String.valueOf(currentLongitude);
                                    goForDropOff = false;
                                }

                                formattedAddress = null;
                                locationName = null;
                            } else {
                                if (goForPickUp) {
                                    pickupLocationAddress.setText(String.format("%s %s", locationName, formattedAddress));
                                    mPICKUP_ADDRESS = locationName + " " + formattedAddress;
                                    from_latitude = String.valueOf(currentLatitude);
                                    from_longitude = String.valueOf(currentLongitude);
                                    goForPickUp = false;
                                } else if (goForDropOff) {
                                    dropoffLocationAddress.setText(String.format("%s %s", locationName, formattedAddress));
                                    mDROPOFF_ADDRESS = locationName + " " + formattedAddress;
                                    to_latitude = String.valueOf(currentLatitude);
                                    to_longitude = String.valueOf(currentLongitude);
                                    goForDropOff = false;
                                }
                                formattedAddress = null;
                            }
                        }
                    }
                }, 3000);
            } else {
                pickupLocationAddress.setText(mPICKUP_ADDRESS);
            }
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
        }
    }

    /**
     * unSubscribing the all channels
     */
    @SuppressLint("StaticFieldLeak")
    private class BackgroundUnSubscribeAll extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            pubnub.unsubscribeAll();
            return null;
        }

    }
}



