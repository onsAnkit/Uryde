package uryde.passenger.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uryde.passenger.R;
import uryde.passenger.fragments.BookYourRide;
import uryde.passenger.model.CarDetail;

/**
 * Created by admin on 10/26/2018.
 */

public class CarPagerAdapter extends PagerAdapter {

    BookYourRide bookYourRide;
    private int count;
    private Context mContext;
    private JSONArray array;
    public CarPagerAdapter(Context context, int count, JSONArray array, BookYourRide bookYourRide) {
        this.count = count;
        mContext = context;
        this.array = array;
        this.bookYourRide = bookYourRide;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemView = inflater.inflate(R.layout.category_pager_item, container, false);
        RecyclerView carsList = itemView.findViewById(R.id.cars_list);
        TextView tv_catname = itemView.findViewById(R.id.tv_cat);
        TextView tv_cat_desc = itemView.findViewById(R.id.tv_cat_desc);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        List<CarDetail> mCarsList = new ArrayList<>();
        try {
            tv_catname.setText(array.getJSONObject(position).getString("vehiclecategory_name"));
            JSONArray subarry = array.getJSONObject(position).getJSONArray("sub_vechicle");
            for (int j = 0; j < subarry.length(); j++) {
                JSONObject mObject = subarry.getJSONObject(j);
                CarDetail carDetails = new CarDetail();
                carDetails.setVehicle_type_city_id(mObject.getString("vehicle_type_city_id"));
                carDetails.setVehicle_type_id(mObject.getString("vehicle_type_id"));
                carDetails.setType_name(mObject.getString("type_name"));
                carDetails.setMax_size(mObject.getString("max_size"));
                carDetails.setMin_size(mObject.getString("min_size"));
                carDetails.setBase_fare(mObject.getString("base_fare"));
                carDetails.setMax_price_per_distance(mObject.getString("max_price_per_distance"));
                carDetails.setBase_fare_type(mObject.getString("base_fare_type"));
                carDetails.setMin_fare(mObject.getString("min_fare"));
                carDetails.setPrice_per_min(mObject.getString("price_per_min"));
                carDetails.setMin_price_per_distance(mObject.getString("min_price_per_distance"));
                carDetails.setPrice_cancellation(mObject.getString("price_cancellation"));
                carDetails.setType_desc(mObject.getString("type_desc"));
                carDetails.setCity_id(mObject.getString("city_id"));
                carDetails.setCity_name(mObject.getString("city_name"));
                carDetails.setBasefare_minute(mObject.getString("basefare_minute"));
                carDetails.setNormal_image(mObject.getString("normal_image"));
                carDetails.setHover_image(mObject.getString("hover_image"));
                carDetails.setSelected(false);
                mCarsList.add(carDetails);
            }
            //CarsDetailAdapter mAdapter=new CarsDetailAdapter()
            CarsDetailAdapter mAdapter = new CarsDetailAdapter(mContext, mCarsList, bookYourRide, 0);
            carsList.setLayoutManager(manager);
            carsList.setItemAnimator(new DefaultItemAnimator());
            carsList.setAdapter(mAdapter);

        } catch (Exception e) {


        }
        itemView.setTag("myview" + position);
        container.addView(itemView);
        return itemView;

    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }


    private void setRecyler(JSONArray subarry) {
    }

    @Override
    public void destroyItem(View container, int position, Object object) {
        ((ViewPager) container).removeView((View) object);
    }
}
