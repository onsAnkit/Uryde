package uryde.passenger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import uryde.passenger.countryCodePicker.CountryPicker;
import uryde.passenger.countryCodePicker.CountryPickerListener;
import uryde.passenger.cropImage.CropImage;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.LocationUtils;
import uryde.passenger.util.PrefsHelper;

public class Register extends AppCompatActivity implements View.OnClickListener {

    private File mFileTemp;
    private TextView ccPicker;
    private PrefsHelper mHelper;
    private ImageView userImage;
    private CountryPicker picker;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private CheckBox termsAndConditionCheck;
    private String conturyCode = "", imagePath = "";
    private static final int REQUEST_CODE_GALLERY = 12;
    private static final int REQUEST_CODE_CROP_IMAGE = 13;
    private static final int REQUEST_CODE_TAKE_PICTURE = 11;
    private static final String TAG = Register.class.getName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 113;
    private EditText userName, userEmail, userPassword, confirmPassword, userMobile;
    private Uri file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        init();
    }

    /**
     * method used to initialization
     */
    private void init() {
        LocationUtils locationUtils = new LocationUtils(this);
        locationUtils.showSettingDialog();

        mHelper = new PrefsHelper(Register.this);
        mDialog = new ProgressDialog(Register.this);
        detector = new ConnectionDetector(Register.this);
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mFileTemp = new File(Environment.getExternalStorageDirectory(), Constants.TEMP_PHOTO_FILE_NAME);
        } else {
            mFileTemp = new File(getFilesDir(), Constants.TEMP_PHOTO_FILE_NAME);
        }


        userName =  findViewById(R.id.user_name);
        ccPicker =  findViewById(R.id.cc_picker);
        ImageView back =  findViewById(R.id.back);
        TextView title =  findViewById(R.id.title);
        userEmail =  findViewById(R.id.user_email);
        userImage =  findViewById(R.id.user_image);
        userMobile =  findViewById(R.id.user_phone);
        userPassword =  findViewById(R.id.user_password);
        confirmPassword =  findViewById(R.id.confirm_password);
        Button registerButton =  findViewById(R.id.register_button);
        TextView alreadyAccount =  findViewById(R.id.already_account);
        termsAndConditionCheck =  findViewById(R.id.terms_condition_check);
        TextView termsAndConditionText =  findViewById(R.id.terms_condition_text);

        mDialog.setCancelable(false);

        title.setTypeface(CommonMethods.headerFont(Register.this));
        ccPicker.setTypeface(CommonMethods.headerFont(Register.this));
        userName.setTypeface(CommonMethods.headerFont(Register.this));
        userEmail.setTypeface(CommonMethods.headerFont(Register.this));
        userMobile.setTypeface(CommonMethods.headerFont(Register.this));
        userPassword.setTypeface(CommonMethods.headerFont(Register.this));
        registerButton.setTypeface(CommonMethods.headerFont(Register.this));
        confirmPassword.setTypeface(CommonMethods.headerFont(Register.this));
        termsAndConditionText.setTypeface(CommonMethods.headerFont(Register.this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d(TAG, getResources().getConfiguration().getLocales().get(0).getCountry());
        } else {
            Log.d(TAG, getResources().getConfiguration().locale.getCountry());
        }

        back.setOnClickListener(this);
        ccPicker.setOnClickListener(this);
        userImage.setOnClickListener(this);
        userImage.setOnClickListener(this);
        alreadyAccount.setOnClickListener(this);
        registerButton.setOnClickListener(this);
        termsAndConditionText.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.back) {
            finish();
        }
        if (view.getId() == R.id.already_account) {
            finish();
        }
        if (view.getId() == R.id.cc_picker) {
            openCountryCodePicker();
        }

        if (view.getId() == R.id.user_image) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(Register.this);
            alertDialog.setMessage(R.string.select_photo_from);
            alertDialog.setPositiveButton(R.string.gallery, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    takePictureFromGallery();
                }
            });
            alertDialog.setNegativeButton(R.string.camera, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_CAMERA);
                    } else {
                        takePicture();
                    }
                }
            });
            alertDialog.show();
        }
        if (view.getId() == R.id.register_button) {
            String name = userName.getText().toString().trim();
            conturyCode = ccPicker.getText().toString().trim();
            String email = userEmail.getText().toString().trim();
            String mobile = userMobile.getText().toString().trim();
            String password = userPassword.getText().toString().trim();
            String cPassword = confirmPassword.getText().toString().trim();

            if (name.isEmpty()) {
                userName.requestFocus();
                CommonMethods.showSnackBar(userName, getString(R.string.valid_name));
            } else if (email.isEmpty()) {
                userEmail.requestFocus();
                CommonMethods.showSnackBar(userEmail, getString(R.string.email_blank));
            } else if (!CommonMethods.isEmailValid(email)) {
                userEmail.requestFocus();
                CommonMethods.showSnackBar(userEmail, getString(R.string.valid_email));
            } else if (mobile.isEmpty()) {
                userMobile.requestFocus();
                CommonMethods.showSnackBar(userMobile, getString(R.string.valid_mobile));
            } else if (mobile.length() < 10) {
                userMobile.requestFocus();
                CommonMethods.showSnackBar(userMobile, getString(R.string.valid_mobile_lenght));
            } else if (password.isEmpty()) {
                userPassword.requestFocus();
                CommonMethods.showSnackBar(userPassword, getString(R.string.password_blank));
            } else if (password.length() < 8) {
                userPassword.requestFocus();
                CommonMethods.showSnackBar(userPassword, getString(R.string.valid_password_lenght));
            } else if (!cPassword.equals(password)) {
                confirmPassword.requestFocus();
                CommonMethods.showSnackBar(confirmPassword, getString(R.string.password_not_match));
            } else if (!termsAndConditionCheck.isChecked()) {
                CommonMethods.showSnackBar(confirmPassword, getString(R.string.aggree_terms));
            } else {
                CommonMethods.hideKeyboard(Register.this);
                generateOTP(name, email, mobile, password, imagePath, conturyCode);
            }
        }

        if (view.getId() == R.id.terms_condition_text) {
            startActivity(new Intent(Register.this, TermsAndConditions.class));
            overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
        }
    }

    /**
     * method used to open country code picker
     */
    private void openCountryCodePicker() {
        picker = CountryPicker.newInstance("Select Country");
        picker.setListener(new CountryPickerListener() {
            @Override
            public void onSelectCountry(String name, String code, String dialCode) {
                ccPicker.setText(dialCode);
                picker.dismiss();
            }
        });
        picker.show(getSupportFragmentManager(), "COUNTRY_CODE_PICKER");
    }

    /**
     * method used to generate OTP
     *
     * @param name        contain user name
     * @param email       contain user email
     * @param mobile      contain user mobile
     * @param password    contain user password
     * @param imagePath   contain user image path
     * @param countryCode contain user country code
     */
    private void generateOTP(final String name, final String email, final String mobile, final String password, final String imagePath, final String countryCode) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.generate_otp));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GENERATE_OTP, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            goToOTPPage(name, email, mobile, password, imagePath, countryCode);
                        } else {
                            CommonMethods.showAlert(Register.this, getString(R.string.message), message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        CommonMethods.showAlert(Register.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(Register.this, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("mobile", mobile);
                    mParams.put("country_code", countryCode);
                    mParams.put("email", email);
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
            Toast.makeText(this, getString(R.string.internet_toast), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * method used to go to OTP page
     *
     * @param name        contain user name
     * @param email       contain user email
     * @param mobile      contain user mobile
     * @param password    contain user password
     * @param imagePath   contain user image path
     * @param countryCode
     */
    private void goToOTPPage(String name, String email, String mobile, String password, String imagePath, String countryCode) {
        startActivity(new Intent(Register.this, EnterOTP.class).putExtra(Constants.USER_NAME, name).putExtra(Constants.USER_EMAIL, email).putExtra(Constants.COUNTRY_CODE, countryCode)
                .putExtra(Constants.USER_MOBILE, mobile).putExtra(Constants.USER_PASSWORD, password).putExtra(Constants.USER_IMAGE, imagePath));
    }

    /**
     * method used to take picture from camera
     */
    private void takePicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, Constants.PERMISSIONS_REQUEST_PHONE_STORAGE_CAMERA);

        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            file = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        }
    }

    /**
     * method used to take picture from gallery
     */
    private void takePictureFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_PHONE_STORAGE_GALLERY);

        } else {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.PERMISSIONS_REQUEST_PHONE_STORAGE_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            }
        }
        if (requestCode == Constants.PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            }
        }
        if (requestCode == Constants.PERMISSIONS_REQUEST_PHONE_STORAGE_GALLERY) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePictureFromGallery();
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                if (data != null) {
                    try {
                        File file = new File(CommonMethods.getRealPathFromURI(Register.this, data.getData()));
                        startCropImage(file);

                    } catch (Exception e) {
                        Log.d(TAG, "you don`t pic any image");
                    }
                } else {
                    Log.d(TAG, "you don`t pic any image");
                }
            } else if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
                startCropImage(new File(CommonMethods.getRealPathFromURI(Register.this, file)));
            } else if (requestCode == REQUEST_CODE_CROP_IMAGE) {
                imagePath = data.getStringExtra(CropImage.IMAGE_PATH);

                if (imagePath == null) {
                    return;
                }

                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                userImage.setImageBitmap(bitmap);
            }
        }
    }

    /**
     * method used to copy input and output stream
     */
    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    /**
     * method used to crop the image
     */
    private void startCropImage(File file) {
        Intent intent = new Intent(Register.this, CropImage.class);
        intent.putExtra(CropImage.IMAGE_PATH, file.getPath());
        intent.putExtra(CropImage.SCALE, true);
        intent.putExtra(CropImage.ASPECT_X, 4);
        intent.putExtra(CropImage.ASPECT_Y, 4);
        startActivityForResult(intent, REQUEST_CODE_CROP_IMAGE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (mHelper.getPref(Constants.DEVICE_TOKEN, "").equals("")) {
            if (checkPlayServices()) {
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
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

