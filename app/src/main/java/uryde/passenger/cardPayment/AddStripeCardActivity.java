package uryde.passenger.cardPayment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.devmarvel.creditcardentry.library.CreditCard;
import com.devmarvel.creditcardentry.library.CreditCardForm;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import uryde.passenger.R;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.ConnectionDetector;
import uryde.passenger.util.Constants;
import uryde.passenger.util.PrefsHelper;

/**
 * Created by admin on 11/3/2016.
 */

public class AddStripeCardActivity extends AppCompatActivity {

    private Context context;
    private PrefsHelper mHelper;
    private CreditCardForm form;
    private CreditCard cardForm;
    private String tokenGenerate;
    private ProgressDialog mDialog;
    private ConnectionDetector detector;
    private String TAG = AddStripeCardActivity.class.getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_stripe_card);

        initView();
    }

    /**
     * method used to initialization
     */
    private void initView() {
        context = AddStripeCardActivity.this;
        mHelper = new PrefsHelper(context);
        mDialog = new ProgressDialog(context);
        mDialog.setCancelable(false);
        ImageView btnBack = findViewById(R.id.back);
        TextView skip = findViewById(R.id.skip);
        TextView title = findViewById(R.id.title);
        detector = new ConnectionDetector(context);

        Button btnAdd = findViewById(R.id.btn_card_add);
        form = findViewById(R.id.card_layout);

        skip.setVisibility(View.GONE);
        title.setTypeface(CommonMethods.headerFont(AddStripeCardActivity.this));
        btnAdd.setTypeface(CommonMethods.headerFont(AddStripeCardActivity.this));

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardForm = form.getCreditCard();
                Card card = new Card(cardForm.getCardNumber(), cardForm.getExpMonth(), cardForm.getExpYear(), cardForm.getSecurityCode());

                mDialog.setMessage(getString(R.string.please_wait));
                mDialog.show();
                if (detector.isConnectingToInternet()) {
                    if (card.validateCard()) {

                        new Stripe(AddStripeCardActivity.this).createToken(card, Constants.PUBLISHABLE_KEY, new TokenCallback() {
                            @Override
                            public void onError(Exception error) {
                                mDialog.dismiss();
                                CommonMethods.showAlert(context, getString(R.string.attention), getString(R.string.invalid_card));
                            }

                            @Override
                            public void onSuccess(Token token) {
                                tokenGenerate = token.getId();
                                addCard(tokenGenerate);

                            }
                        });
                    } else {
                        mDialog.dismiss();
                        CommonMethods.showAlert(context, getString(R.string.attention), getString(R.string.wrong_card));
                    }

                } else {
                    mDialog.dismiss();
                    CommonMethods.showAlert(context, getString(R.string.no_internet), getString(R.string.internet_toast));
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    /**
     * method used to add card to user account
     *
     * @param tokenGenerate contain card token
     */
    private void addCard(final String tokenGenerate) {
        if (detector.isConnectingToInternet()) {

            StringRequest mRequest = new StringRequest(Request.Method.POST, Constants.BASE_URL + Constants.ADD_CARD, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    mDialog.dismiss();
                    Log.d(TAG, response);
                    try {
                        JSONObject mObject = new JSONObject(response);
                        int status = mObject.getInt(Constants.RESPONSE_STATUS);
                        String message = mObject.getString(Constants.RESPONSE_MSG);
                        if (status == 1) {
                            String data = mObject.getString(Constants.RESPONSE_DATA);
                            JSONObject dataObject = new JSONObject(data);
                            String customerCard = dataObject.getString("customer_cards");
                            JSONObject ccObject = new JSONObject(customerCard);
                            String stripeId = ccObject.getString("stripe_id");
                            mHelper.savePref(Constants.STRIPE_ID, stripeId);
                            CommonMethods.showAlertAddActivity(AddStripeCardActivity.this, getString(R.string.message), message);
                        } else {
                            CommonMethods.showAlertAddActivity(AddStripeCardActivity.this, getString(R.string.attention), message);
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                        CommonMethods.showAlertAddActivity(AddStripeCardActivity.this, getString(R.string.attention), getString(R.string.something_wrong));
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    mDialog.dismiss();
                    Log.d(TAG, error.toString());
                    CommonMethods.showAlert(context, getString(R.string.attention), getString(R.string.something_wrong));
                }
            }) {
                @Override
                protected Map<String, String> getParams()  {
                    HashMap<String, String> mParams = new HashMap<>();
                    mParams.put("device_type", "1");
                    mParams.put("stripe_token", tokenGenerate);
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

            Volley.newRequestQueue(this).add(mRequest);
        } else {
            mDialog.dismiss();
            CommonMethods.showAlert(context, getString(R.string.no_internet), getString(R.string.internet_toast));
        }
    }

}
