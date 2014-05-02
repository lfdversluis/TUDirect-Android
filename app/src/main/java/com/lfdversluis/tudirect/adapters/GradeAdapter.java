package com.lfdversluis.tudirect.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lfdversluis.tudirect.R;
import com.lfdversluis.tudirect.objects.CourseWrap;

import java.util.ArrayList;

public class GradeAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private ArrayList<CourseWrap> objects;

    public GradeAdapter(Context context, ArrayList<CourseWrap> objects) {
        inflater = LayoutInflater.from(context);
        this.objects = objects;
    }

    public int getCount() {
        return objects.size();
    }

    public CourseWrap getItem(int position) {
        return objects.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.gradelist, null);
            holder.textViewCourse = (TextView) convertView.findViewById(R.id.textCourse);
            holder.textViewGrade = (TextView) convertView.findViewById(R.id.textGrade);
            holder.textViewDate = (TextView) convertView.findViewById(R.id.textDate);
            holder.textViewECTS = (TextView) convertView.findViewById(R.id.textECTS);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.textViewCourse.setText(objects.get(position).getCourseId());
        holder.textViewGrade.setText(objects.get(position).getGrade());
        holder.textViewDate.setText("Last change: " + objects.get(position).getMutationDate());

        // API changed for grades without backwards compatibility
        // thus if ects equals - it means it's the grade screen and no ECTS can be
        // shown.
        if (!objects.get(position).getECTS().matches("-")) {
            holder.textViewECTS.setText(objects.get(position).getECTS() + " ECTS");
        } else {
            holder.textViewECTS.setText("");
        }

        if (objects.get(position).isSufficient()) {
            holder.textViewGrade.setTextColor(0xFF00DD00);
        } else {
            if (!objects.get(position).getGrade().equals("NV")) {
                holder.textViewGrade.setTextColor(0xFFDD0000);
            } else {
                holder.textViewGrade.setTextColor(0xFF000000);
            }
        }
        return convertView;
    }

    private class ViewHolder {
        TextView textViewCourse;
        TextView textViewGrade;
        TextView textViewDate;
        TextView textViewECTS;
    }
}