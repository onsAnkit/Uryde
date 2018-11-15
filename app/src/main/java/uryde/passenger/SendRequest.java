package uryde.passenger;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class SendRequest extends AppCompatActivity {

    private boolean isVisible;
    private PrefsHelper mHelper;
    private TextView hold_cancel;
    private int bookingType, paymentType;
    private ConnectionDetector detector;
    private RelativeLayout rl_blue, rl_red;
    public static final String TAG = SendRequest.class.getName();
    private String productComment = "", productImage = "", promoCodeId = "", date = "", typeId = "", notes = "", mPICKUP_ADDRESS = "", mDROPOFF_ADDRESS = "",
            from_latitude = "", from_longitude = "", to_latitude = "", to_longitude = "", carTypeCityId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_request);
        isVisible = true;
        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(SendRequest.this);
        detector = new ConnectionDetector(SendRequest.this);
        mPICKUP_ADDRESS = getIntent().getStringExtra("PICKUP_ADDRESS");
        mDROPOFF_ADDRESS = getIntent().getStringExtra("DROPOFF_ADDRESS");
        from_latitude = getIntent().getStringExtra("FromLatitude");
        from_longitude = getIntent().getStringExtra("FromLongitude");
        to_latitude = getIntent().getStringExtra("ToLatitude");
        to_longitude = getIntent().getStringExtra("ToLongitude");
        promoCodeId = getIntent().getStringExtra("promoCodeId");
        carTypeCityId = getIntent().getStringExtra("TypeCityId");
        typeId = getIntent().getStringExtra("TypeId");
        notes = getIntent().getStringExtra("notes");
        bookingType = getIntent().getIntExtra("bookingType", 0);
        paymentType = getIntent().getIntExtra("paymentType", 0);

        Log.d("Drop Off Address:- ", mDROPOFF_ADDRESS);

        ImageView cancel = (ImageView) findViewById(R.id.btn_cancel);
        hold_cancel = (TextView) findViewById(R.id.request);
        rl_red = (RelativeLayout) findViewById(R.id.relative_red);
        rl_blue = (RelativeLayout) findViewById(R.id.relative_blue);

        date = CommonMethods.getDateAndTime();

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hold_cancel.setText(getResources().getString(R.string.cancelling));
                rl_red.setVisibility(View.VISIBLE);
                rl_blue.setVisibility(View.INVISIBLE);
                cancelRequest();
            }
        });

//        new SendRequestData().execute(productImage);
        sendRequest();
    }

    /**
     * method used to send request to driver
     */
    private void cancelRequest() {
        if (detector.isConnectingToInternet()) {
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.CANCEL_REQUEST, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG + " Cancel", response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("status", 0);
                            setResult(RESULT_OK, returnIntent);
                            finish();
                            overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
                        } else {
                            if (isVisible) {
                                showErrorAlert(getString(R.string.message), message);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (isVisible) {
                            showErrorAlert(getString(R.string.attention), getString(R.string.something_wrong));
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    if (error instanceof TimeoutError) {
                        if (isVisible) {
                            showErrorAlert(getString(R.string.attention), getString(R.string.something_wrong));
                        }
                    } else {
                        if (isVisible) {
                            showErrorAlert(getString(R.string.attention), getString(R.string.something_wrong));
                        }
                    }
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("request_date", date);
                    mParams.put("user_timezone", TimeZone.getDefault().getID());
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));


                    Log.d(TAG + " Cancel", mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(this).add(mRequest);
        } else {
            Toast.makeText(SendRequest.this, getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * method used to show error alret
     *
     * @param title   contain title
     * @param message contain message
     */
    public void showErrorAlert(String title, String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(SendRequest.this);
        alert.setTitle(title);
        alert.setCancelable(false);
        alert.setMessage(message);
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("status", 0);
                setResult(RESULT_OK, returnIntent);
                finish();
                overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
            }
        });
        alert.show();
    }

    /**
     * class used to send request to driver
     */
    public void sendRequest() {
        if (detector.isConnectingToInternet()) {
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.SEND_REQUEST, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String appId = dataObject.getString("app_appointment_id");
                            mHelper.savePref(Constants.APPOINTMENT_ID, appId);
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Intent returnIntent = new Intent();
                                    returnIntent.putExtra("status", 1);
                                    setResult(RESULT_OK, returnIntent);
                                    finish();
                                    overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
                                }
                            }, 2500);

                        } else {
                            if (isVisible) {
                                showErrorAlert(getString(R.string.message), message);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, e.toString());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    finish();
                    Toast.makeText(SendRequest.this, getString(R.string.something_wrong), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, error.toString());
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("pick_latitude", from_latitude);
                    mParams.put("pick_longitude", from_longitude);
                    mParams.put("vehicle_type_city_id", carTypeCityId);
                    mParams.put("pick_address", mPICKUP_ADDRESS);
                    mParams.put("appointment_date", date);
                    mParams.put("appointment_timezone", TimeZone.getDefault().getID());
                    mParams.put("promocode_id", promoCodeId);
                    mParams.put("booking_type", bookingType + "");
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("drop_latitude", to_latitude);
                    mParams.put("drop_longitude", to_longitude);
                    mParams.put("drop_address", mDROPOFF_ADDRESS);
                    mParams.put("extra_notes", notes);
                    mParams.put("vehicle_type_id", typeId);
                    mParams.put("payment_type", paymentType + "");
                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(this).add(mRequest);
        } else {
            Toast.makeText(SendRequest.this, getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isVisible = false;
    }

    /**
     * Register user asyncTask used to register a user in app
     */
    private class SendRequestData extends AsyncTask<String, String, String> {

        String response = "";

        @Override
        protected String doInBackground(String... strings){
            if (strings[0].equals("")) {
                try {
                    HttpResponse<JsonNode> request = Unirest.post(Constants.BASE_URL + Constants.SEND_REQUEST)
                            .field("device_type", "1")
                            .field("pick_latitude", from_latitude)
                            .field("pick_longitude", from_longitude)
                            .field("vehicle_type_city_id", carTypeCityId)
                            .field("pick_address", mPICKUP_ADDRESS)
                            .field("appointment_date", date)
                            .field("promocode_id", promoCodeId)
                            .field("booking_type", bookingType + "")
                            .field("drop_latitude", to_latitude)
                            .field("drop_longitude", to_longitude)
                            .field("appointment_timezone", TimeZone.getDefault().getID())
                            .field("drop_address", mDROPOFF_ADDRESS)
                            .field("extra_notes", notes)
                            .field("vehicle_type_id", typeId)
                            .field("payment_type", paymentType + "")
                            .field("language", mHelper.getPref(Constants.APP_LANGUAGE, ""))
                            .field("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""))
                            .asJson();
                    response = request.getBody().toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    HttpResponse<JsonNode> request = Unirest.post(Constants.BASE_URL + Constants.SEND_REQUEST)
                            .field("device_type", "1")
                            .field("pick_latitude", from_latitude)
                            .field("pick_longitude", from_longitude)
                            .field("vehicle_type_city_id", carTypeCityId)
                            .field("pick_address", mPICKUP_ADDRESS)
                            .field("appointment_date", date)
                            .field("promocode_id", promoCodeId)
                            .field("booking_type", bookingType + "")
                            .field("drop_latitude", to_latitude)
                            .field("drop_longitude", to_longitude)
                            .field("appointment_timezone", TimeZone.getDefault().getID())
                            .field("drop_address", mDROPOFF_ADDRESS)
                            .field("extra_notes", notes)
                            .field("vehicle_type_id", typeId)
                            .field("payment_type", paymentType + "")
                            .field("language", mHelper.getPref(Constants.APP_LANGUAGE, ""))
                            .field("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""))
                            .field("product_image", new File(strings[0]))
                            .field("product_comment", productComment)
                            .asJson();
                    response = request.getBody().toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return response;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, response);
            try {
                JSONObject mObject = new JSONObject(response);
                int status = mObject.getInt(Constants.RESPONSE_STATUS);
                String message = mObject.getString(Constants.RESPONSE_MSG);
                if (status == 1) {
                    String data = mObject.getString(Constants.RESPONSE_DATA);
                    JSONObject dataObject = new JSONObject(data);
                    String appId = dataObject.getString("app_appointment_id");
                    mHelper.savePref(Constants.APPOINTMENT_ID, appId);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("status", 1);
                            setResult(RESULT_OK, returnIntent);
                            finish();
                            overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
                        }
                    }, 2500);

                } else {
                    if (isVisible) {
                        showErrorAlert(getString(R.string.message), message);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.d(TAG, e.toString());
                finish();
                overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
            }
        }
    }
}
