package uryde.passenger;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Invoice extends AppCompatActivity {

    private Button finish;
    private EditText review;
    private PrefsHelper mHelper;
    private RatingBar rateDriver;
    private String appointmentId;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    public static final String TAG = Invoice.class.getName();
    private TextView tipAmount,price, serviceCharge, amount, pickUpAddress, dropOffAddress, bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice);

        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(Invoice.this);
        mDialog = new ProgressDialog(Invoice.this);
        detector = new ConnectionDetector(Invoice.this);
        appointmentId = mHelper.getPref(Constants.APPOINTMENT_ID, "");
        TextView title = findViewById(R.id.title);
        rateDriver = findViewById(R.id.rate_driver);
        tipAmount = findViewById(R.id.tip_amount);
        pickUpAddress = findViewById(R.id.pick_up_address);
        dropOffAddress = findViewById(R.id.drop_off_address);
        bookingId = findViewById(R.id.booking_id);

        review = findViewById(R.id.review);
        TextView writeReview = findViewById(R.id.write_review);
        TextView howWasRide = findViewById(R.id.how_war_ride);

        finish = findViewById(R.id.finish);
        amount = findViewById(R.id.balance);
        price = findViewById(R.id.price);
        serviceCharge = findViewById(R.id.service_charge);

        mDialog.setCancelable(false);
        title.setTypeface(CommonMethods.headerFont(Invoice.this));
        amount.setTypeface(CommonMethods.headerFont(Invoice.this));
        tipAmount.setTypeface(CommonMethods.headerFont(Invoice.this));
        bookingId.setTypeface(CommonMethods.headerFont(Invoice.this));
        pickUpAddress.setTypeface(CommonMethods.headerFont(Invoice.this));
        dropOffAddress.setTypeface(CommonMethods.headerFont(Invoice.this));

        price.setTypeface(CommonMethods.headerFont(Invoice.this));
        finish.setTypeface(CommonMethods.headerFont(Invoice.this));
        review.setTypeface(CommonMethods.headerFont(Invoice.this));
        serviceCharge.setTypeface(CommonMethods.headerFont(Invoice.this));
        writeReview.setTypeface(CommonMethods.headerFont(Invoice.this));
        howWasRide.setTypeface(CommonMethods.headerFont(Invoice.this));

        getAppointmentDetail(appointmentId);

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (finish.getText().toString().trim().equals("PAY NOW")) {
                    Toast.makeText(Invoice.this, getString(R.string.come_soon), Toast.LENGTH_SHORT).show();
                } else if (finish.getText().toString().trim().equals(getString(R.string.submit))) {
                    String tip = tipAmount.getText().toString().trim();
                    String userReview = review.getText().toString().trim();
                    float userRating = rateDriver.getRating();
                    if (userRating == 0) {
                        CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.please_select_rating));
                    } else {
                        payForBooking(appointmentId, userReview, userRating,tip);
                    }
                }
            }
        });
    }

    /**
     * method used to get appointment details
     *
     * @param appointmentID contain appointment id
     */
    private void getAppointmentDetail(final String appointmentID) {
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
                        String invalidResponse = mObject.getString("response_invalid");
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String appointment = dataObject.getString("appointment");
                            JSONObject appObject = new JSONObject(appointment);
                            String drop_address = appObject.getString("drop_address");
                            appointmentId = appObject.getString("app_appointment_id");
                            String total_amount = appObject.getString("total_amount");
                            String pickupAddress = appObject.getString("pick_address");
                            String service_charge = appObject.getString("service_charge");
                            String totalAfterServiceCharge = appObject.getString("total_amount_including_service_charge");

                            bookingId.setText(getString(R.string.booking_id_2131) + " " + appointmentId);
                            pickUpAddress.setText(pickupAddress);
                            dropOffAddress.setText(drop_address);
                            finish.setText(getString(R.string.submit));

                            price.setText(Constants.CURRENCY_SIGN + total_amount);
                            serviceCharge.setText(Constants.CURRENCY_SIGN + service_charge);
                            amount.setText(Constants.CURRENCY_SIGN + totalAfterServiceCharge);

                        } else if (invalidResponse.equals("1")) {
                            showAlert(Invoice.this, getString(R.string.message), message);
                        } else {
                            CommonMethods.showAlert(Invoice.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("app_appointment_id", appointmentID);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(Invoice.this).add(mRequest);
        } else {
            CommonMethods.showAlert(Invoice.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to pay booking amount via card
     *
     * @param tip contain amount of tip
     */
    private void payForBooking(final String appointmentId, final String userReview, final float userRating, final String tip) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.PAY_FOR_BOOKING, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);

                        if (status == 1) {
                            mHelper.savePref(Constants.APPOINTMENT_ID, "0");
                            sendFeedBack(appointmentId, userReview, userRating);
                        } else {
                            CommonMethods.showAlert(Invoice.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams()  {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("device_type", "1");
                    mParams.put("app_appointment_id", Invoice.this.appointmentId);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("paypal_transaction_id", "");
                    mParams.put("tip", tip);

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(Invoice.this).add(mRequest);

        } else {
            CommonMethods.showAlert(Invoice.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to send feed back for particular appointment
     *
     * @param appointmentId contain appointment id
     * @param userReview    contain user review
     * @param userRating    contain rating by user
     */
    private void sendFeedBack(final String appointmentId, final String userReview, final float userRating) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.SEND_FEEDBACK, new Response.Listener<String>() {
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
                            mHelper.savePref(Constants.APPOINTMENT_ID, "0");
                            Intent intent = new Intent(Invoice.this, LandingActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                            finish();
                        } else if (invalidResponse.equals("1")) {
                            showAlert(Invoice.this, getString(R.string.message), message);
                        } else {
                            CommonMethods.showAlert(Invoice.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(Invoice.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("app_appointment_id", appointmentId);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("rating", userRating + "");
                    mParams.put("review", userReview);

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(Invoice.this).add(mRequest);
        } else {
            CommonMethods.showAlert(Invoice.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
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
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHelper.clearAllPref();
                Intent intent = new Intent(Invoice.this, Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }
}
