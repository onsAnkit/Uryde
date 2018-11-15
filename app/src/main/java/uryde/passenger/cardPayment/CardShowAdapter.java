package uryde.passenger.cardPayment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import uryde.passenger.R;


public class CardShowAdapter extends BaseAdapter {

    private Context activity;
    private ArrayList<PaymentItem> paymentCardList;


    public CardShowAdapter(Context context, ArrayList<PaymentItem> paymentList) {
        activity = context;
        paymentCardList = new ArrayList<>();
        paymentCardList = paymentList;
    }

    @Override
    public int getCount() {
        return paymentCardList.size();
    }

    @Override
    public Object getItem(int position) {
        return paymentCardList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder = new Holder();

        if (convertView == null) {
            LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflator.inflate(R.layout.row_stripe_card_details, null);
            holder.txtCardNumber = convertView.findViewById(R.id.txt_card_number);
            holder.cardImage = convertView.findViewById(R.id.card_image);
            holder.delete_card = convertView.findViewById(R.id.delete_card);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.txtCardNumber.setText("**** **** **** " + paymentCardList.get(position).getLastFourDigitCardNumber());
        holder.cardImage.setImageResource(paymentCardList.get(position).getImageType());

        holder.delete_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ChangeCard) activity).deleteCard(position);

            }
        });

        holder.txtCardNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity == ChangeCard.mContext) {
                    ((ChangeCard) activity).getCard(position);
                }
            }
        });

        return convertView;
    }

    class Holder {
        TextView txtCardNumber;
        ImageView cardImage, delete_card;
    }
}
