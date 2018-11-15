package uryde.passenger;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.GPSTracker;
import uryde.passenger.util.PrefsHelper;

import static android.content.Context.MODE_PRIVATE;

public class EnterOTP extends AppCompatActivity implements View.OnClickListener {

    private PrefsHelper mHelper;
    private EditText enteredOTP;
    private ProgressDialog mDialog;
    private double latitude, longitude;
    private ConnectionDetector detector;
    private static final int PERMISSION_FOR_LOCATION = 112;
    private static final String TAG = EnterOTP.class.getSimpleName();
    private String countryCode = "", otp = "", name = "", email = "", mobile = "", password = "", imagePath = "";
    private String deviceToken="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_otp);

        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(this);
        mDialog = new ProgressDialog(this);
        detector = new ConnectionDetector(this);

        ImageView back =  findViewById(R.id.back);
        enteredOTP =  findViewById(R.id.entered_otp);
        Button submitOTP =  findViewById(R.id.verify_otp);
        TextView otpText =  findViewById(R.id.otp_text);
        TextView resendOTP =  findViewById(R.id.resend_otp);
        TextView otpMessage =  findViewById(R.id.otp_message);

        name = getIntent().getStringExtra(Constants.USER_NAME);
        email = getIntent().getStringExtra(Constants.USER_EMAIL);
        mobile = getIntent().getStringExtra(Constants.USER_MOBILE);
        imagePath = getIntent().getStringExtra(Constants.USER_IMAGE);
        password = getIntent().getStringExtra(Constants.USER_PASSWORD);
        countryCode = getIntent().getStringExtra(Constants.COUNTRY_CODE);

        otpText.setTypeface(CommonMethods.headerFont(this));
        resendOTP.setTypeface(CommonMethods.headerFont(this));
        submitOTP.setTypeface(CommonMethods.headerFont(this));
        otpMessage.setTypeface(CommonMethods.headerFont(this));
        enteredOTP.setTypeface(CommonMethods.headerFont(this));

        back.setOnClickListener(this);
        submitOTP.setOnClickListener(this);
        resendOTP.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.verify_otp:
                otp = enteredOTP.getText().toString().trim();
                if (otp.isEmpty()) {
                    CommonMethods.showSnackBar(enteredOTP, getString(R.string.blank_otp));
                } else {
                    CommonMethods.hideKeyboard(EnterOTP.this);
                    if (detector.isConnectingToInternet()) {
                        GPSTracker mTracker = new GPSTracker(this);
                        if (mTracker.canGetLocation()) {
                            latitude = mTracker.getLatitude();
                            longitude = mTracker.getLongitude();

                            mDialog.setMessage(getString(R.string.signing_up));
                            mDialog.show();
                            SharedPreferences mPref=getSharedPreferences(Constants.DEVICE_TOKEN,MODE_PRIVATE);
                            deviceToken=mPref.getString(Constants.DEVICE_TOKEN,"");
                            new RegisterUser().execute(imagePath);
                        } else {
                            new GPSTracker(this).showSettingsAlert();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
                    }

                }
                break;
            case R.id.resend_otp:
                generateOTP(mobile, countryCode);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FOR_LOCATION);
        } else {
            GPSTracker tracker = new GPSTracker(EnterOTP.this);
            if (tracker.canGetLocation()) {
                latitude = tracker.getLatitude();
                longitude = tracker.getLongitude();
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
                private class RegisterUser extends AsyncTask<String, String, String> {

                    String response = "";

                    @Override
                    protected String doInBackground(String... params) {
                        if (params[0].equals("")) {
                            try {
                    HttpResponse<JsonNode> request = Unirest.post(Constants.BASE_URL + Constants.SIGN_UP)
                            .field("email", email)
                            .field("mobile", mobile)
                            .field("device_id", mHelper.getPref(Constants.DEVICE_ID, ""))
                            .field("device_token", deviceToken)
                            .field("device_type", Constants.DEVICE_TYPE)
                            .field("name", name)
                            .field("password", password)
                            .field("language", mHelper.getPref(Constants.APP_LANGUAGE, ""))
                            .field("latitude", latitude + "")
                            .field("longitude", longitude + "")
                            .field("profile_pic", params[0])
                            .field("debug_mode", Constants.DEBUG_MODE)
                            .field("country_code", countryCode)
                            .field("otp", otp)
                            .asJson();
                    response = request.getBody().toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    HttpResponse<JsonNode> request = Unirest.post(Constants.BASE_URL + Constants.SIGN_UP)
                            .field("email", email)
                            .field("mobile", mobile)
                            .field("device_id", mHelper.getPref(Constants.DEVICE_ID, ""))
                            .field("device_token", deviceToken)
                            .field("device_type", Constants.DEVICE_TYPE)
                            .field("name", name)
                            .field("password", password)
                            .field("language", mHelper.getPref(Constants.APP_LANGUAGE, ""))
                            .field("latitude", latitude + "")
                            .field("longitude", longitude + "")
                            .field("profile_pic", new File(params[0]))
                            .field("debug_mode", Constants.DEBUG_MODE)
                            .field("country_code", countryCode)
                            .field("otp", otp)
                            .asJson();
                    response = request.getBody().toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            Log.d(TAG, response);
            mDialog.dismiss();
            try {
                JSONObject mObject = new JSONObject(response);
                int status = mObject.getInt(Constants.RESPONSE_STATUS);
                String message = mObject.optString(Constants.RESPONSE_MSG);
                if (status == 1) {
                    String data = mObject.optString(Constants.RESPONSE_DATA);
                    JSONObject dataObject = new JSONObject(data);
                    String userInfo = dataObject.optString("user_info");
                    JSONObject userObject = new JSONObject(userInfo);
                    String customerID = userObject.optString("customer_id");
                    String name = userObject.optString("name");
                    String email = userObject.optString("email");
                    String mobile = userObject.optString("mobile");
                    String profilePic = userObject.optString("profile_pic");
                    String countryCode = userObject.optString("country_code");
                    String bookingStatus = userObject.optString("booking_status");
                    String vehicle = dataObject.optString("vehilce_type");
                    JSONObject vObject = new JSONObject(vehicle);
                    String type = vObject.optString("type");
                    String vehMsg = vObject.optString("msg");
                    String tp = vObject.optString("tp");
                    String dri = vObject.optString("dri");
                    String walletBalance = userObject.optString("wallet_balance");
                    String sessionToken = dataObject.optString("sess_tok");
                    String pubNubChannel = dataObject.optString("pub_chn");
                    String stripeId   = userObject.optString("stripe_id");
                    String serverChannel = dataObject.optString("ser_chn");

                    mHelper.savePref(Constants.VEHICLE_TP, tp);
                    mHelper.savePref(Constants.USER_NAME, name);
                    mHelper.savePref(Constants.VEHICLE_DRI, dri);
                    mHelper.savePref(Constants.VEHICLE_TYPE, type);
                    mHelper.savePref(Constants.USER_EMAIL, email);
                    mHelper.savePref(Constants.USER_ID, customerID);
                    mHelper.savePref(Constants.USER_MOBILE, mobile);
                    mHelper.savePref(Constants.STRIPE_ID, stripeId);
                    mHelper.savePref(Constants.USER_IMAGE, profilePic);
                    mHelper.savePref(Constants.USER_PASSWORD, password);
                    mHelper.savePref(Constants.VEHICLE_MESSAGE, vehMsg);
                    mHelper.savePref(Constants.COUNTRY_CODE, countryCode);
                    mHelper.savePref(Constants.SESSION_TOKEN, sessionToken);
                    mHelper.savePref(Constants.WALLET_BALANCE, walletBalance);
                    mHelper.savePref(Constants.BOOKING_STATUS, bookingStatus);
                    mHelper.savePref(Constants.PUBNUB_CHANNEL_TYPE, pubNubChannel);
                    mHelper.savePref(Constants.PUBNUB_SERVER_CHANNEL, serverChannel);
                    mHelper.savePref("user_login", true);

                    Intent intent = new Intent(EnterOTP.this, LandingActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                    finish();

                } else {
                    CommonMethods.showAlert(EnterOTP.this, getString(R.string.message), message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                CommonMethods.showAlert(EnterOTP.this, getString(R.string.attention), getString(R.string.something_wrong));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_FOR_LOCATION) {
                GPSTracker tracker = new GPSTracker(EnterOTP.this);
                if (tracker.canGetLocation()) {
                    latitude = tracker.getLatitude();
                    latitude = tracker.getLongitude();
                }
            }
        }
    }

    /**
     * method used to generate OTP
     *
     * @param mobile      contain user mobile
     * @param countryCode contain user country code
     */
    private void generateOTP(final String mobile, final String countryCode) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.generate_otp));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GENERATE_OTP, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            Toast.makeText(EnterOTP.this, message, Toast.LENGTH_SHORT).show();
                        } else {
                            CommonMethods.showAlert(EnterOTP.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(EnterOTP.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(EnterOTP.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("mobile", mobile);
                    mParams.put("email", email);
                    mParams.put("country_code", countryCode);
                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(this).add(mRequest);
        } else {
            Toast.makeText(this, getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
        }
    }
}
