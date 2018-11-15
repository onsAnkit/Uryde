package uryde.passenger.rideLater;

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
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
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import uryde.passenger.R;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

public class RideLaterActivity extends AppCompatActivity implements View.OnClickListener {

    private int year;
    private int month;
    private int calenderDate;
    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    private Date dateCurrent, dateLater;
    private ConnectionDetector detector;
    private int bookingType, paymentType;
    private static final String TAG = RideLaterActivity.class.getName();
    private TextView timeAmPm, selectedDate, selectedTime, selectedMonth, selectedYear;
    private String promoCodeId = "0", toDate = "", timeUpdate = "", aTime = "", date = "", typeId = "", notes = "122", mPICKUP_ADDRESS = "", mDROPOFF_ADDRESS = "",
            from_latitude = "", from_longitude = "", to_latitude = "", to_longitude = "", carTypeCityId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_later);

        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        Calendar cal = Calendar.getInstance();
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        calenderDate = cal.get(Calendar.DATE);

        ImageView back = findViewById(R.id.back);
        Button cancel = findViewById(R.id.cancel);
        TextView title = findViewById(R.id.title);
        Button submit = findViewById(R.id.btnSubmit);
        mHelper = new PrefsHelper(RideLaterActivity.this);
        mDialog = new ProgressDialog(RideLaterActivity.this);
        detector = new ConnectionDetector(RideLaterActivity.this);

        timeAmPm = findViewById(R.id.time_am_pm);
        selectedDate = findViewById(R.id.selected_date);
        selectedTime = findViewById(R.id.selected_time);
        selectedYear = findViewById(R.id.selected_year);
        selectedMonth = findViewById(R.id.selected_month);
        ImageView carImage = findViewById(R.id.car_image);
        TextView pickupAddress = findViewById(R.id.pick_up_address);
        TextView dropOffAddress = findViewById(R.id.drop_off_address);
        TextView enterPromoCode = findViewById(R.id.enter_promo_layout);
        LinearLayout pickDateLayout = findViewById(R.id.pick_date_layout);
        LinearLayout pickTimeLayout = findViewById(R.id.pick_time_layout);

        notes = getIntent().getStringExtra("notes");
        typeId = getIntent().getStringExtra("TypeId");
        to_latitude = getIntent().getStringExtra("ToLatitude");
        to_longitude = getIntent().getStringExtra("ToLongitude");
        carTypeCityId = getIntent().getStringExtra("TypeCityId");
        from_latitude = getIntent().getStringExtra("FromLatitude");
        String sCarImage = getIntent().getStringExtra("car_image");
        from_longitude = getIntent().getStringExtra("FromLongitude");
        mDROPOFF_ADDRESS = getIntent().getStringExtra("DROPOFF_ADDRESS");
        mPICKUP_ADDRESS = getIntent().getStringExtra("PICKUP_ADDRESS");
        paymentType = getIntent().getIntExtra("paymentType", 0);
        bookingType = getIntent().getIntExtra("bookingType", 0);

        date = CommonMethods.getDateAndTime();

        title.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        cancel.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        submit.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        timeAmPm.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));

        selectedYear.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        selectedDate.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        selectedTime.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        selectedYear.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        selectedMonth.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        pickupAddress.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        dropOffAddress.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));
        enterPromoCode.setTypeface(CommonMethods.headerFont(RideLaterActivity.this));

        Glide.with(RideLaterActivity.this).load(Constants.CAR_IMAGE_URL + sCarImage).crossFade().placeholder(R.drawable.ic_masarcar_grey).into(carImage);

        String currentTime = CommonMethods.getCurrentTime();
        String time[] = currentTime.split("\\s+");
        String splitTime[] = selectedTime.getText().toString().trim().split(":");
        selectedTime.setText(time[0]);
        timeAmPm.setText(time[1]);

        String nowAsString = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String date[] = CommonMethods.getMonthDate(nowAsString, true).split("-");
        selectedDate.setText(date[0]);
        selectedMonth.setText(date[1]);
        selectedYear.setText(date[2]);

        pickupAddress.setText(mPICKUP_ADDRESS);
        dropOffAddress.setText(mDROPOFF_ADDRESS);

        timeUpdate = splitTime[0] + ":" + splitTime[1] + ":00";
        aTime = CommonMethods.getCurrentTime();
        toDate = selectedYear.getText().toString().trim() + "-" + getSelectedMonthNumber(selectedMonth.getText().toString().trim()) + "-" + selectedDate.getText().toString().trim();

        back.setOnClickListener(this);
        submit.setOnClickListener(this);
        cancel.setOnClickListener(this);
        pickDateLayout.setOnClickListener(this);
        pickTimeLayout.setOnClickListener(this);

    }

    /**
     * method used to get month number from month name
     *
     * @param sMonth contain month name
     * @return number
     */
    private String getSelectedMonthNumber(String sMonth) {
        switch (sMonth) {
            case "January":
                sMonth = "01";
                break;
            case "February":
                sMonth = "02";
                break;
            case "March":
                sMonth = "03";
                break;
            case "April":
                sMonth = "04";
                break;
            case "May":
                sMonth = "05";
                break;
            case "June":
                sMonth = "06";
                break;
            case "July":
                sMonth = "07";
                break;
            case "August":
                sMonth = "08";
                break;
            case "September":
                sMonth = "09";
                break;
            case "October":
                sMonth = "10";
                break;
            case "November":
                sMonth = "11";
                break;
            case "December":
                sMonth = "12";
                break;
        }

        return sMonth;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.pick_time_layout) {
            final Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            TimePickerDialog mTimePicker;
            mTimePicker = new TimePickerDialog(RideLaterActivity.this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {

                    timeUpdate = selectedHour + ":" + selectedMinute + ":00";
                    updateTime(selectedHour, selectedMinute);

                }
            }, hour, minute, false);//Yes 24 hour time
            mTimePicker.setTitle(getString(R.string.select_time));
            mTimePicker.show();

        }

        if (view.getId() == R.id.pick_date_layout) {
            CommonMethods.getDate(RideLaterActivity.this, selectedDate, year, month, calenderDate, true, selectedMonth, selectedYear);
        }
        if (view.getId() == R.id.cancel) {
            finish();
            overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
        }

        if (view.getId() == R.id.btnSubmit) {
            if (!Validate()) {
                Toast.makeText(RideLaterActivity.this, R.string.select_date_time, Toast.LENGTH_LONG).show();
            } else {
                toDate = selectedYear.getText().toString().trim() + "-" + getSelectedMonthNumber(selectedMonth.getText().toString().trim())
                        + "-" + selectedDate.getText().toString().trim();


                String laterTime = toDate + " " + timeUpdate;

                DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm a", new Locale("en", "US"));
                dateFormatter.setLenient(false);
                Date today = new Date();
                String currentDate = dateFormatter.format(today);

                DateFormat dateFormatterConvert = new SimpleDateFormat("yyyy-MM-dd hh:mm a", new Locale("en", "US"));

                try {
                    dateCurrent = dateFormatterConvert.parse(currentDate);
                    dateLater = dateFormatterConvert.parse(toDate + " " + aTime);
                    Log.d(TAG, "Later Date:- " + dateLater);
                    Log.d(TAG, "Current Date:- " + dateCurrent);

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (dateCurrent.after(dateLater)) {
                    CommonMethods.showAlert(RideLaterActivity.this, getString(R.string.message), getString(R.string.later_booking_error));
                } else {
                    long daysDifference = getDaysDifference(dateCurrent, dateLater);
                    long hourDifference = printDifference(dateCurrent, dateLater);
                    if (hourDifference >= 2) {
                        saveLaterBooking(laterTime);
                    } else if (daysDifference >= 1) {
                        saveLaterBooking(laterTime);
                    } else {
                        CommonMethods.showAlert(RideLaterActivity.this, getString(R.string.message), getString(R.string.select_later_booking_after_one_hour));
                    }
                }
            }
        }
        if (view.getId() == R.id.back) {
            finish();
        }
    }

    /**
     * method used to book later booking
     *
     * @param laterDate contain later date
     */
    private void saveLaterBooking(final String laterDate) {
        if (detector.isConnectingToInternet()) {
            if (mDialog != null) {
                mDialog.setMessage(getString(R.string.please_wait));
                mDialog.show();
            }
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.SEND_LATER_REQUEST, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    if (mDialog != null) {
                        if (mDialog.isShowing()) {
                            mDialog.dismiss();
                        }
                    }
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(RideLaterActivity.this);
                            alert.setTitle(getString(R.string.message));
                            alert.setMessage(message);
                            alert.setCancelable(false);
                            alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent returnIntent = new Intent();
                                    setResult(RESULT_OK, returnIntent);
                                    finish();
                                    overridePendingTransition(R.anim.mainfadein, R.anim.slide_down_acvtivity);
                                }
                            });
                            alert.show();
                        } else {
                            CommonMethods.showAlert(RideLaterActivity.this, getString(R.string.message), message);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(RideLaterActivity.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }


                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (mDialog != null) {
                        if (mDialog.isShowing()) {
                            mDialog.dismiss();
                        }
                    }
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(RideLaterActivity.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("drop_latitude", to_latitude);
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("pick_latitude", from_latitude);
                    mParams.put("pick_longitude", from_longitude);
                    mParams.put("vehicle_type_city_id", carTypeCityId);
                    mParams.put("pick_address", mPICKUP_ADDRESS);
                    mParams.put("appointment_date", laterDate);
                    mParams.put("appointment_timezone", TimeZone.getDefault().getID());
                    mParams.put("promocode_id", promoCodeId);
                    mParams.put("booking_type", bookingType + "");
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("vehicle_type_id", typeId);
                    mParams.put("payment_type", paymentType + "");
                    mParams.put("booking_date", date);
                    mParams.put("drop_longitude", to_longitude);
                    mParams.put("drop_address", mDROPOFF_ADDRESS);
                    mParams.put("extra_notes", notes);

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
            CommonMethods.showAlert(RideLaterActivity.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to get difference between two dates
     *
     * @param startDate contain start date
     * @param endDate   contain end date
     * @return difference
     */
    public long printDifference(Date startDate, Date endDate) {

        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : " + endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        System.out.printf(
                "%d days, %d hours, %d minutes, %d seconds%n",
                elapsedDays,
                elapsedHours, elapsedMinutes, elapsedSeconds);
        return elapsedHours;
    }

    /**
     * method used to get difference between two dates
     *
     * @param startDate contain start date
     * @param endDate   contain end date
     * @return difference
     */
    public long getDaysDifference(Date startDate, Date endDate) {

        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        System.out.println("startDate : " + startDate);
        System.out.println("endDate : " + endDate);
        System.out.println("different : " + different);

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        System.out.printf(
                "%d days, %d hours, %d minutes, %d seconds%n",
                elapsedDays,
                elapsedHours, elapsedMinutes, elapsedSeconds);
        return elapsedDays;
    }

    /**
     * method used to validate the fields
     *
     * @return true or false
     */
    private boolean Validate() {

        if (toDate == null) {
            return false;
        } else if (timeUpdate == null) {
            return false;
        }
        return true;
    }

    /**
     * method used to update date and time am pm
     *
     * @param hours contain hours
     * @param mins  contain minutes
     */
    private void updateTime(int hours, int mins) {

        String timeSet;
        if (hours > 12) {
            hours -= 12;
            timeSet = "PM";
        } else if (hours == 0) {
            hours += 12;
            timeSet = "AM";
        } else if (hours == 12)
            timeSet = "PM";
        else
            timeSet = "AM";

        String minutes;
        if (mins < 10)
            minutes = "0" + mins;
        else
            minutes = String.valueOf(mins);

        // Append in a StringBuilder
        aTime = String.valueOf(hours) + ':' +
                minutes + " " + timeSet;
        selectedTime.setText(hours + ":" + minutes);
        timeAmPm.setText(timeSet);

        Log.d("before time", " " + aTime);

    }
}
