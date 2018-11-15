package uryde.passenger.navigation;

import org.json.JSONException;
import org.json.JSONObject;

public interface VolleyListener {
    void onCompleteExecution(JSONObject jSONObject) throws JSONException;

    void onVolleyError(Boolean bool, String str);
}
