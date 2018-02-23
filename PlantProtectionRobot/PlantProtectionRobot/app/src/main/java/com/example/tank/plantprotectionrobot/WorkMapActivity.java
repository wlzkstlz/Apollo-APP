package com.example.tank.plantprotectionrobot;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.CenterControlActivity;
import com.example.tank.plantprotectionrobot.ChoicePage.MySpinnerAdapter;
import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.R;
import com.example.tank.plantprotectionrobot.Robot.TankRobot;
import com.example.tank.plantprotectionrobot.Robot.WorkMatch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingData;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingHead;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getMappingList;

/*
 @ 工作地图界面，以车位中心，每次显示一台车
 */
public class WorkMapActivity extends AppCompatActivity {

    private Button centerBtn;
    private Button startBtn;
    private TextView batteryText;
    private TextView tankLevelText;
    private TextView robotMsgText;
    private LinearLayout backgroundAlarm;


    private Intent centerCtr;
    private Spinner spinner1;

    //BLE service
    private WorkMapActivity.BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;
    private Intent intentSev;
    public ArrayList<TankRobot> workRobotList;
    //所有匹配组
  //  private ArrayList<WorkMatch> workMatchList;

    //路径文件组
    private ArrayList<ArrayList<GpsPoint>> routeList = new ArrayList<ArrayList<GpsPoint>>();
    private ArrayList<String> routeNameList = new ArrayList<String>();
    private int isSelectRobotId; //当前控制的机器人ID


    private GpsPoint screenPoint= new GpsPoint(); //获取屏幕的大小
    private final double NEAR_DIS = 0.5;//判断点是否重合距离，单位米
    private final int MAPMAX_DIS = 5000;//地图最大距离单位米
    private final int GPS_DIS = 111000;//纬度1度的距离，单位米

    private final int TANKLEVEL_MIN=5;
    private final int BATTERY_MIN =10;

   //定时器，用于更新地图
    private TimerTask timerTask;
    private Handler handler;

    //handle消息
    private final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    private final int BLE_CONNECT_OFF =21;//蓝牙连接失败
    private final int BLE_DATA_ON = 30; //接收到数据
    private final int ROBOT_UCONNECT =40;//RTK掉线
    private final int DRAW_MAP =1;

    private TankRobot isWorkRobot=null;//当前监控的机器人

    private final  String TAG = "Tank001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_map);
        backgroundAlarm = (LinearLayout)findViewById(R.id.backgroundAlarm);
        batteryText=(TextView)findViewById(R.id.battery);
        tankLevelText=(TextView)findViewById(R.id.tankLevel);
        robotMsgText =(TextView)findViewById(R.id.robotMsg);

        centerBtn = (Button)findViewById(R.id.button1);
        startBtn = (Button)findViewById(R.id.button2);
        spinner1=(Spinner) findViewById(R.id.spinner1);
       //控制中心界面
       centerCtr = new Intent(this,CenterControlActivity.class);
       intentSev= new Intent(this,BLEService.class);

        setOnClick();
        initData();

        //消息处理
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch(msg.what){
                    case DRAW_MAP:
                        drawMap();
                        break;
                    case BLE_DATA_ON:
                        updateData(true);
                        break;
                    case ROBOT_UCONNECT:
                        updateData(false);
                        break;

                }
            }
        };

    }

    /***
     * 轮序获取的数据，更新显示
     */
    private void updateData(boolean onLine){
        if(onLine == true)
        if(isWorkRobot != null){
            String state="";
            tankLevelText.setText(""+isWorkRobot.heatDataMsg.tankLevel+"%");
            batteryText.setText(""+isWorkRobot.heatDataMsg.batteryPercentage+"%");
            if(isWorkRobot.heatDataMsg.curState < 100) {
                state +="作业中";
            }else{
                state +="完成";

            }
            backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.tankgreen));
            if(isWorkRobot.heatDataMsg.tankLevel < TANKLEVEL_MIN){
                state +="|加药";
                backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
            if(isWorkRobot.heatDataMsg.batteryPercentage<BATTERY_MIN){
                state +="|换电";
                backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
            if(isWorkRobot.heatDataMsg.dAlarm == true){
                state +="|救援";
                backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorRed));
            }
            robotMsgText.setText(state);
            spinner1.setEnabled(true);//禁用

        }else{
            tankLevelText.setText("-");
            batteryText.setText("-");
            robotMsgText.setText("离线");
            backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorGray));
            spinner1.setEnabled(false);//禁用
        }
    }
    /***
     * 重绘地图
     */
    private void drawMap(){

    }
    private void setOnClick() {
        centerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(centerCtr);
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setVisibility(View.INVISIBLE);
                spinner1.setVisibility(View.VISIBLE);
            }
        });
        //控制选择
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    /***
     * 初始化数据
     */
    private void initData() {

        //获取字符
        String [] ctr= (String[])getResources().getStringArray(R.array.carCtr_spinner);

        //填充到spinner
        MySpinnerAdapter adapter;
        adapter = new MySpinnerAdapter(this,
                android.R.layout.simple_spinner_item, ctr);
        spinner1.setAdapter(adapter);

        screenPoint.x = 0;
        screenPoint.y = 0;
    }


    @Override
    protected void onStart() {

        //接收当前正在控制的机器人
        isSelectRobotId = getIntent().getIntExtra("robotId",-1);
        //绑定蓝牙服务
        bleServiceConn = new WorkMapActivity.BleServiceConn();
        bindService(intentSev, bleServiceConn, Context.BIND_AUTO_CREATE);

        //开启定时器
        Timer timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(DRAW_MAP);

            }
        };
        timer.schedule(timerTask,1000,100);

        super.onStart();
    }

    /***
     * 读取测绘路径文件
     */
    public void readMappingFromSD(){
       new Thread(){
           @Override
           public void run() {

               if(isSelectRobotId !=-1){

                   for (int i=0;i<workRobotList.size();i++){

                       if(isSelectRobotId == workRobotList.get(i).heatDataMsg.robotId){
                           String orchardName = workRobotList.get(i).workMatch.orchardName;
                           //文件路径
                           String filedir = "Tank" + File.separator + orchardName + File.separator + "mapping";
                           File[] files = getMappingList(filedir);
                           int[] len=new int[2];

                           if(files != null ) {
                               //打开所有路径文件
                               for(int k=0;k<files.length;k++) {

                                   GpsPoint bpoint = new GpsPoint();

                                   ArrayList<MappingGroup> gpslist = new ArrayList<MappingGroup>();

                                   getMappingHead(filedir + File.separator + files[k].getName(), len, bpoint);

                                   getMappingData(filedir + File.separator + files[k].getName(), gpslist, len[0]);

                                   //当前默认只有一个主干道
                                   //   Log.d(TAG, files[0].getName() +" 帧长："+len[0]+" 基站坐标："+bpoint.x+" "+bpoint.y+ "测绘点个数："+ gpslist.size()+"\n");
                                   ArrayList<GpsPoint> pointList = new ArrayList<GpsPoint>();

                                   for (int j = 0; j < gpslist.size(); j++) {
                                       GpsPoint point=new GpsPoint();
                                       //转化为画布坐标
                                       point.x  = screenPoint.x / 2+ ((((double)gpslist.get(j).longitude/MappingGroup.INM_LON_LAT_SCALE)*180/MappingGroup.PI)* Math.cos(gpslist.get(j).latitude/MappingGroup.INM_LON_LAT_SCALE) - bpoint.x * Math.cos(bpoint.y * Math.PI / 180)) * ((double) screenPoint.x * GPS_DIS / MAPMAX_DIS);
                                       //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                                       point.y = screenPoint.y / 2 + (bpoint.y - ((double)gpslist.get(j).latitude/MappingGroup.INM_LON_LAT_SCALE)*180/MappingGroup.PI) * ((double) screenPoint.y * GPS_DIS / MAPMAX_DIS);

                                       pointList.add(point);

                                       //      Log.d(TAG, "测绘点:"+point.x +" "+point.y+" "+point.z);

                                       //     Log.d(TAG, "RTK状态：" + gpslist.get(j).rtkState + " 经度：" + gpslist.get(j).longitude + " 纬度：" + gpslist.get(j).latitude + " 海拔：" + gpslist.get(j).altitude + " roll：" + gpslist.get(j).roll
                                       //             + " pitch：" + gpslist.get(j).pitch + " 方向：" + gpslist.get(j).yaw + " 周：" + gpslist.get(j).GPSTime_weeks + " 时间：" + gpslist.get(j).GPSTime_ms);

                                   }
                                   routeList.add(pointList);
                                   routeNameList.add(files[k].getName());
                                   Log.d(TAG,"WorkMapActivity路径文件名："+files[k].getName());
                               }

                           }
                       }
                   }
               }

               super.run();
           }
       };


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

            //获取在线的机器人
            workRobotList = binder.getRobotList();

            for(int i=0;i<workRobotList.size();i++){
                if(workRobotList.get(i).heatDataMsg.robotId == isSelectRobotId){
                    Log.d(TAG,"当前机器人ID:"+isSelectRobotId+" 匹配果园："+workRobotList.get(i).workMatch.orchardName);
                    break;
                }
            }

            //设置回调
            binder.getService().setRobotWorkingCallback(new BLEService.RobotWorkingCallback() {
                @Override
                public void RobotStateChanged(TankRobot tankRobot) {
                    if(isSelectRobotId == tankRobot.heatDataMsg.robotId){
                        if(tankRobot.checkCount > CenterControlActivity.ROBOT_OFFLINE_CNT  ||
                                (tankRobot.heatDataMsg.rtkState & 0x03) != 0x03){//掉线

                          handler.sendEmptyMessage(ROBOT_UCONNECT);

                        }else{
                            isWorkRobot = tankRobot;
                            handler.sendEmptyMessage(BLE_DATA_ON);
                        }
                    }
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
