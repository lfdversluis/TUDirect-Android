package com.lfdversluis.tudirect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lfdversluis.tudirect.R;
import com.lfdversluis.tudirect.objects.ScheduleObject;

import java.util.ArrayList;

public class ScheduleAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private ArrayList<ScheduleObject> objects;

    public ScheduleAdapter(Context context, ArrayList<ScheduleObject> objects) {
        inflater = LayoutInflater.from(context);
        this.objects = objects;
    }

    public int getCount() {
        return objects.size();
    }

    public ScheduleObject getItem(int position) {
        return objects.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.schedulelist, null);
            holder.textView1 = (TextView) convertView.findViewById(R.id.textCourse);
            holder.textView2 = (TextView) convertView.findViewById(R.id.textBegin);
            holder.textView3 = (TextView) convertView.findViewById(R.id.textEnd);
            holder.textView4 = (TextView) convertView.findViewById(R.id.textPlace);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textView1.setText(objects.get(position).getCourse());
        holder.textView2.setText(objects.get(position).getBeginDate());
        holder.textView3.setText(objects.get(position).getEndDate());
        holder.textView4.setText(objects.get(position).getPlace());

        return convertView;
    }

    private class ViewHolder {
        TextView textView1;
        TextView textView2;
        TextView textView3;
        TextView textView4;
    }
}