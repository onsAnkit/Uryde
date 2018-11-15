package uryde.passenger.navigation;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import uryde.passenger.Uryde;

public class VolleyRequest {
    private Map<String, String> params;
    private String url;

    public VolleyRequest(String url, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        this.url = url;
        this.params = params;
    }

    public void POST(VolleyListener customVolleyListener) {
        request(1, customVolleyListener);
    }

    public void GET(VolleyListener customVolleyListener) {
        StringBuilder sbParams = new StringBuilder();
        int i = 0;
        for (String key : this.params.keySet()) {
            if (i != 0) {
                sbParams.append("&");
            }
            try {
                sbParams.append(key).append("=").append(URLEncoder.encode((String) this.params.get(key), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            i++;
        }
        this.url += "&" + sbParams.toString();
        request(0, customVolleyListener);
    }

    public void request(int method, final VolleyListener customVolleyListener) {
        StringRequest stringRequest = new StringRequest(method, this.url, new Listener<String>() {
            public void onResponse(String response) {

                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (customVolleyListener != null) {
                        customVolleyListener.onCompleteExecution(jsonObject);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (customVolleyListener != null) {
                        customVolleyListener.onVolleyError(Boolean.TRUE, "Internal error occurred");
                    }
                }
            }
        }, new ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                customVolleyListener.onVolleyError(Boolean.FALSE, "Network error occurred.");
            }
        }) {
            protected Map<String, String> getParams() throws AuthFailureError {
                return VolleyRequest.this.params;
            }
        };
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(25000, 1, 1.0f));
        Uryde.requestQueue.add(stringRequest);
    }
}
