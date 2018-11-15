package uryde.passenger.cardPayment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import uryde.passenger.R;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

public class StripePayment extends Fragment implements View.OnClickListener {

    TextView txtErrorMessage;
    private ListView listCard;
    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    private CardShowAdapter adapter;
    private ConnectionDetector detector;
    private ArrayList<PaymentItem> paymentList;
    private String TAG = StripePayment.class.getName();


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stripe_payment, container, false);
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
        mHelper = new PrefsHelper(getActivity());
        detector = new ConnectionDetector(getActivity());
        mDialog = new ProgressDialog(getActivity());

        paymentList = new ArrayList<>();
        mDialog.setCancelable(false);
        listCard =  view.findViewById(R.id.card_listing);

        txtErrorMessage =  view.findViewById(R.id.error_message);
        Button btnAddCard = view.findViewById(R.id.btn_add_card);
        btnAddCard.setOnClickListener(this);
        adapter = new CardShowAdapter(getActivity(), paymentList);
        listCard.setAdapter(adapter);
    }

    /**
     * method used to get card details from server
     */
    private void getCardDetails() {
        mDialog.setMessage(getString(R.string.getting_card_info));
        mDialog.show();
        StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.GET_DETAILS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, response);
                mDialog.dismiss();
                try {
                    JSONObject mObject = new JSONObject(response);
                    int status = mObject.getInt(Constants.RESPONSE_STATUS);
                    String message = mObject.getString(Constants.RESPONSE_MSG);
                    if (status == 1) {
                        if (mObject.has(Constants.RESPONSE_DATA)) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String customerCard = dataObject.getString("customer_cards");
                            JSONObject ccObject = new JSONObject(customerCard);
                            String cards = ccObject.getString("cards");
                            JSONArray mArray = new JSONArray(cards);
                            listCard.setVisibility(View.VISIBLE);
                            txtErrorMessage.setVisibility(View.GONE);
                            paymentList.clear();

                            for (int i = 0; i < mArray.length(); i++) {
                                JSONObject itemValue = mArray.getJSONObject(i);

                                PaymentItem paymentItem = new PaymentItem();
                                String lastFourDigit = itemValue.getString("last4");
                                String expiryYear = itemValue.getString("exp_year");
                                String expiryMonth = itemValue.getString("exp_month");
                                String imageType = itemValue.getString("type");
                                String cardId = itemValue.getString("id");

                                paymentItem.setExpiryMonth(expiryMonth);
                                paymentItem.setExpiryYear(expiryYear);
                                paymentItem.setLastFourDigitCardNumber(lastFourDigit);
                                paymentItem.setImageType(Utility.setCreditCardLogo(imageType));
                                paymentItem.setCardId(cardId);
                                paymentList.add(paymentItem);

                            }
                        } else {
                            listCard.setVisibility(View.GONE);
                            txtErrorMessage.setVisibility(View.VISIBLE);
                            txtErrorMessage.setText(message);
                        }
                    } else {
                        listCard.setVisibility(View.GONE);
                        txtErrorMessage.setVisibility(View.VISIBLE);
                        txtErrorMessage.setText(message);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    CommonMethods.showAlert(getActivity(), getString(R.string.attention), getString(R.string.something_wrong));
                }
                adapter.notifyDataSetChanged();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
                mDialog.dismiss();
                listCard.setVisibility(View.GONE);
                txtErrorMessage.setVisibility(View.VISIBLE);
                txtErrorMessage.setText(getString(R.string.something_wrong));
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                HashMap<String, String> mParams = new HashMap<>();
                mParams.put("device_type", "1");
                mParams.put("session_token", mHelper.getPref(Constants.SESSION_TOKEN, ""));
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

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_add_card) {
            getActivity().startActivity(new Intent(getActivity(), AddStripeCardActivity.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (detector.isConnectingToInternet()) {

            Log.d("value_stripe", mHelper.getPref(Constants.STRIPE_ID, "null"));

            if (mHelper.getPref(Constants.STRIPE_ID).equals("null")) {
                listCard.setVisibility(View.GONE);
                txtErrorMessage.setVisibility(View.VISIBLE);
                txtErrorMessage.setText(R.string.no_cards_avilable);
            } else {
                getCardDetails();
            }
        } else {
            listCard.setVisibility(View.GONE);
            txtErrorMessage.setVisibility(View.VISIBLE);
        }
    }
}
