package uryde.passenger;

import android.annotation.SuppressLint;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

public class RideHistoryDetail extends AppCompatActivity {

    private Button paymentType;
    private PrefsHelper mHelper;
    private String appointmentId;
    private ProgressDialog mDialog;
    private LinearLayout promoDetailView;
    private ConnectionDetector mDetector;
    public static final String TAG = RideHistoryDetail.class.getName();
    private TextView serviceCharge, driverName, carNumber, bookingId, bookingDate, price, balance, distance, speed, pickUpAddress, dropOffAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history_detail);

        init();
    }

    /**
     * init method used to initialization
     */
    private void init() {
        appointmentId = getIntent().getStringExtra(Constants.APPOINTMENT_ID);
        mHelper = new PrefsHelper(RideHistoryDetail.this);
        mDialog = new ProgressDialog(RideHistoryDetail.this);
        mDetector = new ConnectionDetector(RideHistoryDetail.this);

        mDialog.setCancelable(false);

        speed = findViewById(R.id.speed);
        price = findViewById(R.id.price);
        balance = findViewById(R.id.balance);
        distance = findViewById(R.id.distance);
        ImageView back = findViewById(R.id.back);
        carNumber = findViewById(R.id.car_number);
        TextView title = findViewById(R.id.title);
        bookingId = findViewById(R.id.booking_id);
        driverName = findViewById(R.id.driver_name);
        paymentType = findViewById(R.id.payment_type);
        bookingDate = findViewById(R.id.booking_date);
        TextView baseFare = findViewById(R.id.base_fare);
        serviceCharge = findViewById(R.id.service_charge);
        TextView speedText = findViewById(R.id.speed_text);
        pickUpAddress = findViewById(R.id.pick_up_address);
        dropOffAddress = findViewById(R.id.drop_off_address);
        TextView distanceText = findViewById(R.id.distance_text);
        TextView baseFareText = findViewById(R.id.base_fare_text);

        TextView promoValue = findViewById(R.id.promo_value);
        TextView totalAmount = findViewById(R.id.total_amount);
        promoDetailView = findViewById(R.id.promo_detail_view);
        TextView promoAmount = findViewById(R.id.promo_amount);
        TextView totalFareText = findViewById(R.id.total_fare_text);

        title.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        speed.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        distance.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        baseFare.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        carNumber.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        bookingId.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        speedText.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        driverName.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        promoValue.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        promoAmount.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        totalAmount.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        paymentType.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        bookingDate.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        distanceText.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        serviceCharge.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        baseFareText.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        totalFareText.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        pickUpAddress.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));
        dropOffAddress.setTypeface(CommonMethods.headerFont(RideHistoryDetail.this));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getAppointmentDetail(appointmentId);

    }

    /**
     * method used to get appointment details
     *
     * @param appointmentID contain appointment id
     */
    private void getAppointmentDetail(final String appointmentID) {
        if (mDetector.isConnectingToInternet()) {
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
                        String invalidResponse = mObject.getString("response_invalid");
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String appointment = dataObject.getString("appointment");
                            JSONObject appObject = new JSONObject(appointment);
                            String pickupAddress = appObject.getString("pick_address");
                            String drop_address = appObject.getString("drop_address");
                            appointmentId = appObject.getString("app_appointment_id");
                            String duration = appObject.getString("duration");
                            String total_amount = appObject.getString("total_amount");
                            String driver_name = appObject.getString("driver_name");
                            String vehicle_reg_no = appObject.getString("vehicle_reg_no");
                            String booking_date = appObject.getString("booking_date");
                            String payment_type = appObject.getString("payment_type");
                            String distance_in_mts = appObject.getString("distance_in_mts");
                            String service_charge = appObject.getString("service_charge");
                            String totalAfterServiceCharge = appObject.getString("total_amount_including_service_charge");

                            promoDetailView.setVisibility(View.GONE);

                            bookingId.setText(getString(R.string.booking_id_2131) + " " + appointmentId);
                            pickUpAddress.setText(pickupAddress);
                            dropOffAddress.setText(drop_address);

                            int time = Integer.valueOf(duration);
                            int hours = time / 60;

                            if (hours == 0) {
                                hours = 0;
                            }

                            speed.setText(hours + getString(R.string.mints));
                            driverName.setText(driver_name);
                            carNumber.setText(vehicle_reg_no);
                            bookingDate.setText(CommonMethods.getConvertedDate(booking_date));
                            double dis = CommonMethods.getDistanceInKilometers(Double.valueOf(distance_in_mts));
                            if (payment_type.equals("1")) {
                                paymentType.setText(getString(R.string.payment_type) + " " + getString(R.string.cash));
                            } else if (payment_type.equals("3")) {
                                paymentType.setText(getString(R.string.payment_type) + " " + getString(R.string.card));
                            }

                            price.setText(Constants.CURRENCY_SIGN + total_amount);
                            serviceCharge.setText(Constants.CURRENCY_SIGN + service_charge);
                            balance.setText(Constants.CURRENCY_SIGN + totalAfterServiceCharge);

                            distance.setText(new DecimalFormat("##.##").format(dis) + " " + getString(R.string.distance_unit));
                        } else if (invalidResponse.equals("1")) {
                            showAlert(RideHistoryDetail.this, getString(R.string.message), message);
                        } else {
                            CommonMethods.showAlert(RideHistoryDetail.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(RideHistoryDetail.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(RideHistoryDetail.this, getString(R.string.attention), getString(R.string.something_wrong));
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

            Volley.newRequestQueue(RideHistoryDetail.this).add(mRequest);
        } else {
            CommonMethods.showAlert(RideHistoryDetail.this, getString(R.string.no_internet), getString(R.string.internet_toast));
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
                Intent intent = new Intent(RideHistoryDetail.this, Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }


}
