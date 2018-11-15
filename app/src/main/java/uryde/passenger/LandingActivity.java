package uryde.passenger;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import uryde.passenger.adapter.MenuListAdapter;
import uryde.passenger.cardPayment.StripePayment;
import uryde.passenger.fragments.RideHistory;
import uryde.passenger.fragments.Settings;
import uryde.passenger.fragments.BookYourRide;
import uryde.passenger.fragments.Profile;
import uryde.passenger.fragments.AboutUs;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PookieEventBus;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LandingActivity extends BaseActivity implements PookieEventBus.PookieEventReceiver {

    private TextView userName;
    private ImageView userImage;
    private PrefsHelper mHelper;
    public static TextView title;
    private RelativeLayout mPanel;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private static final String TAG = LandingActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);
        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        detector = new ConnectionDetector(LandingActivity.this);
        mHelper = new PrefsHelper(LandingActivity.this);
        List<String> menuTitleList = new ArrayList<>();

        menuTitleList.add(getString(R.string.book_ride));
        menuTitleList.add(getString(R.string.edit));
        menuTitleList.add(getString(R.string.payment));
        menuTitleList.add(getString(R.string.ride_history));
        menuTitleList.add(getString(R.string.setting));
        menuTitleList.add(getString(R.string.about_us));

        mDialog = new ProgressDialog(LandingActivity.this);
        mDialog.setCancelable(false);
        final MenuListAdapter mAdapter = new MenuListAdapter(LandingActivity.this, menuTitleList);

        title = findViewById(R.id.title);
        userName = findViewById(R.id.user_name);
        userName = findViewById(R.id.user_name);
        mPanel = findViewById(R.id.panel);
        userImage = findViewById(R.id.user_image);
        ImageView openMenu = findViewById(R.id.menu);
        ListView menuList = findViewById(R.id.menu_list);
        TextView logoutText = findViewById(R.id.logout_text);
        TextView versionCode = findViewById(R.id.version_code);
        LinearLayout logoutView = findViewById(R.id.logout_view);

        menuList.setAdapter(mAdapter);

        logoutText.setTypeface(CommonMethods.headerFont(this));
        versionCode.setTypeface(CommonMethods.headerFont(this));

        title.setText(getString(R.string.home));
        title.setVisibility(View.VISIBLE);
        goToMainFragment();

        userName.setTypeface(CommonMethods.headerFont(LandingActivity.this));

        openMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPanel.getVisibility() == View.VISIBLE) {
                    mPanel.setVisibility(View.GONE);
                } else {
                    mPanel.setVisibility(View.VISIBLE);
                }
            }
        });

        mPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPanel.getVisibility() == View.VISIBLE) {
                    mPanel.setVisibility(View.GONE);
                } else {
                    mPanel.setVisibility(View.VISIBLE);
                }
            }
        });

        menuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                mAdapter.setSelection(position);
                if (position == 0) {
                    if (Constants.exitValue == 5) {
                        title.setVisibility(View.VISIBLE);
                        mPanel.setVisibility(View.GONE);
                    } else {
                        displayView(0);
                        title.setVisibility(View.VISIBLE);
                        mPanel.setVisibility(View.GONE);
                    }
                } else if (position == 1) {
                    displayView(1);
                    title.setVisibility(View.VISIBLE);
                    mPanel.setVisibility(View.GONE);
                } else if (position == 2) {
                    displayView(2);
                    title.setVisibility(View.VISIBLE);
                    mPanel.setVisibility(View.GONE);
                } else if (position == 3) {
                    displayView(3);
                    title.setVisibility(View.VISIBLE);
                    mPanel.setVisibility(View.GONE);
                } else if (position == 4) {
                    displayView(4);
                    title.setVisibility(View.VISIBLE);
                    mPanel.setVisibility(View.GONE);
                } else if (position == 5) {
                    displayView(5);
                    title.setVisibility(View.VISIBLE);
                    mPanel.setVisibility(View.GONE);
                }
            }
        });

        logoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(LandingActivity.this);
                alert.setMessage(R.string.logout_text);
                alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPanel.setVisibility(View.GONE);
                        logoutFromApp();
                    }
                });
                alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alert.show();
            }
        });

        PookieEventBus.getInstance().subscribe(this, "update_now");

        if (!mHelper.getPref(Constants.APPOINTMENT_ID, "").equals("")) {
            if (!mHelper.getPref(Constants.APPOINTMENT_ID, "").equals("0")) {
                getHomeDetails();
            }
        } else {
            Log.d(TAG, mHelper.getPref(Constants.APPOINTMENT_ID, ""));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        userName.setText(String.format("%s %s", mHelper.getPref(Constants.USER_NAME, ""), mHelper.getPref(Constants.USER_LAST_NAME, "")));
        Log.d(TAG, Constants.IMAGE_BASE_URL + mHelper.getPref(Constants.USER_IMAGE, ""));
        if (mHelper.getPref(Constants.USER_IMAGE, "").equals("")) {
            userImage.setImageResource(R.drawable.default_user_pic);
        } else {
            Glide.with(LandingActivity.this).load(Constants.IMAGE_BASE_URL + mHelper.getPref(Constants.USER_IMAGE, "")).asBitmap().placeholder(R.drawable.default_user_pic)
                    .centerCrop().into(new BitmapImageViewTarget(userImage) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(LandingActivity.this.getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    userImage.setImageDrawable(circularBitmapDrawable);
                }
            });
        }
    }

    /**
     * method used to display a view
     *
     * @param position contain position of view
     */
    private void displayView(int position) {
        switch (position) {
            case 0:
                Constants.exitValue = 5;
                title.setText(getString(R.string.home));
                changeFragment(new BookYourRide());
                break;

            case 1:
                Constants.exitValue = 6;
                title.setText(getString(R.string.edit_profile));
                changeFragment(new Profile());
                break;
            case 2:
                Constants.exitValue = 7;
                title.setText(getString(R.string.payment));
                changeFragment(new StripePayment());
                break;
            case 3:
                Constants.exitValue = 7;
                title.setText(getString(R.string.ride_history));
                changeFragment(new RideHistory());
                break;
            case 4:
                Constants.exitValue = 10;
                title.setText(getString(R.string.setting));
                changeFragment(new Settings());
                break;
            case 5:
                Constants.exitValue = 8;
                title.setText(getString(R.string.about_us));
                changeFragment(new AboutUs());
                break;
            default:
                break;
        }
    }

    /**
     * method used to change fragment
     *
     * @param fragment contain fragment
     */
    public void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment, "fragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (mPanel.getVisibility() == View.VISIBLE) {
            mPanel.setVisibility(View.GONE);
        } else {
            if (Constants.exitValue == 9) {
                Constants.exitValue = 10;
                displayView(3);
            } else if (Constants.exitValue == 5) {
                finish();
                overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                super.onBackPressed();
            } else {
                goToMainFragment();
            }
        }
    }

    /**
     * method used to logout from app
     */
    private void logoutFromApp() {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.sign_out));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.LOGOUT, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    mDialog.dismiss();
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            mHelper.clearAllPref();
                            Intent intent = new Intent(LandingActivity.this, Splash.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
                        } else {
                            CommonMethods.showAlert(LandingActivity.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(LandingActivity.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(LandingActivity.this, getString(R.string.attention), getString(R.string.something_wrong));

                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));

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
            CommonMethods.showAlert(LandingActivity.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to get appointment status detail
     */
    private void getHomeDetails() {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GET_HOME_DETAILS, new Response.Listener<String>() {
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
                            String homeData = dataObject.getString("home_data");
                            if (homeData != null) {
                                JSONObject homeObject = new JSONObject(homeData);
                                String appointmentId = homeObject.getString("app_appointment_id");
                                String appointmentStatus = homeObject.getString("status");
                                String vehicle_type_city_id = homeObject.getString("vehicle_type_city_id");

                                mHelper.savePref(Constants.APPOINTMENT_ID, appointmentId);
                                mHelper.savePref(Constants.APPOINTMENT_STATUS, appointmentStatus);

                                switch (appointmentStatus) {
                                    case "2":
                                        mHelper.savePref(Constants.DRIVER_ON_THE_WAY, false);
                                        mHelper.savePref(Constants.DRIVER_ARRIVED, false);
                                        mHelper.savePref(Constants.BEGIN_JOURNEY, false);

                                        startActivity(new Intent(LandingActivity.this, Invoice.class));
                                        overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                                        break;
                                    default:
                                        startActivity(new Intent(LandingActivity.this, RideInfo.class).putExtra("city_id", vehicle_type_city_id));
                                        overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                                        finish();
                                        break;
                                }
                            }
                        } else if (invalidResponse.equals("1")) {
                            showAlert(LandingActivity.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(LandingActivity.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(LandingActivity.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("device_type", "1");

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
            CommonMethods.showAlert(LandingActivity.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

    /**
     * method used to go to home fragment
     */
    private void goToMainFragment() {
        Constants.exitValue = 5;
        displayView(0);
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
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHelper.clearAllPref();
                Intent intent = new Intent(LandingActivity.this, Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onEvent(String term, Object object) {
        userName.setText(mHelper.getPref(Constants.USER_NAME, "") + " " + mHelper.getPref(Constants.USER_LAST_NAME, ""));
        Log.d(TAG, Constants.IMAGE_BASE_URL + mHelper.getPref(Constants.USER_IMAGE, ""));
        if (mHelper.getPref(Constants.USER_IMAGE, "").equals("")) {
            userImage.setImageResource(R.drawable.default_user_pic);
        } else {
            Glide.with(LandingActivity.this).load(Constants.IMAGE_BASE_URL + mHelper.getPref(Constants.USER_IMAGE, "")).asBitmap().placeholder(R.drawable.default_user_pic)
                    .centerCrop().into(new BitmapImageViewTarget(userImage) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(LandingActivity.this.getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    userImage.setImageDrawable(circularBitmapDrawable);
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mHelper.savePref("taxi_view", 0);
        mHelper.savePref("restaurant_market", 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHelper.savePref("taxi_view", 0);
        mHelper.savePref("restaurant_market", 0);
    }

}
