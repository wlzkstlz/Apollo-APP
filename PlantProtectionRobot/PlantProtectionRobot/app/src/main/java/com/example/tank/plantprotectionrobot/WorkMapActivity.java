package com.example.tank.plantprotectionrobot;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.CenterControlActivity;
import com.example.tank.plantprotectionrobot.ChoicePage.MySpinnerAdapter;
import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.R;
import com.example.tank.plantprotectionrobot.Robot.CommondType;
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
public class WorkMapActivity extends AppCompatActivity implements View.OnTouchListener{

    private Button centerBtn;
    private Button startBtn;
    private TextView batteryText;    //电量
    private TextView tankLevelText;  //药量
    private TextView robotMsgText;   //机器人状态
    private TextView zoomText;     //比例尺
    private LinearLayout backgroundAlarm;

    //地图相关
    private WorkMapView workMapView;
    private Button mapZoomUp;
    private Button mapZoomDown;


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
    private ArrayList<ArrayList<GpsPoint>> routeList_M = new ArrayList<ArrayList<GpsPoint>>();//主干道
    private ArrayList<ArrayList<GpsPoint>> routeList_L = new ArrayList<ArrayList<GpsPoint>>();                  //果园

    private ArrayList<String> routeNameList = new ArrayList<String>();
    private ArrayList<Integer> matchFlagList = new ArrayList<Integer>();
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

    //地图相关
    private GpsPoint basicPosition = new GpsPoint();   //基站位置
    private GpsPoint movePoint = new GpsPoint();        //平移坐标
    private GpsPoint robotPosition =new GpsPoint();    //机器人位置
    private GpsPoint personPosition =new GpsPoint();   //操作员位置
    private  int mapRatio;                 //地图放大系数
    private  int mapRatioZoom;            //地图缩放等级
    private final int MAPRATIO_MIN=1;
    private final int MAPRATIO_MAX = 9;//地图放大有10个等级
    private final int MAPRATIOZOOM_DEFAULT=4;


    private boolean mapFirstInit=true;//第一次初始化，获取画布尺寸


    //高德地图
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    private boolean locatonFlag;  //权限获取标志1表示获取位置权限成功


    //手势控制相关
    private PointF startPoint = new PointF(); //手指按下的坐标

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_map);
        workMapView = (WorkMapView)findViewById(R.id.workMapView);
        backgroundAlarm = (LinearLayout)findViewById(R.id.backgroundAlarm);
        batteryText=(TextView)findViewById(R.id.battery);
        tankLevelText=(TextView)findViewById(R.id.tankLevel);
        robotMsgText =(TextView)findViewById(R.id.robotMsg);
        zoomText = (TextView)findViewById(R.id.zoomText);

        mapZoomUp=(Button)findViewById(R.id.mapZoomUp);
        mapZoomDown=(Button)findViewById(R.id.mapZoomDown);
        centerBtn = (Button)findViewById(R.id.button1);
        startBtn = (Button)findViewById(R.id.button2);
        spinner1=(Spinner) findViewById(R.id.spinner1);


       //控制中心界面
       centerCtr = new Intent(this,CenterControlActivity.class);
       intentSev= new Intent(this,BLEService.class);

        setOnClick();
        initData();

        //初始化高德地图
        initAmap();

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

        workMapView.setRobotAndPersonPosition(robotPosition,personPosition);
        workMapView.drawMatchRoute(routeList_L,matchFlagList);
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

                switch (i){
                    case 0:
                        //自动驾驶
                        for(int j=0;j<workRobotList.size();j++){
                            if(workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId){
                                workRobotList.get(j).heatDataMsg.command = CommondType.CMD_AUTO;
                                break;
                            }
                        }

                        break;
                    case 1:
                        //手动控制
                        for(int j=0;j<workRobotList.size();j++){
                            if(workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId){
                                workRobotList.get(j).heatDataMsg.command = CommondType.CMD_MANUAL;
                                break;
                            }
                        }
                        break;
                    case 2:
                        //手动转场
                        for(int j=0;j<workRobotList.size();j++){
                            if(workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId){
                                workRobotList.get(j).heatDataMsg.command = CommondType.CMD_TRANSITION;
                                break;
                            }
                        }
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mapZoomUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mapRatioZoom < MAPRATIO_MAX){
                    mapRatioZoom++;
                    transformationPosition();

                    if(screenPoint.x > 0) {

                        movePoint.x = (1 - mapRatio) * (robotPosition.x);
                        movePoint.y = (1 - mapRatio) * (robotPosition.y);
                    }
                }

            }
        });

        mapZoomDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mapRatioZoom > MAPRATIO_MIN){
                    mapRatioZoom--;
                    transformationPosition();
                }
            }
        });

        //测量控件高度监听函数
        workMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            //此函数只要WorkMapView按键事件或其它变化就会被触发
            @Override
            public void onGlobalLayout() {
                //       Log.e("Tank001", mapView.getMeasuredWidth() + "==" + mapView.getMeasuredHeight());
                screenPoint.x = workMapView.getMeasuredWidth();
                screenPoint.y=   workMapView.getMeasuredHeight();

                //加载主干道数据
                if(mapFirstInit == true) {
                    mapFirstInit = false;
                    //读取路径文件

                    mapFirstInit  = false;
                    movePoint.set((1-mapRatio)*screenPoint.x/2,(1-mapRatio)*screenPoint.y/2,0,0);
                  //  mvPointO.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);

                //    basicPosition.x=-1000;
                //    basicPosition.y=-1000;
                    workMapView.InitWorkMapView(mapRatio,movePoint,basicPosition);
                    robotPosition.x = screenPoint.x;
                    robotPosition.y = screenPoint.y;
                    //平移限制
                    if (movePoint.x < screenPoint.x*(1 - mapRatio)) {
                        movePoint.x = screenPoint.x*(1 - mapRatio);
                    } else if (movePoint.x > 0) {
                        movePoint.x = 0;
                    }
                    if (movePoint.y < screenPoint.y * (1 - mapRatio)) {
                        movePoint.y = screenPoint.y * (1 - mapRatio);
                    } else if (movePoint.y > 0) {
                        movePoint.y = 0;
                    }
                }

            }
        });

    }

    /***
     * 9个缩放等级，缩放等级转为放大倍数
     */
    public void transformationPosition(){
        switch(mapRatioZoom ){
            case 1:
                mapRatio=1;
                break;
            case 2:
                mapRatio=5;
                break;
            case 3:
                mapRatio=10;
                break;
            case 4:
                mapRatio=25;
                break;
            case 5:
                mapRatio=50;
                break;
            case 6:
                mapRatio=100;
                break;
            case 7:
                mapRatio=250;
                break;
            case 8:
                mapRatio=500;
                break;
            case 9:
                mapRatio=1000;
                break;

        }

        zoomText.setText(""+MAPMAX_DIS/mapRatio+"米");
    }

    /***
     * 手势控制
     * @param v
     * @param event
     * @return
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //手势监听
        switch (event.getAction() & event.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:           // 手指按下事件
                startPoint.set(event.getX(),event.getY());
                break;
            case MotionEvent.ACTION_MOVE:          //滑动
                movePoint.x = movePoint.x + event.getX() - startPoint.x;
                movePoint.y = movePoint.y + event.getY() - startPoint.y;

                startPoint.x = event.getX();
                startPoint.y = event.getY();

                if (movePoint.x < screenPoint.x*(1 - mapRatio)) {
                    movePoint.x = screenPoint.x*(1 - mapRatio);
                } else if (movePoint.x > 0) {
                    movePoint.x = 0;
                }
                if (movePoint.y < screenPoint.y * (1 - mapRatio)) {
                    movePoint.y = screenPoint.y * (1 - mapRatio);
                } else if (movePoint.y > 0) {
                    movePoint.y = 0;
                }

                break;

        }
        return true;
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

        mapRatioZoom = MAPRATIOZOOM_DEFAULT;
        transformationPosition();

        //初始位置
        robotPosition.x=-1000;
        robotPosition.y =-1000;
        personPosition.x=-1000;
        personPosition.y=-1000;


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
                                       point.x  = screenPoint.x / 2+ ((((double)gpslist.get(j).longitude/MappingGroup.INM_LON_LAT_SCALE)*180/MappingGroup.PI)* Math.cos(gpslist.get(j).latitude/MappingGroup.INM_LON_LAT_SCALE) - bpoint.x * Math.cos(bpoint.y * Math.PI / 180)) * (screenPoint.x * GPS_DIS / MAPMAX_DIS);
                                       //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                                       point.y = screenPoint.y / 2 + (bpoint.y - ((double)gpslist.get(j).latitude/MappingGroup.INM_LON_LAT_SCALE)*180/MappingGroup.PI) * (screenPoint.y * GPS_DIS / MAPMAX_DIS);

                                       pointList.add(point);

                                  //     if(j<20) {
                                   //        Log.d(TAG, "测绘点:" + gpslist.get(j).longitude + " " + gpslist.get(j).latitude);
                                  //     }


                                       //     Log.d(TAG, "RTK状态：" + gpslist.get(j).rtkState + " 经度：" + gpslist.get(j).longitude + " 纬度：" + gpslist.get(j).latitude + " 海拔：" + gpslist.get(j).altitude + " roll：" + gpslist.get(j).roll
                                       //             + " pitch：" + gpslist.get(j).pitch + " 方向：" + gpslist.get(j).yaw + " 周：" + gpslist.get(j).GPSTime_weeks + " 时间：" + gpslist.get(j).GPSTime_ms);

                                   }

                                   if(files[k].getName().indexOf("L_") !=-1) {
                                       routeList_L.add(pointList);//果园
                                       routeNameList.add(files[k].getName());

                                       for(int p=0;p<workRobotList.size();p++){//标记匹配果园路径
                                           if(workRobotList.get(p).workMatch.equals(files[k].getName())){
                                               matchFlagList.add(1);//表示已匹配
                                               Log.d(TAG,"WorkMapActivity路径文件->"+files[k].getName()+"已匹配");
                                           }else{
                                               matchFlagList.add(0);//表示未匹配
                                               Log.d(TAG,"WorkMapActivity路径文件->"+files[k].getName()+"未匹配");
                                           }

                                       }

                                   }else{
                                       routeList_M.add(pointList);//主干道
                                   }
                                   Log.d(TAG,"WorkMapActivity路径文件名："+files[k].getName());
                               }

                           }
                       }
                   }
               }

               super.run();
           }
       }.start();


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
                    workRobotList.get(i).inWorkPage = true;
                    binder.setPollingFragment();//进入控制界面，分配时间碎片
                    break;
                }
            }

            //读取路径文件
            readMappingFromSD();

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

                 //       robotPosition.x = tankRobot.heatDataMsg.poseLongitude;
                //        robotPosition.y = tankRobot.heatDataMsg.poseLatitude;

                        //坐标转换
                        robotPosition.x  = screenPoint.x / 2+ ((((double)tankRobot.heatDataMsg.poseLongitude/MappingGroup.INM_LON_LAT_SCALE)*180/MappingGroup.PI)* Math.cos(tankRobot.heatDataMsg.poseLatitude/MappingGroup.INM_LON_LAT_SCALE) - basicPosition.x * Math.cos(basicPosition.y * Math.PI / 180)) * (screenPoint.x * GPS_DIS / MAPMAX_DIS);
                        //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                        robotPosition.y = screenPoint.y / 2 + (basicPosition.y - ((double)tankRobot.heatDataMsg.poseLatitude/MappingGroup.INM_LON_LAT_SCALE)*180/MappingGroup.PI) * (screenPoint.y * GPS_DIS / MAPMAX_DIS);


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


    /***
     * 初始高德地图
     */
    private void initAmap(){
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

        //连续定位，该方法默认为false。
        mLocationOption.setOnceLocation(false);
        //获取最近3s内精度最高的一次定位结果：
        mLocationOption.setOnceLocationLatest(true);
        //连续定位事件间隔
        mLocationOption.setInterval(1000);

        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(false);
        //设置是否允许模拟位置,默认为true，允许模拟位置
        mLocationOption.setMockEnable(true);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();

    }
    //高德地图回调
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {

            if (aMapLocation != null) {

                if (aMapLocation.getErrorCode() == 0) {

                    String str="";
                    str += aMapLocation.getLatitude();//获取纬度
                    str +="\n";
                    str +=aMapLocation.getLongitude();//获取经度
                    str +="\n";

                    Log.d(TAG,str);
                    //坐标转换
                    personPosition.x  = screenPoint.x / 2+ ((aMapLocation.getLongitude()*180/MappingGroup.PI)* Math.cos( aMapLocation.getLatitude()) - basicPosition.x * Math.cos(basicPosition.y * Math.PI / 180)) * (screenPoint.x * GPS_DIS / MAPMAX_DIS);
                    //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                    personPosition.y = screenPoint.y / 2 + (basicPosition.y - aMapLocation.getLatitude()*180/MappingGroup.PI) * (screenPoint.y * GPS_DIS / MAPMAX_DIS);

                }else {
                    //   Toast.makeText(getApplicationContext(),"定位失败",
                    //         Toast.LENGTH_SHORT).show();

                }
            }
        }
    };
}
