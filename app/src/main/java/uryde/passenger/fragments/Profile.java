package uryde.passenger.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.support.v4.app.FragmentTransaction;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import uryde.passenger.ChangePassword;
import uryde.passenger.LandingActivity;
import uryde.passenger.R;
import uryde.passenger.countryCodePicker.CountryPicker;
import uryde.passenger.countryCodePicker.CountryPickerListener;
import uryde.passenger.cropImage.CropImage;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PookieEventBus;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


public class Profile extends Fragment implements View.OnClickListener {

    private Uri file;
    private PrefsHelper mHelper;
    private Button updateProfile;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private ImageView userImage,editImage;
    private static final int REQUEST_CODE_GALLERY = 12;
    private static final int REQUEST_CODE_CROP_IMAGE = 13;
    private static final int REQUEST_CODE_TAKE_PICTURE = 11;
    public static final String TAG = Profile.class.getName();
    private EditText userName, userPhone, userEmail,countryCode;
    private String otpNumber = "", name = "", email = "", phone = "", imagePath = "",userCC="";
    private CountryPicker picker;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile, container, false);
        setHasOptionsMenu(true);
        init(view);
        return view;
    }

    /**
     * method used to initialization
     *
     * @param view contain view
     */
    private void init(View view) {
        mDialog = new ProgressDialog(getActivity());
        mHelper = new PrefsHelper(getActivity());
        detector = new ConnectionDetector(getActivity());
        userName =  view.findViewById(R.id.user_name);
        userEmail =  view.findViewById(R.id.user_email);
        userPhone =  view.findViewById(R.id.user_phone);
        userImage =  view.findViewById(R.id.user_image);
        editImage =  view.findViewById(R.id.edit_image);
        countryCode =  view.findViewById(R.id.country_code);
        updateProfile =  view.findViewById(R.id.update_profile);
        ImageView editProfile =  view.findViewById(R.id.edit_profile);
        TextView changePassword =  view.findViewById(R.id.change_password);

        mDialog.setCancelable(false);
        userName.setTypeface(CommonMethods.headerFont(getActivity()));
        userEmail.setTypeface(CommonMethods.headerFont(getActivity()));
        userPhone.setTypeface(CommonMethods.headerFont(getActivity()));
        countryCode.setTypeface(CommonMethods.headerFont(getActivity()));
        changePassword.setTypeface(CommonMethods.headerFont(getActivity()));

        userImage.setOnClickListener(this);
        editProfile.setOnClickListener(this);
        countryCode.setOnClickListener(this);
        updateProfile.setOnClickListener(this);
        changePassword.setOnClickListener(this);

        disableEditText();
        userName.setText(mHelper.getPref(Constants.USER_NAME, ""));
        userEmail.setText(mHelper.getPref(Constants.USER_EMAIL, ""));
        userPhone.setText(mHelper.getPref(Constants.USER_MOBILE, ""));
        countryCode.setText(mHelper.getPref(Constants.COUNTRY_CODE, ""));

        if (mHelper.getPref(Constants.USER_IMAGE, "").equals("")) {
            userImage.setImageResource(R.drawable.default_user_pic);
        } else {
            Glide.with(getActivity()).load(Constants.IMAGE_BASE_URL + mHelper.getPref(Constants.USER_IMAGE, "")).asBitmap().placeholder(R.drawable.default_user_pic)
                    .centerCrop().into(new BitmapImageViewTarget(userImage) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(getActivity().getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    userImage.setImageDrawable(circularBitmapDrawable);
                }
            });
        }
    }

    /**
     * method used to getUserProfile
     */
    private void getUserProfile() {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.loading));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GET_PROFILE, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.d(TAG, response);
                    mDialog.dismiss();
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String profile = dataObject.getString("profile");
                            JSONObject profileData = new JSONObject(profile);
                            String name = profileData.getString("first_name");
                            String lastName = profileData.getString("last_name");
                            String email = profileData.getString("email");
                            String mobile = profileData.getString("mobile");
                            String profilePic = profileData.getString("profile_pic");

                            userName.setText(name);
                            userEmail.setText(email);
                            userPhone.setText(mobile);

                            if (mHelper.getPref(Constants.USER_IMAGE, "").equals("")) {
                                userImage.setImageResource(R.drawable.default_user_pic);
                            } else {
                                Glide.with(getActivity()).load(Constants.IMAGE_BASE_URL + profilePic).asBitmap().placeholder(R.drawable.default_user_pic)
                                        .centerCrop().into(new BitmapImageViewTarget(userImage) {
                                    @Override
                                    protected void setResource(Bitmap resource) {
                                        RoundedBitmapDrawable circularBitmapDrawable =
                                                RoundedBitmapDrawableFactory.create(getActivity().getResources(), resource);
                                        circularBitmapDrawable.setCircular(true);
                                        userImage.setImageDrawable(circularBitmapDrawable);
                                    }
                                });
                            }
                            mHelper.savePref(Constants.USER_NAME, name);
                            mHelper.savePref(Constants.USER_EMAIL, email);
                            mHelper.savePref(Constants.USER_MOBILE, mobile);
                            mHelper.savePref(Constants.USER_IMAGE, profilePic);

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
                    Log.d(TAG, error.toString());
                    mDialog.dismiss();
                    CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));

                }
            }) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
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

            Volley.newRequestQueue(getActivity()).add(mRequest);
        } else {
            CommonMethods.showAlert(getActivity(), getString(R.string.no_internet), getString(R.string.something_wrong));
        }
    }

    /**
     * method used to enable the edit text
     */
    private void enableEditText() {
        editImage.setVisibility(View.VISIBLE);
        updateProfile.setVisibility(View.VISIBLE);
        userName.setFocusableInTouchMode(true);
        userName.setFocusable(true);

        userPhone.setFocusableInTouchMode(true);
        userPhone.setFocusable(true);

        userEmail.setFocusableInTouchMode(true);
        userEmail.setFocusable(true);

    }

    /**
     * method used to disable the edit text
     */
    private void disableEditText() {
        editImage.setVisibility(View.GONE);
        updateProfile.setVisibility(View.GONE);
        userName.setFocusableInTouchMode(false);
        userName.setFocusable(false);

        userPhone.setFocusableInTouchMode(false);
        userPhone.setFocusable(false);

        userEmail.setFocusableInTouchMode(false);
        userEmail.setFocusable(false);

        countryCode.setFocusableInTouchMode(false);
        countryCode.setFocusable(false);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.user_image) {
            if (updateProfile.getVisibility() == View.VISIBLE) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setMessage(R.string.select_photo_from);
                alertDialog.setPositiveButton(R.string.gallery, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        takePictureFromGallery();
                    }
                });
                alertDialog.setNegativeButton(R.string.camera, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.PERMISSIONS_REQUEST_CAMERA);
                        } else {
                            takePicture();
                        }
                    }
                });
                alertDialog.show();
            }
        }
        if (view.getId() == R.id.change_password) {
            startActivity(new Intent(getActivity(), ChangePassword.class));
            getActivity().overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        }
        if (view.getId() == R.id.edit_profile) {
            enableEditText();
        }
        if (view.getId() == R.id.country_code) {
            if(updateProfile.getVisibility()==View.VISIBLE){
                openCountryCodePicker();
            }
        }

        if (view.getId() == R.id.update_profile) {
            name = userName.getText().toString().trim();
            email = userEmail.getText().toString().trim();
            phone = userPhone.getText().toString().trim();
            userCC = countryCode.getText().toString().trim();

            if (name.isEmpty()) {
                CommonMethods.showSnackBar(userName, getString(R.string.valid_name));
            } else if (email.isEmpty()) {
                CommonMethods.showSnackBar(userPhone, getString(R.string.email_blank));
            } else if (!CommonMethods.isEmailValid(email)) {
                CommonMethods.showSnackBar(userPhone, getString(R.string.valid_email));
            } else if (phone.isEmpty()) {
                CommonMethods.showSnackBar(userPhone, getString(R.string.valid_mobile));
            }  else if (phone.length() < 10) {
                CommonMethods.showSnackBar(userPhone, getString(R.string.valid_mobile_lenght));
            } else {
                mDialog.setMessage(getString(R.string.updating));
                mDialog.show();
                new UpdateProfile().execute(imagePath);
            }
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
                countryCode.setText(dialCode);
                picker.dismiss();
            }
        });
        picker.show(getFragmentManager(), "COUNTRY_CODE_PICKER");
    }

    /**
     * method used to take picture from camera
     */
    private void takePicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, Constants.PERMISSIONS_REQUEST_PHONE_STORAGE_CAMERA);

        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            ContentValues values = new ContentValues(1);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            file = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        }
    }

    /**
     * method used to take picture from gallery
     */
    private void takePictureFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
            } else {
                Toast.makeText(getActivity(), "Go to setting", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == Constants.PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture();
            } else {
                Toast.makeText(getActivity(), "Go to setting", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == Constants.PERMISSIONS_REQUEST_PHONE_STORAGE_GALLERY) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePictureFromGallery();
            } else {
                Toast.makeText(getActivity(), "Go to setting", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_GALLERY) {
                if (data != null) {

                    File file = new File(CommonMethods.getRealPathFromURI(getActivity(), data.getData()));
                    startCropImage(file);

                } else {
                    Log.d(TAG, "you don`t pic any image");
                }
            } else if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
                try {
                    startCropImage(new File(CommonMethods.getRealPathFromURI(getActivity(), file)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == REQUEST_CODE_CROP_IMAGE) {
                imagePath = data.getStringExtra(CropImage.IMAGE_PATH);
                if (imagePath == null) {
                    return;
                }
//                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//                userImage.setImageBitmap(bitmap);

                Glide.with(getActivity()).load(imagePath).asBitmap().placeholder(R.drawable.default_user_pic)
                        .centerCrop().into(new BitmapImageViewTarget(userImage) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circularBitmapDrawable =
                                RoundedBitmapDrawableFactory.create(getActivity().getResources(), resource);
                        circularBitmapDrawable.setCircular(true);
                        userImage.setImageDrawable(circularBitmapDrawable);
                    }
                });
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
    private void startCropImage(File mFileTemp) {
        Intent intent = new Intent(getActivity(), CropImage.class);
        intent.putExtra(CropImage.IMAGE_PATH, mFileTemp.getPath());
        intent.putExtra(CropImage.SCALE, true);
        intent.putExtra(CropImage.ASPECT_X, 4);
        intent.putExtra(CropImage.ASPECT_Y, 4);
        startActivityForResult(intent, REQUEST_CODE_CROP_IMAGE);
    }

    /**
     * Register user asyncTask used to register a user in app
     */
    @SuppressLint("StaticFieldLeak")
    private class UpdateProfile extends AsyncTask<String, String, String> {

        String response = "";

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, strings[0] + " -profile_pic- " + phone + " -OTP- " + otpNumber + " -device_type- " + "1" + " -email- " + email + " -name- " + name + " -language- " +
                    mHelper.getPref(Constants.APP_LANGUAGE, "") + " -session_token- " + mHelper.getPref(Constants.SESSION_TOKEN, ""));
            if (strings[0].equals("")) {
                try {
                    HttpResponse<JsonNode> request = Unirest.post(Constants.BASE_URL + Constants.UPDATE_PROFILE)
                            .field("mobile", phone)
                            .field("device_type", "1")
                            .field("email", email)
                            .field("name", name)
                            .field("country_code", userCC)
                            .field("language", mHelper.getPref(Constants.APP_LANGUAGE, ""))
                            .field("profile_pic", strings[0])
                            .field("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""))
                            .asJson();
                    response = request.getBody().toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                try {
                    HttpResponse<JsonNode> request = Unirest.post(Constants.BASE_URL + Constants.UPDATE_PROFILE)
                            .field("mobile", phone)
                            .field("device_type", "1")
                            .field("email", email)
                            .field("name", name)
                            .field("country_code", userCC)
                            .field("language", mHelper.getPref(Constants.APP_LANGUAGE, ""))
                            .field("profile_pic", new File(strings[0]))
                            .field("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""))
                            .asJson();
                    response = request.getBody().toString();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return response;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mDialog.dismiss();
            Log.d(TAG, s);
            try {
                JSONObject mObject = new JSONObject(s);
                int status = mObject.getInt(Constants.RESPONSE_STATUS);
                String message = mObject.getString(Constants.RESPONSE_MSG);
                if (status == 1) {
                    String data = mObject.getString(Constants.RESPONSE_DATA);
                    JSONObject dataObject = new JSONObject(data);
                    String profile = dataObject.getString("profile");
                    JSONObject pObject = new JSONObject(profile);

                    mHelper.savePref(Constants.USER_NAME, pObject.getString("name"));
                    mHelper.savePref(Constants.USER_EMAIL, pObject.getString("email"));
                    mHelper.savePref(Constants.USER_MOBILE, pObject.getString("mobile"));
                    mHelper.savePref(Constants.USER_IMAGE, pObject.getString("profile_pic"));
                    mHelper.savePref(Constants.COUNTRY_CODE, pObject.getString("country_code"));

                    disableEditText();
                    PookieEventBus.getInstance().publish("update_now", "update");
                    gotoHomeFragment();
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                } else {
                    CommonMethods.showAlert(getActivity(), getString(R.string.message), message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
            }
        }
    }
    private void gotoHomeFragment() {
        LandingActivity.title.setText(getString(R.string.home));
        Constants.exitValue = 5;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, new BookYourRide());
        ft.commit();
    }
}
