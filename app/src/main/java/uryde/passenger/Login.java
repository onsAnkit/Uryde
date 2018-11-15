package uryde.passenger;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.GPSTracker;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private String deviceToken;
    private PrefsHelper mHelper;
    private String phone, password;
    private ProgressDialog mDialog;
    private double latitude, longitude;
    private ConnectionDetector detector;
    private EditText userPassword, userPhone;
    private static final int PERMISSION_FOR_LOCATION = 11;
    public static final String TAG = Login.class.getName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 112;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        mDialog = new ProgressDialog(Login.this);
        mDialog.setCancelable(false);
        mHelper = new PrefsHelper(Login.this);
        detector = new ConnectionDetector(Login.this);

        userPhone =  findViewById(R.id.user_phone);
        userPassword =  findViewById(R.id.user_password);
        TextView register =  findViewById(R.id.register);
        Button loginButton =  findViewById(R.id.login_button);
        TextView forgotPassword =  findViewById(R.id.forgot_password);

        register.setOnClickListener(this);
        loginButton.setOnClickListener(this);
        forgotPassword.setOnClickListener(this);

        userPhone.setTypeface(CommonMethods.headerFont(Login.this));
        userPassword.setTypeface(CommonMethods.headerFont(Login.this));
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.login_button) {
            phone = userPhone.getText().toString().trim();
            password = userPassword.getText().toString().trim();

            if (phone.isEmpty()) {
                userPhone.requestFocus();
                CommonMethods.showSnackBar(userPhone, getString(R.string.email_blank));
            }  else if (password.isEmpty()) {
                userPassword.requestFocus();
                CommonMethods.showSnackBar(userPhone, getString(R.string.password_blank));
            } else if (password.length() < 8) {
                userPassword.requestFocus();
                CommonMethods.showSnackBar(userPhone, getString(R.string.valid_password_lenght));
            } else {
                SharedPreferences mPref=getSharedPreferences(Constants.DEVICE_TOKEN,MODE_PRIVATE);
                deviceToken=mPref.getString(Constants.DEVICE_TOKEN,"");
                CommonMethods.hideKeyboard(Login.this);
                loginUser();
            }
        } else if (view.getId() == R.id.register) {
            userPhone.setText("");
            userPassword.setText("");
            goToRegister();
        } else if (view.getId() == R.id.forgot_password) {
            userPhone.setText("");
            userPassword.setText("");
            startActivity(new Intent(Login.this, ForgotPassword.class));
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (mHelper.getPref(Constants.DEVICE_TOKEN, "").equals("")) {
            if (checkPlayServices()) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
        }
        deviceToken = mHelper.getPref(Constants.DEVICE_TOKEN, "");
*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_FOR_LOCATION);
        } else {
            GPSTracker tracker = new GPSTracker(Login.this);
            if (tracker.canGetLocation()) {
                latitude = tracker.getLatitude();
                longitude = tracker.getLongitude();
            } else {
                tracker.showSettingsAlert();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_FOR_LOCATION) {
                GPSTracker tracker = new GPSTracker(Login.this);
                if (tracker.canGetLocation()) {
                    Log.d(TAG, tracker.getLatitude() + " -- " + tracker.getLongitude());
                } else {
                    tracker.showSettingsAlert();
                }
            }
        }
    }

    /**
     * method used to login user
     */
    private void loginUser() {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.signing_in));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.LOGIN, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    mDialog.dismiss();
                    try {
                        JSONObject mObject = new JSONObject(response);
                        Log.d("MyApp",response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String session = dataObject.getString("session_token");
                            String user = dataObject.getString("profile");
                            JSONObject userObject = new JSONObject(user);
                            String customerID = userObject.getString("customer_id");
                            String name = userObject.getString("name");
                            String email = userObject.getString("email");
                            String mobile = userObject.getString("mobile");
                            String walletBalance = userObject.getString("wallet_balance");
                            String profilePic = userObject.getString("profile_pic");
                            String bookingStatus = userObject.getString("booking_status");
                            String vehicle = dataObject.getString("vehilce_type");
                            JSONObject vObject = new JSONObject(vehicle);
                            String type = vObject.getString("type");
                        //    String vehMsg = vObject.getString("msg");
                            String tp = vObject.getString("tp");
                            String dri = vObject.getString("dri");
                            String stripeId = userObject.getString("stripe_id");
                            String pubNubChannel = dataObject.getString("pub_chn");
                            String serverChannel = dataObject.getString("ser_chn");
                            String countryCode = userObject.getString("country_code");

                            mHelper.savePref(Constants.VEHICLE_TP, tp);
                            mHelper.savePref(Constants.USER_NAME, name);
                            mHelper.savePref(Constants.VEHICLE_DRI, dri);
                            mHelper.savePref(Constants.USER_EMAIL, email);
                            mHelper.savePref(Constants.VEHICLE_TYPE, type);
                            mHelper.savePref(Constants.USER_ID, customerID);
                            mHelper.savePref(Constants.USER_MOBILE, mobile);
                            mHelper.savePref(Constants.STRIPE_ID, stripeId);
                            mHelper.savePref(Constants.SESSION_TOKEN, session);
                            mHelper.savePref(Constants.USER_IMAGE, profilePic);
                            mHelper.savePref(Constants.USER_PASSWORD, password);
                          //  mHelper.savePref(Constants.VEHICLE_MESSAGE, vehMsg);
                            mHelper.savePref(Constants.COUNTRY_CODE, countryCode);
                            mHelper.savePref(Constants.WALLET_BALANCE, walletBalance);
                            mHelper.savePref(Constants.BOOKING_STATUS, bookingStatus);
                            mHelper.savePref(Constants.PUBNUB_CHANNEL_TYPE, pubNubChannel);
                            mHelper.savePref(Constants.PUBNUB_SERVER_CHANNEL, serverChannel);
                            mHelper.savePref("user_login", true);

                            startActivity(new Intent(Login.this, LandingActivity.class));
                            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                            finish();

                        } else {
                            CommonMethods.showAlert(Login.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(Login.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(Login.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("email", phone);
                    mParams.put("device_type", "1");
                    mParams.put("password", password);
                    mParams.put("latitude", latitude + "");
                    mParams.put("longitude", longitude + "");
                    mParams.put("device_token", deviceToken);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("device_id", mHelper.getPref(Constants.DEVICE_ID, ""));

                    mParams.put("login_with", "3");
                    mParams.put("debug_mode", "1");

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
            CommonMethods.showAlert(Login.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to go to register activity
     */
    private void goToRegister() {
        startActivity(new Intent(Login.this, Register.class));
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
    }

    /**
     * method used to check play services
     *
     * @return true or false
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

}
