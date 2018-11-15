package uryde.passenger;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TermsAndConditions extends AppCompatActivity {

    private WebView contentView;
    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private TextView termsAndConditionText;
    private static final String TAG = TermsAndConditions.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);

        init();
    }

    /**
     * init method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(TermsAndConditions.this);
        mDialog = new ProgressDialog(TermsAndConditions.this);
        detector = new ConnectionDetector(TermsAndConditions.this);

        mDialog.setCancelable(false);
        ImageView back =  findViewById(R.id.back);
        contentView = findViewById(R.id.content_view);
        termsAndConditionText = findViewById(R.id.terms_condition_text);
        termsAndConditionText.setTypeface(CommonMethods.headerFont(TermsAndConditions.this));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getTermsAndConditions();
    }

    /**
     * method used to get Terms and conditions
     */
    private void getTermsAndConditions() {
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
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String page = dataObject.getString("page");
                            JSONObject pageObject = new JSONObject(page);
                            String pages_desc = pageObject.getString("pages_desc");

                           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                termsAndConditionText.setText(Html.fromHtml(pages_desc, Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                termsAndConditionText.setText(Html.fromHtml(pages_desc));
                            }*/
                            contentView.loadData(pages_desc,"text/html", "UTF-8");

                        } else {
                            CommonMethods.showAlert(TermsAndConditions.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(TermsAndConditions.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(TermsAndConditions.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("identifier", "terms-condition-passenger");

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
            CommonMethods.showAlert(TermsAndConditions.this, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }
}
