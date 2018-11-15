package uryde.passenger.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import uryde.passenger.R;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AboutUs extends Fragment {

    private PrefsHelper mHelper;
    private WebView contentView;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private static final String TAG = AboutUs.class.getName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.about_us, container, false);

        init(view);

        return view;
    }

    /**
     * method used to initialization
     */
    private void init(View view) {
        mDialog = new ProgressDialog(getActivity());
        mHelper = new PrefsHelper(getActivity());
        detector = new ConnectionDetector(getActivity());

        mDialog.setCancelable(false);
        contentView = view.findViewById(R.id.content_view);
        TextView supportText = view.findViewById(R.id.terms_condition_text);
        supportText.setTypeface(CommonMethods.headerFont(getActivity()));

        getSupportContent();

    }

    /**
     * method used to get support content
     */
    private void getSupportContent() {
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

                          /*  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                supportText.setText(Html.fromHtml(pages_desc, Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                supportText.setText(Html.fromHtml(pages_desc));
                            }*/
                            contentView.loadData(pages_desc,"text/html", "UTF-8");
                        } else {
                            CommonMethods.showAlert(getActivity(), getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("identifier", "aboutus");

                    Log.d(TAG, mParams.toString());
                    return mParams;
                }
            };
            mRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Constants.MY_SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            Volley.newRequestQueue(getActivity()).add(mRequest);
        } else {
            CommonMethods.showAlert(getActivity(), getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }
}
