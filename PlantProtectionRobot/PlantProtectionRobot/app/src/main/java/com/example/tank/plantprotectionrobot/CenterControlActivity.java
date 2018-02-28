package com.example.tank.plantprotectionrobot;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.BLE.BluetoothLeClass;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool;
import com.example.tank.plantprotectionrobot.Robot.HeatDataMsg;
import com.example.tank.plantprotectionrobot.Robot.TankRobot;
import com.example.tank.plantprotectionrobot.Robot.WorkMatch;
import com.example.tank.plantprotectionrobot.appdata.ListviewAdapterOne;
import com.example.tank.plantprotectionrobot.appdata.ListviewAdapterTwo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.initLocationPermission;
import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.verifyStoragePermissions;

/*
 @总控台界面
 */


public class CenterControlActivity extends AppCompatActivity {

    private static boolean isExit = false;

    private Button workDataBtn;
    private Button listBtn;
    private Button addBtn1;
    private Button addBtn2;
    private Button deleBtn;
    private Button dleOkBtn;

    private ListView listView;
    private List<Map<String, Object>> dataList;

    private Intent workData;
    private Intent orchardList;
    private Intent connectMac;
    private Intent workMap;
    //点击删除机器人时，不更新界面
    private boolean detRobotFlag=false;

    //蓝牙
    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**蓝牙管理器**/
    private BluetoothManager bluetoothManager;


    //BLE service
    private CenterControlActivity.BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;
    private Intent intentSev;
    public ArrayList<TankRobot> workRobotList;
    //工作匹配组，机器人与果园、路径匹配


    //
    private final int EXIT_MSG=0;
    private final int HEART_MSG = 20;  //接收到心跳数据
    private final int TANKLEVEL_MIN=5;
    private final int BATTERY_MIN =10;


    private final String DEBUG_TAG = "Tank001";
    private  ListviewAdapterTwo listviewAdapterTwo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_center_control);
        workDataBtn = (Button) findViewById(R.id.button1);
        listBtn = (Button) findViewById(R.id.button2);
        addBtn1 = (Button) findViewById(R.id.button3);
        addBtn2 = (Button) findViewById(R.id.button5);
        deleBtn = (Button) findViewById(R.id.button4);
        dleOkBtn = (Button) findViewById(R.id.button6);
        dleOkBtn.setVisibility(View.INVISIBLE);
        addBtn1.setVisibility(View.INVISIBLE);
        listView = (ListView) findViewById(R.id.listview1);

        workRobotList = new ArrayList<TankRobot>();


        //需要启动的activity
        workData = new Intent(this, JobDataActivity.class);
        orchardList = new Intent(this, OrchardListActivity.class);
        connectMac = new Intent(this, ConnectActivity.class);
        workMap = new Intent(this,WorkMapActivity.class);

        //蓝牙后台服务
        intentSev = new Intent(this, BLEService.class);
        startService(intentSev); //启动蓝牙后台


        setOnClick();//按键监听

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    private void setOnClick() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            //    Log.d(DEBUG_TAG,"你点的是"+i);
            if(workRobotList.size()>0) {
               if(!workRobotList.get(i).workMatch.orchardName.equals("")){
                   workMap.putExtra("robotId",workRobotList.get(i).heatDataMsg.robotId);
                   startActivity(workMap);
               }else{
                   Toast.makeText(getApplicationContext(), "机器人还未匹配果园",
                           Toast.LENGTH_SHORT).show();
               }
            }

            }
        });
        workDataBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(workData);
            }
        });
        listBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(orchardList);
            }
        });

        addBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(connectMac);
            }
        });
        addBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(workRobotList.size()<5) {
                    startActivity(connectMac);
                }else{
                    Toast.makeText(getApplicationContext(), "最多只能添加5台机器人",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        deleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(workRobotList.size() > 0) {
                    listviewAdapterTwo = new ListviewAdapterTwo(CenterControlActivity.this, dataList);
                    listView.setAdapter(listviewAdapterTwo);

                    dleOkBtn.setVisibility(View.VISIBLE);
                    addBtn2.setVisibility(View.INVISIBLE);
                    deleBtn.setVisibility(View.INVISIBLE);
                    detRobotFlag =true;
                }
            }
        });

        dleOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                detRobotFlag =false;
                dleOkBtn.setVisibility(View.INVISIBLE);
                addBtn2.setVisibility(View.VISIBLE);
                deleBtn.setVisibility(View.VISIBLE);

           //     List<Map<String, Object>> robotlist = new ArrayList<Map<String, Object>>();
                ArrayList<TankRobot> robotList = new ArrayList<TankRobot>();
                boolean detFlag =false;
                for(int i=0;i<dataList.size();i++){
                    if(listviewAdapterTwo.checkedNum.get(i) == 1) {
               //         Log.d(DEBUG_TAG,"删除的是："+i+"\n");
                        detFlag =true;
                    }else{
                  //      robotlist.add(dataList.get(i));
                        robotList.add(workRobotList.get(i));
                    }

                }

                //有删除机器人，更新数据
                if(detFlag == true) {
                    //跟新后台机器人列表
                    if (binder != null) {
                        workRobotList = robotList;
                        binder.changeWorkRobotList(workRobotList);
                    }
                    //删除后的数据
                    dataList.clear();
                    dataList = getData();
                }
                listView.setAdapter(new ListviewAdapterOne(CenterControlActivity.this, dataList));


            }
        });
    }

    /***
     *
     * @return 解析成显示数据
     */
    public List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        if(workRobotList.size()>0) {

            for (int i = 0; i < workRobotList.size(); i++) {
                Map<String, Object> map = new HashMap<String, Object>();

                String state="";
                if(workRobotList.get(i).heatDataMsg.robotId<10){
                    map.put("mac_number", "0000" + workRobotList.get(i).heatDataMsg.robotId);
                }else if(workRobotList.get(i).heatDataMsg.robotId<100){
                    map.put("mac_number", "000" + workRobotList.get(i).heatDataMsg.robotId);
                }else if(workRobotList.get(i).heatDataMsg.robotId<1000){
                    map.put("mac_number", "00" + workRobotList.get(i).heatDataMsg.robotId);
                }else if(workRobotList.get(i).heatDataMsg.robotId<10000){
                    map.put("mac_number", "0" + workRobotList.get(i).heatDataMsg.robotId);
                }else{
                    map.put("mac_number", "" + workRobotList.get(i).heatDataMsg.robotId);
                }


                if(workRobotList.get(i).robotOnline == false  || (workRobotList.get(i).heatDataMsg.rtkState & 0x03) != 0x03) {

                    map.put("mac_task", "--");
                    map.put("mac_pesticides", "--");
                    map.put("mac_power", "--");

                    state +="离线";

                }else{

                    map.put("mac_task", "" + workRobotList.get(i).heatDataMsg.curState + "%");
                    map.put("mac_pesticides", "" + workRobotList.get(i).heatDataMsg.tankLevel + "%");
                    map.put("mac_power", "" + workRobotList.get(i).heatDataMsg.batteryPercentage + "%");

                    if(workRobotList.get(i).heatDataMsg.taskFile == true){
                        if (workRobotList.get(i).heatDataMsg.curState < 100) {
                            state += "作业";
                        } else {
                            state += "完成";
                        }
                    }else{
                        state += "转场";
                    }
                    if(workRobotList.get(i).heatDataMsg.tankLevel < TANKLEVEL_MIN){
                        state +="|加药";
                    }
                    if(workRobotList.get(i).heatDataMsg.batteryPercentage<BATTERY_MIN){
                        state +="|换电";
                    }
                    if(workRobotList.get(i).heatDataMsg.dAlarm == true){
                        state +="|救援";
                    }

                }
                map.put("mac_state", state);
                list.add(map);
            }

        }

        return list;

    }


   private Handler bleHandler = new Handler(){
       @Override
       public void handleMessage(Message msg) {
           switch (msg.what){
               case EXIT_MSG:
                   isExit = false;
                   break;
               case HEART_MSG:
                   if(detRobotFlag == false) {
                       dataList = getData();
                       listView.setAdapter(new ListviewAdapterOne(CenterControlActivity.this, dataList));
                   }
                   break;
           }

           super.handleMessage(msg);


       }
   };



    @Override
    protected void onStart() {
        verifyStoragePermissions(this) ;//针对6.0以上版本做权限适配
        initLocationPermission(this);

        //绑定蓝牙服务
        bleServiceConn = new CenterControlActivity.BleServiceConn();
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
            binder.setBleWorkTpye(BLEService.BLE_HANDLE_CONECT,true);
            workRobotList = binder.getRobotList();//数据在后台一致运行，进入时获取当前添加的所有机器人
            Log.d(DEBUG_TAG,"添加的机器人数"+workRobotList.size());
            //添加显示数据
            dataList= getData();
            listView.setAdapter(new ListviewAdapterOne(CenterControlActivity.this, dataList));


            binder.getService().setRobotWorkingCallback(new BLEService.RobotWorkingCallback() {
                @Override
                public void RobotStateChanged(TankRobot tankRobot) {
                    for(int i=0;i<workRobotList.size();i++){
                        if(workRobotList.get(i).heatDataMsg.robotId == tankRobot.heatDataMsg.robotId){
                            workRobotList.get(i).heatDataMsg = tankRobot.heatDataMsg;
                            bleHandler.sendEmptyMessage(HEART_MSG);
                        }
                    }

                }

                @Override
                public void ComdReturnChange(HeatDataMsg heatDataMsg) {

                }

                @Override
                public void BleStateChanged(int msg) {
                    switch(msg){
                        case BLEService.BLE_CONNECT_OFF:
                            //掉线重连
                            binder.connectBle(null,true);
                            break;
                        case BLEService.BLE_CONNECT_ON:
                            break;
                        case BLEService.BLE_CONNECTED:
                            break;
                    }
                }

                @Override
                public void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList) {
                    if (mBleDeviceList.size()>0) {
                        //连接蓝牙
                        binder.connectBle(mBleDeviceList.get(0),false);
                    }
                }

                @Override
                public void BleConnectedDevice(BluetoothDevice connectedDevice) {
                    if(connectedDevice == null){
                   //     binder.startScanBle();
                    }

                }
            });

        }

        // 服务奔溃或者被杀掉执行
        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    }


    private void exit() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            // 利用handler延迟发送更改状态信息
            bleHandler.sendEmptyMessageDelayed(0, 2000);
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN); //指定跳到系统桌面
            startMain.addCategory(Intent.CATEGORY_HOME);
            startActivity(startMain); //开始跳转
            //    System.exit(0);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

}