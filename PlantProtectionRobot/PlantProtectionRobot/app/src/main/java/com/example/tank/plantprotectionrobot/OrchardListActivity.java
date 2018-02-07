package com.example.tank.plantprotectionrobot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.example.tank.plantprotectionrobot.ChoicePage.MySpinnerAdapter;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.appdata.ListviewUserNameAdp;
import com.example.tank.plantprotectionrobot.appdata.SoundPlayUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingData;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingLength;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getFileList;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getMappingList;
import static com.example.tank.plantprotectionrobot.appdata.SoundPlayUtils.init;
import static com.example.tank.plantprotectionrobot.appdata.SoundPlayUtils.play;

/*
 @果园列表界面，显示已经建地图的农场
 */
public class OrchardListActivity extends AppCompatActivity {

    Button newMapBtn;
    Button worBtn;

    Spinner spinner1; //机器
    Spinner spinner2; //省
    Spinner spinner3; //市
    Spinner spinner4; //县
    Spinner spinner5;//乡

    private ListView listView;
    private ListviewUserNameAdp listviewUserNameAdp;

    //用户所在城市
    private String  provinceName;
    private String  cityName;
    private String  countyName;
    private String   twonName;

    private ArrayList<String> seletUser = new ArrayList<String>();

    //设置数据，此处保存当前打开的农场主名，即是农场主文件中名
    private SharedPreferences setinfo;
    private SharedPreferences.Editor infoEditor;

    private Intent newMap;
    private Intent workMap;

    private final String TAG = "Tank001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orchard_list);

        spinner1=(Spinner)findViewById(R.id.spinner1);
        spinner2=(Spinner)findViewById(R.id.spinner2);
        spinner3=(Spinner)findViewById(R.id.spinner3);
        spinner4=(Spinner)findViewById(R.id.spinner4);
        spinner5=(Spinner)findViewById(R.id.spinner5);
        listView=(ListView)findViewById(R.id.listView) ;

        newMapBtn = (Button)findViewById(R.id.button1);
        worBtn = (Button)findViewById(R.id.button3);

        //配置数据文件
        setinfo = getSharedPreferences("TankSetInfo", Context.MODE_PRIVATE);
        infoEditor = setinfo.edit();

        //需要启动的APP
        newMap = new Intent(this,ChoiveActivity.class);
        workMap = new Intent(this,WorkMapActivity.class);

        listviewUserNameAdp = new ListviewUserNameAdp(this,seletUser);

        //初始化数据
        initData();
        //按键监听
        setOnClick();

    }

    private void setOnClick(){

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                listviewUserNameAdp.clearSelection(i);
                listviewUserNameAdp.notifyDataSetChanged();
            }
        });
        //所在省
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                provinceName = spinner2.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //市
        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                     cityName=spinner3.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //县
        spinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
               countyName = spinner4.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //选择所属乡镇
        spinner5.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
               twonName = spinner5.getSelectedItem().toString();
          //      Log.d(TAG,userListGroup+"\n");
                //获取所有用户文件
                File[] files = getFileList();
                if(files !=null){
                    for (int j=0;j<files.length;j++){
                        String name = files[i].getName();

                        if(name.indexOf(provinceName) !=-1 && name.indexOf(cityName) !=-1
                                && name.indexOf(countyName) !=-1 && name.indexOf(twonName) !=-1 ){
                            String b[]= name.split(",");

                            if(b !=null) {
                                if (b.length > 0) {
                                    seletUser.add(b[1]);
                                    //    Log.d(TAG, "解析成功：" + name);
                                }
                            }
                        }

                    }
                    if(seletUser.size()>0) {
                        listviewUserNameAdp.setNameList(seletUser);
                        listView.setAdapter(listviewUserNameAdp);
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        //新建地图按钮
        newMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(newMap);
            }
        });
        //开始作业按钮
        worBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

              /*
                //获取文件路径
                String UserFileUsing = setinfo.getString("UserFileUsing",null);
                String filedir = "Tank" + File.separator + UserFileUsing + File.separator + "mapping";

                File[] files = getMappingList(filedir);
                int[] len=new int[2];
                if(files != null ) {
                    for (int i = 0; i < files.length; i++) {

                        ArrayList<MappingGroup> gpslist = new ArrayList<MappingGroup>();
                        getMappingLength(filedir+File.separator+files[i].getName(),len);

                        getMappingData(filedir+File.separator+files[i].getName(),gpslist,len[0]);

                        Log.d("debug001", files[i].getName() +" 帧长："+len[0]+" 测绘点个数："+ gpslist.size()+"\n");

                        for(int j=0;j<gpslist.size();j++){
                            String gps="";
                            gps+="RTK状态:"+gpslist.get(j).rtkState;
                            gps+=" ";
                            gps+="经度:"+gpslist.get(j).longitude;
                            gps+=" ";
                            gps+="纬度:"+gpslist.get(j).latitude;
                            gps+=" ";
                            gps+="海拔:"+gpslist.get(j).altitude;
                            gps+="\n";
                            gps+="方向:"+gpslist.get(j).direction;
                            gps+=" ";
                            gps+="GPS周:"+gpslist.get(j).GPSTime_wn;
                            gps+=" ";
                            gps+="GPS时间:"+gpslist.get(j).GPSTime_tow;
                            Log.d("debug001",gps);

                        }
                    }
                }
*/
              //    startActivity(workMap);

            }
        });
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
        adapter = new MySpinnerAdapter(this,
                android.R.layout.simple_spinner_item, listProvince);
        spinner2.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this,
                android.R.layout.simple_spinner_item, listCity);
        spinner3.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this,
                android.R.layout.simple_spinner_item, listCounty);
        spinner4.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this,
                android.R.layout.simple_spinner_item, listTown);
        spinner5.setAdapter(adapter);



    }

}
