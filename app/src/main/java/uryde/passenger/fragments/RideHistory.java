package uryde.passenger.fragments;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import uryde.passenger.R;
import uryde.passenger.Splash;
import uryde.passenger.adapter.HistoryAdapter;
import uryde.passenger.model.HistoryModel;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RideHistory extends Fragment {

    private int count = 1;
    private PrefsHelper mHelper;
    private int year, month, date;
    private ProgressDialog mDialog;
    private HistoryAdapter mAdapter;
    private List<HistoryModel> mList;
    private ConnectionDetector mDetector;
    private int listViewIndex, listViewTop;
    private PullToRefreshListView historyList;
    private String from_date = "", to_date = "";
    private TextView emptyView, toDate, fromDate;
    private static final String TAG = RideHistory.class.getName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.histoty_layout, container, false);

        init(view);

        return view;
    }

    /**
     * method used to initialization
     *
     * @param view contain view
     */
    private void init(View view) {
        Calendar cal = Calendar.getInstance();
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        date = cal.get(Calendar.DATE);

        mList = new ArrayList<>();
        mHelper = new PrefsHelper(getActivity());
        mDialog = new ProgressDialog(getActivity());
        mDetector = new ConnectionDetector(getActivity());

        mDialog.setCancelable(false);

        toDate = (TextView) view.findViewById(R.id.to_date);
        fromDate = (TextView) view.findViewById(R.id.from_date);
        emptyView = (TextView) view.findViewById(R.id.empty_view);
        TextView toText = (TextView) view.findViewById(R.id.to_text);
        TextView fromText = (TextView) view.findViewById(R.id.from_text);
        ImageView getHistoryData = (ImageView) view.findViewById(R.id.get_history);
        historyList = (PullToRefreshListView) view.findViewById(R.id.history_list);

        toText.setTypeface(CommonMethods.headerFont(getActivity()));
        fromText.setTypeface(CommonMethods.headerFont(getActivity()));
        toDate.setTypeface(CommonMethods.headerFont(getActivity()));
        fromDate.setTypeface(CommonMethods.headerFont(getActivity()));

        Date date = new Date();
        String nowAsString = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).format(date);
        toDate.setText(nowAsString);
        fromDate.setText(nowAsString);

        to_date = nowAsString;
        from_date = nowAsString;

        historyList.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                loadNextPage();
                listViewIndex = historyList.getRefreshableView().getFirstVisiblePosition();
                View v = historyList.getRefreshableView().getChildAt(0);
                listViewTop = (v == null) ? 0 : v.getTop();
            }
        });

        fromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFromDate(fromDate);
            }
        });
        toDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getToDate(toDate);
            }
        });

        getHistory(fromDate.getText().toString().trim(), toDate.getText().toString().trim(), count);

        getHistoryData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fromDate.getText().toString().trim().equals("") || toDate.getText().toString().trim().equals("")) {
                    CommonMethods.showSnackBar(fromDate, getString(R.string.from_to_blank));
                } else if (fromDate.getText().toString().trim().equals("")) {
                    CommonMethods.showSnackBar(toDate, getString(R.string.from_blank));
                } else if (toDate.getText().toString().trim().equals("")) {
                    CommonMethods.showSnackBar(toDate, getString(R.string.to_blank));
                } else {
                    getHistory(fromDate.getText().toString().trim(), toDate.getText().toString().trim(), count);
                }
            }
        });
    }

    /**
     * method used to load more pages.
     */
    private void loadNextPage() {
        ++count;
        Log.d(TAG, count + "");
        getRideHistory(fromDate.getText().toString(), toDate.getText().toString(), count);
    }

    /**
     * method used to get ride history after clear previous data
     *
     * @param fromDate contain from date
     * @param toDate   contain to date
     * @param count    contain page count
     */
    private void getHistory(String fromDate, String toDate, int count) {
        mList.clear();
        getRideHistory(fromDate, toDate, count);
    }

    /**
     * method used to show date picker and get date
     *
     * @param text and set to text view
     */
    private void getFromDate(final TextView text) {
        DatePickerDialog dpd = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // TODO Auto-generated method stub

                        Date now = new Date();
                        Date normal = null;
                        String nowAsString = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).format(now);
                        Log.d(TAG, nowAsString);
                        try {
                            normal = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).parse(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                            String normalAsString = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).format(normal);
                            Log.d(TAG, normalAsString);

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        try {
                            Date todate = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).parse(to_date);

                            if (normal.equals(now)) {
                                String fromDate = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                                from_date = fromDate;
                                text.setText(fromDate);
                            } else if (normal.after(now)) {
                                Toast.makeText(getActivity(), getString(R.string.valid_date_in_history), Toast.LENGTH_SHORT).show();
                            } else if (normal.after(todate)) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.change_to_date), Toast.LENGTH_LONG).show();
                            } else {
                                String fromDate = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                                from_date = fromDate;
                                text.setText(fromDate);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                year,
                month,
                date);
        dpd.show();
    }

    /**
     * method used to show date picker and get date
     *
     * @param text and set to text view
     */
    private void getToDate(final TextView text) {
        DatePickerDialog dpd = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {
                        // TODO Auto-generated method stub

                        Date now = new Date();
                        Date normal = null;
                        String nowAsString = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).format(now);
                        Log.d(TAG, nowAsString);
                        try {
                            normal = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).parse(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                            String normalAsString = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).format(normal);
                            Log.d(TAG, normalAsString);

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (normal.equals(now)) {
                            to_date = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                            text.setText(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                        } else if (normal.after(now)) {
                            Toast.makeText(getActivity(), getString(R.string.valid_date_in_history), Toast.LENGTH_SHORT).show();
                        } else if (normal.before(now)) {
                            if (!fromDate.getText().toString().trim().isEmpty()) {
                                try {
                                    Date from = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.US).parse(fromDate.getText().toString().trim());
                                    if (normal.equals(from)) {
                                        to_date = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                                        text.setText(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                                    } else if (normal.after(from)) {
                                        to_date = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                                        text.setText(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                                    } else {
                                        Toast.makeText(getActivity(), getString(R.string.valid_date_in_history), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                to_date = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                                text.setText(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                            }
                        } else {
                            to_date = year + "-" + (monthOfYear + 1) + "-" +dayOfMonth;
                            text.setText(year + "-" + (monthOfYear + 1) + "-" +dayOfMonth);
                        }
                    }
                },
                year,
                month,
                date);
        dpd.show();
    }

    /**
     * method used to get ride history
     *
     * @param fromDate contain from date
     * @param toDate   contain to date
     */
    private void getRideHistory(final String fromDate, final String toDate, final int pageCount) {
        if (mDetector.isConnectingToInternet()) {
            mDialog.setMessage(getString(R.string.please_wait));
            mDialog.show();
            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.HISTORY + "/page/" + pageCount, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        String invalidResponse = mObject.getString("response_invalid");
                        if (status == 1) {
                            emptyView.setVisibility(View.GONE);
                            historyList.setVisibility(View.VISIBLE);
                            if (mObject.has(Constants.RESPONSE_DATA)) {
                                String data = mObject.getString(Constants.RESPONSE_DATA);
                                JSONObject dataObject = new JSONObject(data);
                                String appointment = dataObject.getString("appointment");
                                JSONArray mArray = new JSONArray(appointment);
                                for (int i = 0; i < mArray.length(); i++) {
                                    JSONObject appObject = mArray.getJSONObject(i);
                                    HistoryModel model = new HistoryModel();
                                    model.setApp_appointment_id(appObject.getString("app_appointment_id"));
                                    model.setComplete_dt(appObject.getString("appointment_date"));
                                    model.setDrop_address(appObject.getString("drop_address"));
                                    model.setPick_address(appObject.getString("pick_address"));
                                    model.setTotal_amount(appObject.getString("total_amount"));
                                    model.setStatus(appObject.getString("status"));
                                    model.setPromoCodeId(appObject.getString("promocode_id"));

                                    mList.add(model);

                                }
                                mAdapter = new HistoryAdapter(getActivity(), mList);
                                historyList.setAdapter(mAdapter);
                                historyList.onRefreshComplete();
                                historyList.getRefreshableView().setSelectionFromTop(listViewIndex, listViewTop);
                            } else {
                                count = 1;
                                historyList.stopRefreshing();
                                historyList.setEnabled(false);
                                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                            }
                        } else if (invalidResponse.equals("1")) {
                            showAlert(getActivity(), getString(R.string.message), message);
                        } else {
                            emptyView.setText(message);
                            historyList.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        emptyView.setText(getString(R.string.something_wrong));
                        historyList.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
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
                    mParams.put("device_type", "1");
                    mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
                    mParams.put("language", mHelper.getPref(Constants.APP_LANGUAGE, ""));
                    mParams.put("start_dt", fromDate);
                    mParams.put("end_dt", toDate);

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

    /**
     * method used to show alert dialog
     *
     * @param string get alert message
     */
    private void showAlert(Context context, String title, String string) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(title);
        alert.setMessage(string);
        alert.setCancelable(false);
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHelper.clearAllPref();
                Intent intent = new Intent(getActivity(), Splash.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finish();
                getActivity().overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            }
        });
        alert.show();
    }
}
