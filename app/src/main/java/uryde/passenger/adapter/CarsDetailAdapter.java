package uryde.passenger.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import uryde.passenger.R;
import uryde.passenger.fragments.BookYourRide;
import uryde.passenger.model.CarDetail;
import uryde.passenger.util.Constants;


public class CarsDetailAdapter extends RecyclerView.Adapter<CarsDetailAdapter.ViewHolder> {

    private Context mContext;
    private BookYourRide fragment;
    public static int selected_position;
    public List<CarDetail> mItems;
    public static final String TAG = CarsDetailAdapter.class.getName();

    public CarsDetailAdapter(Context context, List<CarDetail> items, BookYourRide bookYourRide, int selected_position) {
        mContext = context;
        mItems = items;
        this.fragment = bookYourRide;
        this.selected_position = selected_position;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int i) {

        CarDetail item = mItems.get(i);
        viewHolder.mText.setText(mItems.get(i).getType_name());
        Log.e(TAG + "Car detail size", "" + mItems.size());

        if (selected_position == i) {
            viewHolder.mRadio.clearFocus();
            Log.i("clicked_pos", "same " + selected_position + " " + i);
            Glide.with(mContext)
                    .load(Constants.CAR_IMAGE_URL + item.getHover_image())
                    .crossFade()
                    .placeholder(R.drawable.ic_masarcar_grey)
                    .into(viewHolder.mRadio);
        } else {
            Log.i("clicked_pos", "different " + selected_position + " " + i);
            Glide.with(mContext).
                    load(Constants.CAR_IMAGE_URL + item.getNormal_image())
                    .crossFade()
                    .placeholder(R.drawable.ic_masarcar_grey)
                    .into(viewHolder.mRadio);
        }

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notifyItemChanged(selected_position);

                if (selected_position == i) {
                    fragment.showCarDetails(mItems.get(i).getType_name(), mItems.get(i).getType_desc(), mItems.get(i).getMin_price_per_distance(), mItems.get(i).getMax_price_per_distance()
                            , mItems.get(i).getMin_fare(), mItems.get(i).getMax_size(), mItems.get(i).getBase_fare(), mItems.get(i).getPrice_per_min(), mItems.get(i).getBase_fare_type()
                            , mItems.get(i).getHover_image());
                    mItems.get(selected_position).setSelected(false);
                } else {
                    mItems.get(selected_position).setSelected(true);
                    fragment.startPublishingWithTimer(mItems.get(i).getVehicle_type_city_id(), mItems.get(i).getVehicle_type_id(), mItems.get(i).getBase_fare_type(),mItems.get(i).getBase_fare_type(),"0");
                }
                selected_position = i;
                notifyItemChanged(selected_position);
                Log.i(TAG, "clecked on " + mItems.get(selected_position).getType_name() + selected_position);
                Log.e(TAG, "" + mItems.size());


            }
        });

    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.cars_list, viewGroup, false);
        return new ViewHolder(view);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView mText;
        ImageView mRadio;
        LinearLayout mCarLayout;

        ViewHolder(View inflate) {
            super(inflate);
            mText = (TextView) inflate.findViewById(R.id.car_name);
            mRadio = (ImageView) inflate.findViewById(R.id.radio);
            mCarLayout = (LinearLayout) inflate.findViewById(R.id.car_layout);
        }
    }
}
