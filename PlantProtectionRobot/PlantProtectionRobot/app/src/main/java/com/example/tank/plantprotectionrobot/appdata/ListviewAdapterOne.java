package com.example.tank.plantprotectionrobot.appdata;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.tank.plantprotectionrobot.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListviewAdapterOne extends BaseAdapter {

     private List<Map<String, Object>> data;
    private LayoutInflater layoutInflater;
    private Context context;
    public ArrayList<Integer> checkedNum = new ArrayList<>();

 //   private final String DEBUG_TAG = "MyListViewAdapter调试";
    private final String DEBUG_TAG = "Tank001";

public final class ListItemView{                //自定义控件集合
    public LinearLayout linearLayout;
    public TextView textView1;
    public TextView textView2;
    public TextView textView3;
    public TextView textView4;
    public TextView textView5;

}


    public ListviewAdapterOne(Context context, List<Map<String, Object>> data){
        this.context=context;
        this.data=data;
        this.layoutInflater= LayoutInflater.from(context);

        for (int i=0;i<data.size();i++){
            checkedNum.add(0);
        }
     //   Log.i(DEBUG_TAG,"listview元素个数checkedNum.size() ="+checkedNum.size());
    }
    public ArrayList<Integer> getCheckedNum(){
        return checkedNum;
    }
    @Override
    public int getCount() {

        return data.size();
    }
    /**
     * 获得某一位置的数据
     */
    @Override
    public Object getItem(int position) {
        return data.get(position);
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
        ListItemView listItemView = null;
        listItemView = new ListItemView();

        //获取组件，实例化组件
        convertView=layoutInflater.inflate(R.layout.center_item1, null);
        listItemView.linearLayout=(LinearLayout)convertView.findViewById(R.id.linearLayout1);
        listItemView.textView1 = (TextView)convertView.findViewById(R.id.textView1);
        listItemView.textView2 = (TextView)convertView.findViewById(R.id.textView2);
        listItemView.textView3 = (TextView)convertView.findViewById(R.id.textView3);
        listItemView.textView4 = (TextView)convertView.findViewById(R.id.textView4);
        listItemView.textView5 = (TextView)convertView.findViewById(R.id.textView5);


        //绑定数据
        listItemView.textView1.setText((String)data.get(position).get("mac_number"));
        listItemView.textView2.setText((String)data.get(position).get("mac_task"));
        listItemView.textView3.setText((String)data.get(position).get("mac_pesticides"));
        listItemView.textView4.setText((String)data.get(position).get("mac_power"));
        listItemView.textView5.setText((String)data.get(position).get("mac_state"));

        return convertView;
    }
}
