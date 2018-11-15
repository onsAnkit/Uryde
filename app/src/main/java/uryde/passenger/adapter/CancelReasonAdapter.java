package uryde.passenger.adapter;

/**
 * Created by admin on 18-10-2016.
 */

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import uryde.passenger.R;

public class CancelReasonAdapter extends ArrayAdapter<String> {

    private String[] reasons;
    private Activity activity;
    private int selectedPosition = -1;

    public CancelReasonAdapter(Activity context, String[] objects) {
        super(context, 0, objects);

        this.activity = context;
        this.reasons = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.cancel_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.checkBox.setTag(position);
        holder.checkBox.setText(reasons[position]);
        if (position == selectedPosition) {
            holder.checkBox.setChecked(true);
        } else holder.checkBox.setChecked(false);

        holder.checkBox.setOnClickListener(onStateChangedListener(holder.checkBox, position));

        return convertView;
    }

    private View.OnClickListener onStateChangedListener(final CheckBox checkBox, final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    selectedPosition = position;
                } else {
                    selectedPosition = -1;
                }
                notifyDataSetChanged();
            }
        };
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    private static class ViewHolder {
        private CheckBox checkBox;
        private TextView friendName;
        private LinearLayout cancelParent;

        public ViewHolder(View v) {
            checkBox = (CheckBox) v.findViewById(R.id.check);
            friendName = (TextView) v.findViewById(R.id.name);
            cancelParent = (LinearLayout) v.findViewById(R.id.cancel_parent);
        }
    }


}
