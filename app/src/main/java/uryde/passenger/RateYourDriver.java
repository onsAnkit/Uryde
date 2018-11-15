package uryde.passenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RateYourDriver extends AppCompatActivity {

    private TextView title;
    private EditText review;
    private float rating = 0;
    private PrefsHelper mHelper;
    private String appointmentId;
    private Button cancel, submit;
    private ImageView driverImage;
    private RatingBar driverRating;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    public static final String TAG = RateYourDriver.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_your_driver);

        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(RateYourDriver.this);
        mDialog = new ProgressDialog(RateYourDriver.this);
        detector = new ConnectionDetector(RateYourDriver.this);

        appointmentId = getIntent().getStringExtra("appointment_id");

        title = (TextView) findViewById(R.id.title);
        submit = (Button) findViewById(R.id.submit);
        cancel = (Button) findViewById(R.id.cancel);
        review = (EditText) findViewById(R.id.review);
        driverImage = (ImageView) findViewById(R.id.driver_image);
        driverRating = (RatingBar) findViewById(R.id.rate_driver);
        TextView howWasRide = (TextView) findViewById(R.id.how_war_ride);
        TextView writeReview = (TextView) findViewById(R.id.write_review);

        mDialog.setCancelable(false);
        title.setTypeface(CommonMethods.headerFont(RateYourDriver.this));
        review.setTypeface(CommonMethods.headerFont(RateYourDriver.this));
        cancel.setTypeface(CommonMethods.headerFont(RateYourDriver.this));
        submit.setTypeface(CommonMethods.headerFont(RateYourDriver.this));
        howWasRide.setTypeface(CommonMethods.headerFont(RateYourDriver.this));
        writeReview.setTypeface(CommonMethods.headerFont(RateYourDriver.this));

        Glide.with(RateYourDriver.this).load(Constants.IMAGE_BASE_URL_DRIVER + mHelper.getPref("driver_image", "")).asBitmap().placeholder(R.drawable.default_user_pic)
                .centerCrop().into(new BitmapImageViewTarget(driverImage) {
            @Override
            protected void setResource(Bitmap resource) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(RateYourDriver.this.getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                driverImage.setImageDrawable(circularBitmapDrawable);
            }
        });


        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rating = driverRating.getRating();
                String reviewText = review.getText().toString().trim();
                if (rating == 0) {
                    CommonMethods.showAlert(RateYourDriver.this, getString(R.string.attention), getString(R.string.please_select_rating));
                }else if (reviewText.equals("") && reviewText.isEmpty()) {
                    sendFeedBack(appointmentId, reviewText);
                } else {
                    sendFeedBack(appointmentId, reviewText);
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHelper.savePref(Constants.APPOINTMENT_ID, "0");
                Intent intent = new Intent(RateYourDriver.this, LandingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                finish();
            }
        });

    }

    /**
     * method used to pay amount for booking
     *
     * @param appointmentId contain booking id
     * @param reviewText
     */
    private void sendFeedBack(final String appointmentId, final String reviewText) {
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
                            Intent intent = new Intent(RateYourDriver.this, LandingActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                            finish();
                        }else if(invalidResponse.equals("1")){
                            showAlert(RateYourDriver.this,getString(R.string.message),message);
                        } else {
                            CommonMethods.showAlert(RateYourDriver.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(RateYourDriver.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(RateYourDriver.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("app_appointment_id", appointmentId);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("rating", rating + "");
                    mParams.put("review", reviewText);

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(RateYourDriver.this).add(mRequest);
        } else {
            CommonMethods.showAlert(RateYourDriver.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }


    /**
     * method used to show alert dialog
     *
     * @param string get alert message
     */
    private void showAlert(Context context, String title, String string) {
        // TODO Auto-generated method stub
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title);
        alert.setMessage(string);
        alert.setCancelable(false);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                mHelper.clearAllPref();
                Intent intent = new Intent(RateYourDriver.this, Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }
}
