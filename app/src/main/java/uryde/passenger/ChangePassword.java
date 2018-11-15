package uryde.passenger;

import android.app.ProgressDialog;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import uryde.passenger.fragments.BookYourRide;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChangePassword extends Fragment {

    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private EditText oldPassword, newPassword, confirmPassword;
    private static final String TAG = ChangePassword.class.getName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_change_password, container, false);
        init(view);
        return view;

    }

    /**
     * method used to initialization
     */
    private void init(View view) {
        mHelper = new PrefsHelper(getActivity());
        detector = new ConnectionDetector(getActivity());
        mDialog = new ProgressDialog(getActivity());
        oldPassword =  view.findViewById(R.id.old_password);
        newPassword =  view.findViewById(R.id.new_password);
        Button update =  view.findViewById(R.id.change_password);
        confirmPassword =  view.findViewById(R.id.confirm_password);

        update.setTypeface(CommonMethods.headerFont(getActivity()));
        oldPassword.setTypeface(CommonMethods.headerFont(getActivity()));
        newPassword.setTypeface(CommonMethods.headerFont(getActivity()));
        confirmPassword.setTypeface(CommonMethods.headerFont(getActivity()));

        mDialog.setCancelable(false);

        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String old = oldPassword.getText().toString();
                String nPassword = newPassword.getText().toString();
                String cPassword = confirmPassword.getText().toString();

                if (old.equals("")) {
                    CommonMethods.showSnackBar(oldPassword, getString(R.string.blank_ol));
                } else if (!old.equals(mHelper.getPref(Constants.USER_PASSWORD, ""))) {
                    CommonMethods.showSnackBar(oldPassword, getString(R.string.incorrect_old));
                } else if (nPassword.equals("")) {
                    CommonMethods.showSnackBar(newPassword, getString(R.string.blank_new_password));
                } else if (nPassword.length() < 8) {
                    CommonMethods.showSnackBar(newPassword, getString(R.string.new_password_lenght));
                } else if (!cPassword.equals(nPassword)) {
                    CommonMethods.showSnackBar(confirmPassword, getString(R.string.password_not_match));
                } else if (nPassword.equals(mHelper.getPref(Constants.USER_PASSWORD, ""))) {
                    CommonMethods.showSnackBar(newPassword, getString(R.string.should_be_diffrent_password));
                } else {
                    changePassword(old, nPassword);
                }
            }
        });
    }

    /**
     * method used to change user password
     *
     * @param old       contain old password
     * @param nPassword contain new password
     */
    private void changePassword(final String old, final String nPassword) {
        if (detector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.updating));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.UPDATE_PASSWORD, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            oldPassword.setText("");
                            newPassword.setText("");
                            confirmPassword.setText("");
                            mHelper.savePref(Constants.USER_PASSWORD,nPassword);
                            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                            gotoHomeFragment();
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
                protected Map<String, String> getParams() {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("newpassword", nPassword);
                    mParams.put("oldpassword", old);
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
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

    private void gotoHomeFragment() {
        LandingActivity.title.setText(getString(R.string.home));
        Constants.exitValue = 5;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, new BookYourRide());
        ft.commit();
    }
}
