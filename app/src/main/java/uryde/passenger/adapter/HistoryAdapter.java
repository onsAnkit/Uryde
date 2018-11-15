package uryde.passenger.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import uryde.passenger.R;
import uryde.passenger.RideHistoryDetail;
import uryde.passenger.model.HistoryModel;
import uryde.passenger.util.CommonMethods;
import uryde.passenger.util.Constants;

import java.util.List;

/**
 * Created by admin on 30-09-2016.
 */

public class HistoryAdapter extends BaseAdapter {

    private Context context;
    private List<HistoryModel> dataList;
    private LayoutInflater inflater;

    public HistoryAdapter(Context context, List<HistoryModel> dataList) {
        this.context = context;
        this.dataList = dataList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.history_item, viewGroup, false);
            holder = new ViewHolder();
            holder.date =  view.findViewById(R.id.date);
            holder.amount = view.findViewById(R.id.amount);
            holder.cancel = view.findViewById(R.id.cancel);
            holder.bookingId = view.findViewById(R.id.booking_id);
            holder.rideDetail =view.findViewById(R.id.ride_detail);
            holder.picUpAddress = view.findViewById(R.id.pick_up_address);
            holder.parentLayout = view.findViewById(R.id.parent_layout);
            holder.dropOffAddress = view.findViewById(R.id.drop_off_address);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.date.setTypeface(CommonMethods.headerFont(context));
        holder.amount.setTypeface(CommonMethods.headerFont(context));
        holder.bookingId.setTypeface(CommonMethods.headerFont(context));
        holder.picUpAddress.setTypeface(CommonMethods.headerFont(context));
        holder.dropOffAddress.setTypeface(CommonMethods.headerFont(context));

        holder.picUpAddress.setText(dataList.get(position).getPick_address());
        holder.dropOffAddress.setText(dataList.get(position).getDrop_address());
        holder.amount.setText(Constants.CURRENCY_SIGN + dataList.get(position).getTotal_amount());
        holder.date.setText(CommonMethods.getConvertedDate(dataList.get(position).getComplete_dt()));
        holder.bookingId.setText(context.getString(R.string.booking_id_2131) + " " + dataList.get(position).getApp_appointment_id());

        switch (dataList.get(position).getStatus()) {
            case "8":
                holder.cancel.setVisibility(View.VISIBLE);
                holder.cancel.setText(context.getString(R.string.canceled_by_driver));
                break;
            case "9":
                holder.cancel.setVisibility(View.VISIBLE);
                holder.cancel.setText(context.getString(R.string.canceled_by_you));
                break;
            default:
                holder.cancel.setVisibility(View.GONE);
                break;
        }
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (dataList.get(position).getStatus()) {
                    case "8":
                        CommonMethods.showAlert(context, context.getString(R.string.message), context.getString(R.string.canceled_by_driver));
                        break;
                    case "9":
                        CommonMethods.showAlert(context, context.getString(R.string.message), context.getString(R.string.canceled_by_you));
                        break;
                    default:
                        Intent intent = new Intent(context, RideHistoryDetail.class);
                        intent.putExtra(Constants.APPOINTMENT_ID, dataList.get(position).getApp_appointment_id());
                        context.startActivity(intent);
                        break;
                }
            }
        });
        return view;
    }

    private static class ViewHolder {
        ImageView rideDetail;
        LinearLayout parentLayout;
        TextView amount, picUpAddress, dropOffAddress, bookingId, date, cancel;
    }
}
