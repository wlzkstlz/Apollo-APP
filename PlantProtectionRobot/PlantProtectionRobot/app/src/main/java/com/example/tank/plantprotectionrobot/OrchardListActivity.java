package com.example.tank.plantprotectionrobot;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.ChoicePage.MySpinnerAdapter;
import com.example.tank.plantprotectionrobot.Robot.TankRobot;

import com.example.tank.plantprotectionrobot.Robot.WorkMatch;
import com.example.tank.plantprotectionrobot.appdata.ListviewUserNameAdp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingData;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingLength;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getFileList;


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
    private ArrayList<String> orchardList = new ArrayList<String>();

    //设置数据，此处保存当前打开的农场主名，即是农场主文件中名
    private SharedPreferences setinfo;
    private SharedPreferences.Editor infoEditor;

    //跳转的界面
    private Intent newMap;
    private Intent workMap;

    //BLE service
    private OrchardListActivity.BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;
    private Intent intentSev;

    //当前在线机器人
    private ArrayList<TankRobot> workRobotList;

    private int selectRobotPon=0;//选中的机器人在ArrayList的位置
    private String selectOrchardName="";//选择的果园名

    //机器人匹配组
  //  public WorkMatch newWorkMatch=new WorkMatch();
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
        intentSev= new Intent(this,BLEService.class);

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
                listviewUserNameAdp.orchardSelection(i);
                listviewUserNameAdp.notifyDataSetChanged();
                selectOrchardName = orchardList.get(i);//果园名
            }
        });

        //在线的机器人
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if(workRobotList.size()>0) {
               //     newWorkMatch.robotId = workRobotList.get(i).heatDataMsg.robotId;//机器人ID
                    selectRobotPon = i;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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
                seletUser.clear();
                if(files !=null){
                    for (int j=0;j<files.length;j++){
                        String name = files[j].getName();
                        orchardList.add(name);
                      //  Log.d(TAG, "解析成功：" + files[i].toString());

                        if(name.indexOf(provinceName) !=-1 && name.indexOf(cityName) !=-1
                                && name.indexOf(countyName) !=-1 && name.indexOf(twonName) !=-1 ){
                            String b[]= name.split(",");

                            if(b !=null) {
                                if (b.length > 0) {
                                    seletUser.add(b[1]);
                                //    Log.d(TAG, "解析成功：" + b[1]);
                                }
                            }
                        }

                    }
                //    if(seletUser.size()>0) {
                        listviewUserNameAdp.setNameList(seletUser);
                        listView.setAdapter(listviewUserNameAdp);
                  //  }
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
                if(workRobotList.size()>0) {
                    Toast.makeText(OrchardListActivity.this, "工作模式下不能进行测绘" ,
                            Toast.LENGTH_SHORT).show();

                }else{
                    startActivity(newMap);
                }

            }
        });
        //开始作业按钮
        worBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(workRobotList.size()>0 && !selectOrchardName.equals("")) {

                    workRobotList.get(selectRobotPon).workMatch.orchardName = selectOrchardName;
                    workMap.putExtra("robotId", workRobotList.get(selectRobotPon).heatDataMsg.robotId);

                    startActivity(workMap);
                }else {
                    Toast.makeText(OrchardListActivity.this, "没有添加机器人或未选择果园" ,
                            Toast.LENGTH_SHORT).show();
                }

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


    @Override
    protected void onStart() {
        //绑定蓝牙服务
        bleServiceConn = new OrchardListActivity.BleServiceConn();
        bindService(intentSev, bleServiceConn, Context.BIND_AUTO_CREATE);
        super.onStart();
    }

    /***
     * service连接
     */
    class BleServiceConn implements ServiceConnection {
        // 服务被绑定成功之后执行
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // IBinder service为onBind方法返回的Service实例
            binder = (BLEService.BleBinder) service;

            //绑定后执行动作
            binder.setBleWorkTpye(BLEService.BLE_HANDLE_CONECT);

            //获取当前在线的机器人
            workRobotList = binder.getRobotList();

            if(workRobotList.size()>0) {
                String[] listRobot = new String[workRobotList.size()];
                for (int i = 0; i < workRobotList.size(); i++) {
                    listRobot[i] = "果园机器人" + workRobotList.get(i).heatDataMsg.robotId;

                }
                MySpinnerAdapter adapter;
                adapter = new MySpinnerAdapter(OrchardListActivity.this,
                        android.R.layout.simple_spinner_item, listRobot);
                spinner1.setAdapter(adapter);
            }
            //设置回调
            binder.getService().setRobotWorkingCallback(new BLEService.RobotWorkingCallback() {
                @Override
                public void RobotStateChanged(TankRobot tankRobot) {

                }

                @Override
                public void BleStateChanged(int msg) {

                }

                @Override
                public void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList) {

                }

                @Override
                public void BleConnectedDevice(BluetoothDevice connectedDevice) {

                }
            });

        }

        // 服务奔溃或者被杀掉执行
        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    }


}
