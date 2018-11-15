package uryde.passenger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPassword extends AppCompatActivity implements View.OnClickListener {

    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    public static final String TAG = ForgotPassword.class.getName();
    private String countryCode = "", customerId = "", userOtp = "", name = "";

    private ImageView rightImage;
    private TextView forgotText;
    private EditText userName, userPassword, confirmPassword, enterCode;
    private LinearLayout emailFound, enterEmailView, enterCodeView, enterPasswordView, emailNotFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        init();
    }

    /**
     * init method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(ForgotPassword.this);
        mDialog = new ProgressDialog(ForgotPassword.this);
        detector = new ConnectionDetector(ForgotPassword.this);

        Button next = findViewById(R.id.next);
        Button verify = findViewById(R.id.verify);
        Button resetPassword = findViewById(R.id.reset_password);

        userName = findViewById(R.id.enter_email);
        enterCode = findViewById(R.id.enter_code);
        userPassword = findViewById(R.id.user_password);
        confirmPassword = findViewById(R.id.confirm_password);
        TextView otpNotReceive = findViewById(R.id.otp_not_receive);

        forgotText = findViewById(R.id.forgot_text);
        rightImage = findViewById(R.id.right_image);
        emailFound = findViewById(R.id.email_found);
        emailNotFound = findViewById(R.id.email_not_found);
        enterCodeView = findViewById(R.id.enter_code_view);
        enterEmailView = findViewById(R.id.enter_email_view);
        enterPasswordView = findViewById(R.id.enter_password_view);

        ImageView back = findViewById(R.id.back);
        mDialog.setCancelable(false);

        next.setTypeface(CommonMethods.headerFont(ForgotPassword.this));
        verify.setTypeface(CommonMethods.headerFont(ForgotPassword.this));
        userName.setTypeface(CommonMethods.headerFont(ForgotPassword.this));
        enterCode.setTypeface(CommonMethods.headerFont(ForgotPassword.this));
        userPassword.setTypeface(CommonMethods.headerFont(ForgotPassword.this));
        resetPassword.setTypeface(CommonMethods.headerFont(ForgotPassword.this));
        confirmPassword.setTypeface(CommonMethods.headerFont(ForgotPassword.this));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (emailNotFound.getVisibility() == View.VISIBLE) {
                    emailNotFound.setVisibility(View.GONE);
                    emailFound.setVisibility(View.VISIBLE);
                    emailFound.animate().alpha(0.0f).setDuration(0);
                    emailFound.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            forgotText.setText(getString(R.string.enter_mobile_number_text));
                            rightImage.setVisibility(View.GONE);
                        }
                    });
                } else if (enterCodeView.getVisibility() == View.VISIBLE) {
                    enterCodeView.setVisibility(View.GONE);
                    enterEmailView.setVisibility(View.VISIBLE);
                    enterEmailView.animate().alpha(0.0f).setDuration(0);
                    enterEmailView.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            forgotText.setText(getString(R.string.enter_mobile_number_text));
                            rightImage.setVisibility(View.GONE);
                        }
                    });
                } else if (enterPasswordView.getVisibility() == View.VISIBLE) {
                    enterPasswordView.setVisibility(View.GONE);
                    enterCodeView.setVisibility(View.VISIBLE);
                    enterCodeView.animate().alpha(0.0f).setDuration(0);
                    enterCodeView.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            forgotText.setText(getString(R.string.otp_sent));
                            rightImage.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    finish();
                }
            }
        });

        next.setOnClickListener(this);
        verify.setOnClickListener(this);
        resetPassword.setOnClickListener(this);
        otpNotReceive.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.next) {
            name = userName.getText().toString().trim();
            if (name.isEmpty()) {
                CommonMethods.showSnackBar(userName, getString(R.string.email_blank));
            } else if (!CommonMethods.isEmailValid(name)) {
                CommonMethods.showSnackBar(userName, getString(R.string.valid_email));
            } else {
                forgotPassword();
            }
        } else if (view.getId() == R.id.otp_not_receive) {
            forgotPassword();
        } else if (view.getId() == R.id.verify) {
            userOtp = enterCode.getText().toString().trim();
            if (userOtp.isEmpty()) {
                CommonMethods.showSnackBar(enterCode, getString(R.string.blank_otp));
            } else if (userOtp.length() < 6) {
                CommonMethods.showSnackBar(enterCode, getString(R.string.valid_otp));
            } else {
                verifyOTP(userOtp);
            }
        } else if (view.getId() == R.id.reset_password) {
            String newPassword = userPassword.getText().toString().trim();
            String cPassword = confirmPassword.getText().toString().trim();
            if (newPassword.isEmpty()) {
                CommonMethods.showSnackBar(userPassword, getString(R.string.new_passwod_blank));
            } else if (newPassword.length() < 8) {
                CommonMethods.showSnackBar(userPassword, getString(R.string.new_password_lenght));
            } else if (!cPassword.equals(newPassword)) {
                CommonMethods.showSnackBar(confirmPassword, getString(R.string.password_not_match));
            } else {
                changePassword(newPassword, cPassword);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (emailNotFound.getVisibility() == View.VISIBLE) {
            emailNotFound.setVisibility(View.GONE);
            emailFound.setVisibility(View.VISIBLE);
            emailFound.animate().alpha(0.0f).setDuration(0);
            emailFound.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    forgotText.setText(getString(R.string.enter_mobile_number_text));
                    rightImage.setVisibility(View.GONE);
                }
            });
        } else if (enterCodeView.getVisibility() == View.VISIBLE) {
            enterCodeView.setVisibility(View.GONE);
            enterEmailView.setVisibility(View.VISIBLE);
            enterEmailView.animate().alpha(0.0f).setDuration(0);
            enterEmailView.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    forgotText.setText(getString(R.string.enter_mobile_number_text));
                    rightImage.setVisibility(View.GONE);
                }
            });
        } else if (enterPasswordView.getVisibility() == View.VISIBLE) {
            enterPasswordView.setVisibility(View.GONE);
            enterCodeView.setVisibility(View.VISIBLE);
            enterCodeView.animate().alpha(0.0f).setDuration(0);
            enterCodeView.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    forgotText.setText(getString(R.string.otp_sent));
                    rightImage.setVisibility(View.VISIBLE);
                    rightImage.setImageResource(R.drawable.right);
                }
            });
        } else {
            super.onBackPressed();
            finish();
        }
    }

    /**
     * method used for forgot password
     */
    private void forgotPassword() {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.FORGOT_PASSWORD, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        if (status == 1) {
                            enterCodeView.animate().alpha(0.0f).setDuration(0);
                            enterEmailView.animate().alpha(0.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    enterEmailView.setVisibility(View.GONE);
                                    enterCodeView.setVisibility(View.VISIBLE);
                                    enterCodeView.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME);
                                    forgotText.setText(getString(R.string.otp_sent));
                                    rightImage.setVisibility(View.VISIBLE);
                                    rightImage.setImageResource(R.drawable.right);
                                }
                            });
                        } else {
                            emailNotFound.animate().alpha(0.0f).setDuration(0);
                            emailFound.animate().alpha(0.0f).setDuration(Constants.ANIMATION_TIME).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    emailFound.setVisibility(View.GONE);
                                    emailNotFound.setVisibility(View.VISIBLE);
                                    emailNotFound.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME);
                                    forgotText.setText(getString(R.string.email_not_found_text));
                                    rightImage.setVisibility(View.VISIBLE);
                                    rightImage.setImageResource(R.drawable.close);
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(ForgotPassword.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(ForgotPassword.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("mobile", name);

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
            CommonMethods.showAlert(ForgotPassword.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used for verify user OTP
     *
     * @param userOtp contain otp entered by user
     */
    private void verifyOTP(final String userOtp) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.CHECK_OTP, new Response.Listener<String>() {
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
                            JSONObject mData = new JSONObject(data);
                            String profile = mData.getString("profile");
                            JSONObject pData = new JSONObject(profile);
                            customerId = pData.getString("customer_id");
                            enterPasswordView.animate().alpha(0.0f).setDuration(0);
                            enterCodeView.animate().alpha(0.0f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    enterCodeView.setVisibility(View.GONE);
                                    enterPasswordView.setVisibility(View.VISIBLE);
                                    enterPasswordView.animate().alpha(1.0f).setDuration(Constants.ANIMATION_TIME);
                                    forgotText.setText(getString(R.string.choose_new_password));
                                    rightImage.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            CommonMethods.showSnackBar(enterCode, message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(ForgotPassword.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(ForgotPassword.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("mobile", name);
                    mParams.put("otp", userOtp);
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
            CommonMethods.showAlert(ForgotPassword.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to change user password
     */
    private void changePassword(final String newPassword, final String cPassword) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.RESET_PASSWORD, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            goToLogin();
                        } else {
                            CommonMethods.showSnackBar(confirmPassword, message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(ForgotPassword.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(ForgotPassword.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("confirm_password", cPassword);
                    mParams.put("otp", userOtp);
                    mParams.put("password", newPassword);
                    mParams.put("customer_id", customerId);

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
            CommonMethods.showAlert(ForgotPassword.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to go to login page
     */
    private void goToLogin() {
        Intent intent = new Intent(ForgotPassword.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
