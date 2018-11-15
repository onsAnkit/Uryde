package uryde.passenger;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


public class Uryde extends Application {

    public static RequestQueue requestQueue;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        requestQueue = Volley.newRequestQueue(this);
    }
}
