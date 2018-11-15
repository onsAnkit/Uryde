package uryde.passenger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import uryde.passenger.howItWorks.HowItWorks;
import io.fabric.sdk.android.Fabric;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import java.util.Locale;

public class Splash extends AppCompatActivity {

    private String deviceID;
    private boolean isLogin;
    private PrefsHelper mHelper;
    private String language = "en";
    private static final String TAG = Splash.class.getName();
    private static final int PERMISSION_READ_PHONE_STATE = 11;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
//        forceCrash();
        init();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_PHONE_STATE);
        } else {
            goToNext();
        }
    }

    /**
     * method used to initialization
     */
    private void init() {
        mHelper = new PrefsHelper(Splash.this);
        isLogin = mHelper.getPref("user_login", false);
    }

    /**
     * method used to show dialog for choose language options
     */
    private void showLanguageDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Choose Language")
                .setView(R.layout.langauage_dialog)
                .create();

        dialog.show();

        final TextView languageEn = dialog.findViewById(R.id.language_en);
        final TextView languageFr = dialog.findViewById(R.id.language_fr);
        final TextView languageAr = dialog.findViewById(R.id.language_ar);

        languageEn.setTypeface(CommonMethods.headerFont(Splash.this));
        languageFr.setTypeface(CommonMethods.headerFont(Splash.this));
        languageAr.setTypeface(CommonMethods.headerFont(Splash.this));


        languageEn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                changeLanguage("en");
            }
        });

        languageFr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                changeLanguage("fr");
            }
        });

        languageAr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                changeLanguage("ar");
            }
        });
    }




    /**
     * method used to goToNew Activity
     */
    private void goToNext() {
        deviceID = getDeviceId();
        mHelper.savePref(Constants.DEVICE_ID, deviceID);
        mHelper.savePref(Constants.APP_LANGUAGE, language);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                changeLanguage(language);
            }
        }, 3000);
    }

    /**
     * method used to get device id
     *
     * @return device id
     */
    @SuppressLint("HardwareIds")
    private String getDeviceId() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        } else {
            return manager.getDeviceId();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goToNext();
        } else {
            finish();
        }
    }

    /**
     * method used to change language of app
     *
     * @param changedLanguage contain language code
     */
    void changeLanguage(String changedLanguage) {
        Locale locale = new Locale(changedLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        language = changedLanguage;
        mHelper.savePref(Constants.DEVICE_ID, deviceID);
        mHelper.savePref(Constants.APP_LANGUAGE, language);

        if (isLogin) {
            startActivity(new Intent(Splash.this, LandingActivity.class));
            Splash.this.finish();
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        } else {
            startActivity(new Intent(Splash.this, HowItWorks.class));
            Splash.this.finish();
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
       /* if (checkPlayServices()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }*/
    }

    /**
     * method used to check play services
     *
     * @return true or false
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
