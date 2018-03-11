package com.example.tank.plantprotectionrobot.ChoicePage;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.R;
import com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool;

import java.io.File;


/**
 *@新建地图时，给地图命名的界面page
 *用户命名是不能有下划线_
 */

public class SetMapName extends Fragment{

    private Button btn;
    private Spinner spinner1;
    private Spinner spinner2;
    private Spinner spinner3;
    private Spinner spinner4;
    private EditText userET;
    private EditText phoneET;

    //农场主命名
    private String  provinceName;
    private String  cityName;
    private String  countyName;
    public String   twonName;
    private String  userName;
    private String  phoneNumner;

    //设置数据，此处保存当前打开的农场主名，即是农场主文件中名
    private SharedPreferences setinfo;
    private SharedPreferences.Editor infoEditor;

    private OnFragmentClickListener onFragmentClickListener;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choive_page1, container, false);
        btn = (Button)view.findViewById(R.id.button1);

        spinner1= (Spinner)view.findViewById(R.id.spinner1);
        spinner2= (Spinner)view.findViewById(R.id.spinner2);
        spinner3= (Spinner)view.findViewById(R.id.spinner3);
        spinner4= (Spinner)view.findViewById(R.id.spinner4);
        userET=(EditText)view.findViewById(R.id.editText1);
        phoneET=(EditText)view.findViewById(R.id.editText2);

        //配置数据文件
       setinfo = getActivity().getSharedPreferences("TankSetInfo", Context.MODE_PRIVATE);
       infoEditor = setinfo.edit();

        //初始化数据
        initData();

        //下一步按钮
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                userName = ","+userET.getText().toString();
                phoneNumner = phoneET.getText().toString();
/*
                if(onFragmentClickListener !=null) {
                    onFragmentClickListener.onFragmentClick(btn);
                }
*/

                if (TextUtils.isEmpty(userName))
                {
                    Toast.makeText(getActivity().getApplicationContext(), "请输入用户名",
                            Toast.LENGTH_SHORT).show();
                }else
                {

                    if(isMobile(phoneNumner))
                    {
                        if(SDCardFileTool.createDataFiles("Tank"+ File.separator +provinceName+countyName+cityName+cityName
                                +twonName+userName+phoneNumner+ File.separator +"work")) {
                            //农场果园命名
                            String UserName = provinceName+countyName+cityName+
                                    cityName+twonName+userName+phoneNumner;

                            if(SDCardFileTool.createDataFiles("Tank" + File.separator +UserName+ File.separator +"mapping")){
                                //如果读没有错误则进入新页面，UserFileUsing当前app打开的农场主文件名
                                infoEditor.putString("UserFileUsing",UserName);
                                infoEditor.commit();

                                if(onFragmentClickListener !=null) {
                                    onFragmentClickListener.onFragmentClick(btn);
                                }

                            }else {
                                Toast.makeText(getActivity().getApplicationContext(), "手机读写权限获取失败请手动打开",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            Toast.makeText(getActivity().getApplicationContext(), "手机读写权限获取失败请手动打开",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }else
                    {
                        Toast.makeText(getActivity().getApplicationContext(), "请输入正确的手机号码",
                                Toast.LENGTH_SHORT).show();
                    }

                }

    //            Log.d("debug001",provinceName+countyName+cityName+cityName+twonName+userName+phoneNumner);

            }
        });

        /*
         @spinner_ 建立地图的省市县镇，以及按键响应
         */
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                provinceName = spinner1.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

               cityName  = spinner2.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                countyName  = spinner3.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                twonName =spinner4.getSelectedItem().toString();
                Log.d("debug001",provinceName+cityName+cityName+twonName);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return view;

    }

    /*
   *初始化数据
    */
    private void initData() {

        //获取省市县乡
        String [] listProvince= (String[])getResources().getStringArray(R.array.province_spinner);
        String [] listCity= (String[])getResources().getStringArray(R.array.city_spinner);
        String [] listCounty= (String[])getResources().getStringArray(R.array.county_spinner);
        String [] listTown= (String[])getResources().getStringArray(R.array.town_spinner);

        //填充到spinner
        MySpinnerAdapter adapter;
        adapter = new MySpinnerAdapter(this.getActivity(),
                android.R.layout.simple_spinner_item, listProvince);
        spinner1.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this.getActivity(),
                android.R.layout.simple_spinner_item, listCity);
        spinner2.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this.getActivity(),
                android.R.layout.simple_spinner_item, listCounty);
        spinner3.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this.getActivity(),
                android.R.layout.simple_spinner_item, listTown);
        spinner4.setAdapter(adapter);

    }

    /**
     * 验证手机格式
     */
    public static boolean isMobile(String number) {

        String num = "[1][34578]\\d{9}";//"[1]"代表第1位为数字1，"[34578]"代表第二位可以为3、4、5、7、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        if (TextUtils.isEmpty(number)) {
            return false;
        } else {
            return number.matches(num);//判断是否匹配 是true 否 flase
        }
    }



    public void setOnFragmentClickListener(OnFragmentClickListener onFragmentClickListener) {
        this.onFragmentClickListener = onFragmentClickListener;
    }

    public interface OnFragmentClickListener{
        public void onFragmentClick(View view);
    }




}
