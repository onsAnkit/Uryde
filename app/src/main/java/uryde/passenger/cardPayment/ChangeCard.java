package uryde.passenger.cardPayment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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


public class ChangeCard extends AppCompatActivity implements View.OnClickListener {

    private ListView listCard;
    private PrefsHelper mHelper;
    private ProgressDialog mDialog;
    TextView txtErrorMessage, title;
    private CardShowAdapter adapter;
    public static Context mContext;
    private ConnectionDetector detector;
    private ArrayList<PaymentItem> paymentList;
    private String TAG = ChangeCard.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_card);

        init();
    }

    /**
     * init method used to initialization
     */
    private void init() {
        mContext=ChangeCard.this;
        mHelper = new PrefsHelper(ChangeCard.this);
        detector = new ConnectionDetector(ChangeCard.this);
        mDialog = new ProgressDialog(ChangeCard.this);

        paymentList = new ArrayList<>();
        mDialog.setCancelable(false);
        listCard = findViewById(R.id.card_listing);

        title = findViewById(R.id.title);
        ImageView back = findViewById(R.id.back);
        Button btnAddCard = findViewById(R.id.btn_add_card);
        txtErrorMessage = findViewById(R.id.error_message);

        btnAddCard.setOnClickListener(this);
        adapter = new CardShowAdapter(ChangeCard.this, paymentList);
        listCard.setAdapter(adapter);

        title.setTypeface(CommonMethods.headerFont(ChangeCard.this));

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        listCard.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.d(TAG, "LENGTH" + paymentList.size());
                getCard(position);
            }
        });
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
                    CommonMethods.showAlert(ChangeCard.this, getString(R.string.attention), getString(R.string.something_wrong));
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

        Volley.newRequestQueue(ChangeCard.this).add(mRequest);

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_add_card) {
            Intent intent = new Intent(ChangeCard.this, AddStripeCardActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.anim_two, R.anim.anim_one);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (detector.isConnectingToInternet()) {
            Log.d("value_stripe", mHelper.getPref(Constants.STRIPE_ID, "null"));

            if (mHelper.getPref(Constants.STRIPE_ID).equals("null")) {
                listCard.setVisibility(View.GONE);
                txtErrorMessage.setVisibility(View.VISIBLE);
                txtErrorMessage.setText(getString(R.string.no_cards_avilable));
            } else {
                getCardDetails();
            }
        } else {
            listCard.setVisibility(View.GONE);
            txtErrorMessage.setVisibility(View.VISIBLE);
        }
    }

    public void getCard(int position) {
        PaymentItem item = (PaymentItem) adapter.getItem(position);
        String cardId = item.getCardId();
        Intent in = new Intent();
        in.putExtra(Constants.CARD_ID, cardId);
        setResult(RESULT_OK, in);
        finish();
    }

    /**
     * method used to show pop for deleting card
     * @param position contain card position
     */
    public void deleteCard(final int position) {
        if (detector.isConnectingToInternet()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(ChangeCard.this);
            alert.setTitle(getString(R.string.message));
            alert.setMessage(getString(R.string.delete_card_message));
            alert.setCancelable(false);
            alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteStripeCard(position);
                }
            });
            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            alert.show();
        }
    }

    /**
     * method used to delete stripe card
     */
    private void deleteStripeCard(final int position) {
        mDialog.setMessage("Deleting Card....");
        mDialog.show();
        StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.REMOVE_CARDS, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, response);
                mDialog.dismiss();
                try {
                    int responseStatus = new JSONObject(response).getInt("response_status");
                    if (responseStatus == 1) {
                        String responseMessage = new JSONObject(response).getString("response_msg");
                        Toast.makeText(ChangeCard.this, responseMessage, Toast.LENGTH_SHORT).show();
                        String cardDetails = new JSONObject(new JSONObject(new JSONObject(response).getString("response_data")).getString("customer_cards")).getString("cards");
                        if (cardDetails.equals("") || cardDetails.equals("[]")) {
                            listCard.setVisibility(View.GONE);
                            txtErrorMessage.setVisibility(View.VISIBLE);
                            txtErrorMessage.setText(R.string.no_cards_avilable);
                        } else {
                            listCard.setVisibility(View.VISIBLE);
                            txtErrorMessage.setVisibility(View.GONE);
                            paymentList.clear();
                            for (int i = 0; i < new JSONArray(cardDetails).length(); i++) {
                                JSONObject itemValue = new JSONArray(cardDetails).getJSONObject(i);

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
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        String responseMessage = new JSONObject(response).getString("response_msg");
                        CommonMethods.showAlertAddActivity(ChangeCard.this, getString(R.string.message), responseMessage);
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                    CommonMethods.showAlert(ChangeCard.this, getString(R.string.attention), getString(R.string.something_wrong));
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
                mDialog.dismiss();
                CommonMethods.showAlert(ChangeCard.this, getString(R.string.attention), getString(R.string.something_wrong));
            }
        }) {
            @Override
            protected Map<String, String> getParams()  {
                HashMap<String, String> mParams = new HashMap<>();
                mParams.put("device_type", "1");
                mParams.put("stripe_card_id", paymentList.get(position).getCardId());
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

        Volley.newRequestQueue(ChangeCard.this).add(mRequest);
    }

}
