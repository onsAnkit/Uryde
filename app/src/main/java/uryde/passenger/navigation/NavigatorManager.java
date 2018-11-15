package uryde.passenger.navigation;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NavigatorManager {
    private LatLng destinationLatLong;
    private LatLng sourceLatLng;

    public interface DirectionListener {
        void onGetDirection(List<LatLng> list);
    }

    public NavigatorManager(LatLng sourceLatLng, LatLng destinationLatLong) {
        this.sourceLatLng = sourceLatLng;
        this.destinationLatLong = destinationLatLong;
    }

    public void getDirectionLatLngList(final DirectionListener directionListener) {
        Map<String, String> params = new HashMap<>();
        params.put("origin", this.sourceLatLng.latitude + "," + this.sourceLatLng.longitude);
        params.put("destination", this.destinationLatLong.latitude + "," + this.destinationLatLong.longitude);
        params.put("key","AIzaSyBsX9y8QMr_28CJDU5nqmvEPRd_gc2aHKo");
        new VolleyRequest("https://maps.googleapis.com/maps/api/directions/json?" +
                "mode=driving&" +
                "transit_routing_preference=less_driving", params).GET(new VolleyListener() {
            public void onCompleteExecution(JSONObject jsonObject) {
                try {
                    NavigatorManager.this.decodeJSON(jsonObject, directionListener);
                } catch (JSONException e) {
                    e.printStackTrace();
                    onVolleyError(Boolean.TRUE, "");
                }
            }

            public void onVolleyError(Boolean isJsonError, String message) {
                if (directionListener != null) {
                    directionListener.onGetDirection(null);
                }
            }
        });
    }

    private void decodeJSON(JSONObject jsonObject, DirectionListener directionListener) throws JSONException {
        List<LatLng> latLngList = null;
        if (jsonObject.getString("status").equalsIgnoreCase("OK") && jsonObject.has("routes")) {
            JSONArray routeArray = jsonObject.getJSONArray("routes");
            if (routeArray.length() > 0) {
                String encodedPolyLine = routeArray.getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                latLngList = decodePolyLine(encodedPolyLine);
            }
        }
        if (directionListener != null) {
            directionListener.onGetDirection(latLngList);
        }
    }

    public static List<LatLng> decodePolyLine(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;
        while (index < len) {
            int index2;
            int shift = 0;
            int result = 0;
            while (true) {
                index2 = index + 1;
                int b = encoded.charAt(index) - 63;
                result |= (b & 31) << shift;
                shift += 5;
                if (b < 32) {
                    break;
                }
                index = index2;
            }
            lat += (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            shift = 0;
            result = 0;
            index = index2;
            while (true) {
                index2 = index + 1;
                int b = encoded.charAt(index) - 63;
                result |= (b & 31) << shift;
                shift += 5;
                if (b < 32) {
                    break;
                }
                index = index2;
            }
            lng += (result & 1) != 0 ? ~(result >> 1) : result >> 1;
            poly.add(new LatLng(((double) lat) / 100000.0d, ((double) lng) / 100000.0d));
            index = index2;
        }
        return poly;
    }
}