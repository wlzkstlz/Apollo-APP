package com.example.tank.plantprotectionrobot.appdata;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.tank.plantprotectionrobot.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 2018/1/25.
 */

public class ListviewUserNameAdp extends BaseAdapter {

    private  ArrayList<String>  nameList;
    private LayoutInflater layoutInflater;
    private Context context;
    private int selectedPosition=-1;

    private final String TAG = "Tank001";

    public final class ListItemView{                //自定义控件集合
        public TextView textView1;

    }

    public ListviewUserNameAdp(Context context, ArrayList<String> nList){
        this.context=context;
        this.nameList=nList;
        this.layoutInflater= LayoutInflater.from(context);
    }

    public void clearSelection(int position) {
        selectedPosition = position;
    }
    public void setNameList(ArrayList<String>  nList){
        this.nameList = nList;
    }

    @Override
    public int getCount() {
        return nameList.size();
    }
    /**
     * 获得某一位置的数据
     */
    @Override
    public Object getItem(int position) {
        return nameList.get(position);
    }
    /**
     * 获得唯一标识
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        ListviewUserNameAdp.ListItemView listItemView = null;
        listItemView = new ListviewUserNameAdp.ListItemView();

        //获取组件，实例化组件
        convertView=layoutInflater.inflate(R.layout.userlist_item, null);
        listItemView.textView1 = (TextView)convertView.findViewById(R.id.textView1);

        //绑定数据
        if(nameList.size()>0) {
            listItemView.textView1.setText((String) nameList.get(position));

            if (position == selectedPosition) {
                listItemView.textView1.setBackgroundColor(Color.GREEN);
            }
        }
        return convertView;
    }
}