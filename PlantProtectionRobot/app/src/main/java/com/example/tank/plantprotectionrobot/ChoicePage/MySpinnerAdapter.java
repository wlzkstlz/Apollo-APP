package com.example.tank.plantprotectionrobot.ChoicePage;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by TK on 2018/1/12.
 * 下拉选项设置类
 */

public class MySpinnerAdapter extends ArrayAdapter<String> {
    Context context;
    String[] items = new String[] {};

    public MySpinnerAdapter(final Context context,
                            final int textViewResourceId, final String[] objects) {
        super(context, textViewResourceId, objects);
        this.items = objects;
        this.context = context;
    }

    //下拉列表设置
    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {

        //     parent.setBackgroundColor(getResources().getColor(R.color.myItemBackground2));
        final TextView tv;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(
                    android.R.layout.simple_spinner_item, parent, false);
        }

        tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(items[position]);
        //       tv.setTextColor(getResources().getColor(R.color.white));
        tv.setTextSize(18);
        tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        //      tv.setBackgroundColor(getResources().getColor(R.color.myItemBackground2));
        tv.setPadding(0,0,0,10);

        return convertView;

    }

    //显示窗口设置
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(
                    android.R.layout.simple_spinner_item, parent, false);
        }

        // android.R.id.text1 is default text view in resource of the android.
        // android.R.layout.simple_spinner_item is default layout in resources of android.

        tv = (TextView) convertView.findViewById(android.R.id.text1);
        tv.setText(items[position]);
        //     tv.setTextColor(getResources().getColor(R.color.white));
        tv.setTextSize(17);
        tv.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        //    tv.setBackgroundColor(getResources().getColor(R.color.myItemBackground));
        return convertView;
    }
}