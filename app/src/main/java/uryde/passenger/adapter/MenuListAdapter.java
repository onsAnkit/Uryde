package uryde.passenger.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import uryde.passenger.R;

import java.util.List;

public class MenuListAdapter extends BaseAdapter {

    private Context context;
    private List<String> mList;
    private int selectedPos = NOT_SELECTED;
    private static final int NOT_SELECTED = -1;

    public MenuListAdapter(Context context, List<String> mList) {
        this.context = context;
        this.mList = mList;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public void setSelection(int position) {
        if (selectedPos == position) {
            selectedPos = NOT_SELECTED;
        } else {
            selectedPos = position;
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.menu_item, viewGroup, false);
        TextView textView = rowView.findViewById(R.id.text);
        ImageView image = rowView.findViewById(R.id.image);
        LinearLayout parent = rowView.findViewById(R.id.parent);
        TextView versionCode = rowView.findViewById(R.id.version_code);
        image.setVisibility(View.GONE);
        versionCode.setVisibility(View.GONE);
        textView.setText(mList.get(position));

        if (position == selectedPos) {
            textView.setTextColor(Color.parseColor("#ffffff"));
            parent.setBackgroundColor(Color.parseColor("#1e26c1"));
        } else {
            parent.setBackgroundColor(Color.parseColor("#ffffff"));
            textView.setTextColor(Color.parseColor("#333333"));
        }

        return rowView;
    }
}
