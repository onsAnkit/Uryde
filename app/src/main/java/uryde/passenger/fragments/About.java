package uryde.passenger.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
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

import java.util.HashMap;
import java.util.Map;

import uryde.passenger.R;
import uryde.passenger.Splash;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

public class About extends AppCompatActivity {

    private WebView contentView;
    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private static final String TAG = About.class.getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        mDialog = new ProgressDialog(About.this);
        mHelper = new PrefsHelper(About.this);
        detector = new ConnectionDetector(About.this);

        mDialog.setCancelable(false);
        ImageView back = findViewById(R.id.back);
        TextView title = findViewById(R.id.title);
        TextView supportText = findViewById(R.id.support_text);
        contentView = findViewById(R.id.content_view);

        title.setTypeface(CommonMethods.headerFont(About.this));
        supportText.setTypeface(CommonMethods.headerFont(About.this));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String text = getIntent().getStringExtra("text");

        switch (text) {
            case "help":
                title.setText(getString(R.string.help));
                getSupportContent("help");
                break;
            case "support":
                title.setText(getString(R.string.support));
                getSupportContent("support");
                break;
            case "privacy-policy":
                title.setText(getString(R.string.privacy_policy));
                getSupportContent("privacy-passenger");
                break;
            case "terms-condition-passenger":
                title.setText(getString(R.string.terms_contions));
                getSupportContent("terms-condition-passenger");
                break;
        }
    }

    /**
     * method used to get support content
     *
     * @param text contain of page name
     */
    private void getSupportContent(final String text) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.PAGES, new Response.Listener<String>() {
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
                            String page = dataObject.getString("page");
                            JSONObject pageObject = new JSONObject(page);
                            String pages_desc = pageObject.getString("pages_desc");

                            contentView.loadData(pages_desc,"text/html", "UTF-8");

                        } else if (invalidResponse.equals("1")) {
                            showAlert(About.this, getString(R.string.message), message);
                        } else {
                            CommonMethods.showAlert(About.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(About.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(About.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("identifier", text);

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(About.this).add(mRequest);
        } else {
            CommonMethods.showAlert(About.this, getString(R.string.no_internet), getString(R.string.internet_toast));
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
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHelper.clearAllPref();
                Intent intent = new Intent(About.this, Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                About.this.finish();
                About.this.overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }
}
