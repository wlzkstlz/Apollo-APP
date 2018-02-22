package com.example.tank.plantprotectionrobot.appdata;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.example.tank.plantprotectionrobot.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListviewAdapterTwo extends BaseAdapter {

    private List<Map<String, Object>> data;
    private LayoutInflater layoutInflater;
    private Context context;
    public ArrayList<Integer> checkedNum = new ArrayList<>();

    private final String DEBUG_TAG = "Tank001";

    static public  class ListItemView{                //自定义控件集合

        public LinearLayout linearLayout;
    //    public RadioButton radioButton;
        public CheckBox checkBox;
        public TextView textView1;
        public TextView textView2;
        public TextView textView3;
        public TextView textView4;
        public TextView textView5;
        public int robotPosition;

    }


    public ListviewAdapterTwo(Context context, List<Map<String, Object>> data){
        this.context=context;
        this.data=data;
        this.layoutInflater= LayoutInflater.from(context);

        for (int i=0;i<data.size();i++){
            checkedNum.add(0);
        }
   //     Log.i(DEBUG_TAG,"listview元素个数checkedNum.size() ="+checkedNum.size());
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
      //  ListItemView listItemView = null;
        final ListItemView listItemView = new ListItemView();
        convertView=layoutInflater.inflate(R.layout.center_item2, null);
        listItemView.linearLayout=(LinearLayout)convertView.findViewById(R.id.linearLayout1);
     //   listItemView.radioButton = (RadioButton)convertView.findViewById(R.id.radioButton1);

        listItemView.checkBox = (CheckBox)convertView.findViewById(R.id.checkBox1);
        listItemView.textView1 = (TextView)convertView.findViewById(R.id.textView1);
        listItemView.textView2 = (TextView)convertView.findViewById(R.id.textView2);
        listItemView.textView3 = (TextView)convertView.findViewById(R.id.textView3);
        listItemView.textView4 = (TextView)convertView.findViewById(R.id.textView4);
        listItemView.textView5 = (TextView)convertView.findViewById(R.id.textView5);
        listItemView.robotPosition = position;

        listItemView.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(listItemView.checkBox.isChecked()){
                    checkedNum.set(listItemView.robotPosition,1);//选中置标志位
                }else{
                    checkedNum.set(listItemView.robotPosition,0);//选中置标志位
                }
            }
        });

/*
        listItemView.radioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

              Log.d(DEBUG_TAG,"ListviewAdapterTwo要删除的是"+listItemView.robotPosition);
              if(listItemView.radioButton.isChecked()){
                  listItemView.radioButton.setChecked(false);
              }else{
                  listItemView.radioButton.setChecked(true);
              }

            }
        });
*/
        //绑定数据
        listItemView.textView1.setText((String)data.get(position).get("mac_number"));
        listItemView.textView2.setText((String)data.get(position).get("mac_task"));
        listItemView.textView3.setText((String)data.get(position).get("mac_pesticides"));
        listItemView.textView4.setText((String)data.get(position).get("mac_power"));
        listItemView.textView5.setText((String)data.get(position).get("mac_state"));

        return convertView;
    }


}
