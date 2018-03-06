package com.example.tank.plantprotectionrobot;

import android.app.ProgressDialog;
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
import android.support.v7.app.AlertDialog;
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
import com.example.tank.plantprotectionrobot.Robot.HeatDataMsg;
import com.example.tank.plantprotectionrobot.Robot.TankRobot;
import com.example.tank.plantprotectionrobot.Robot.WorkMatch;
import com.example.tank.plantprotectionrobot.WaveFiltering.RobotCruisePath;

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
    private ProgressDialog progressDialog;//等待对话框

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
    //

    private  int matchRouteId =-1;//匹配果园路径序号
    private ArrayList<Integer> matchFlagList = new ArrayList<Integer>();
    private int isSelectRobotId; //当前控制的机器人ID
    private TankRobot isWorkRobot=null;//当前监控的机器人

    private boolean satrtTransfeRoute = false;
    private boolean sendFailConnectHandle = false;//传输文件失败重新连接 标志

    //路径文件组
    private ArrayList<ArrayList<GpsPoint>> routeList_M = new ArrayList<ArrayList<GpsPoint>>();//主干道
    private ArrayList<ArrayList<GpsPoint>> routeList_L = new ArrayList<ArrayList<GpsPoint>>();//果园
    private ArrayList<RobotCruisePath> robotCruisePaths = new ArrayList<RobotCruisePath>();//果园数据
    private GpsPoint screenPoint= new GpsPoint(); //获取屏幕的大小
    private final double NEAR_DIS = 0.5;//判断点是否重合距离，单位米
    private final int MAPMAX_DIS = 5000;//地图最大距离单位米
    private final int GPS_DIS = 111000;//纬度1度的距离，单位米

    //地图相关
    private GpsPoint basicPosition = new GpsPoint();   //基站位置
    private GpsPoint movePoint = new GpsPoint();        //平移坐标
    private GpsPoint robotPosition =new GpsPoint();    //机器人位置
    private GpsPoint personPosition =new GpsPoint();   //操作员位置
    private  int mapRatio;                 //地图放大系数
    private  int mapRatioZoom;            //地图缩放等级
    private final int MAPRATIO_MIN=1;
    private final int MAPRATIO_MAX = 9;//地图放大有10个等级
    private final int MAPRATIOZOOM_DEFAULT=1;

    private final int TANKLEVEL_MIN=5;
    private final int BATTERY_MIN =10;

   //定时器，用于更新地图
    private TimerTask timerTask;
    private Handler handler;

    //handle消息
    private final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    private final int BLE_CONNECT_OFF =21;//蓝牙连接失败
    private final int BLE_ROBOTBLE_SCAN_OFF = 22;//机器人蓝牙搜索超时
    private final int BLE_DATA_ON = 30; //接收到数据
    private final int ROBOT_UCONNECT =40;//RTK掉线
    private final int COMD_NO_RETURN = 50; //发送指令无返回
    private final int COMD_CHOICE_DELAY= 60;//重新选择后等待一段时间在更新界面
    private final int COMD_BLE_WAIT_OFF= 70;//WAIT指令发送超时
    private final int COMD_BLE_START_OFF= 71;//START_指令发送超时
    private final int COMD_BLE_END_OFF= 72;//END指令发送超时
    private final int SEND_ROUTE_DATA_FAIL =90;//连接上算法板蓝牙
    private final int SEND_ROUTE_SUCCESS = 100;//文件传输成功
    private final int DRAW_MAP =1;

    private boolean robotBleConnectAgain=false;//机器人蓝牙掉线重连标志，在没有连接到串口时断开连接自动重连

    private final  String TAG = "Tank001";



    //手势控制相关
    private PointF startPoint = new PointF(); //手指按下的坐标
    private boolean mapFirstInit=true;//第一次初始化，获取画布尺寸
    private boolean ctrSpinnerUpdate = true;//第一次进入，防止自己选择
    private boolean changeComd =false;

    //音乐震动控制
    private VibrationAndMusic vibrationAndMusic;

    //高德地图
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    private boolean locatonFlag;  //权限获取标志1表示获取位置权限成功

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

        startBtn.setVisibility(View.INVISIBLE);
        workMapView.setOnTouchListener(this);


       //控制中心界面
       centerCtr = new Intent(this,CenterControlActivity.class);
       intentSev= new Intent(this,BLEService.class);

        setOnClick();
        initData();

        //初始化高德地图
        initAmap();

        initProgressDialog();
        vibrationAndMusic = new VibrationAndMusic(WorkMapActivity.this);

        //消息处理
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                AlertDialog.Builder msgDialog =
                        new AlertDialog.Builder(WorkMapActivity.this);

                switch(msg.what){
                    case DRAW_MAP:
                        drawMap();
                        break;
                    case ROBOT_UCONNECT:
                        updateData(false);
                        break;
                    case COMD_NO_RETURN:
                        updateData(false);
                        break;
                    case COMD_CHOICE_DELAY:
                        changeComd = false;
                        break;
                    case BLE_ROBOTBLE_SCAN_OFF:
                        break;
                    case BLE_DATA_ON:
                        if(changeComd == false) {
                            updateData(true);
                        }
                        translateChangeMsg();
                        break;

                    case SEND_ROUTE_DATA_FAIL:

                        progressDialog.cancel();
                        sendFailConnectHandle = true;
                        robotBleConnectAgain = false;

                        msgDialog.setTitle("提示");
                        msgDialog.setMessage("文件传输失败");
                        msgDialog.show();

                        startBtn.setVisibility(View.VISIBLE);
                        startBtn.setEnabled(false);
                        spinner1.setVisibility(View.INVISIBLE);

                        binder.IntoWorkMapPage(true);
                        for (int i = 0; i < workRobotList.size(); i++) {
                            if (isSelectRobotId == workRobotList.get(i).heatDataMsg.robotId) {
                                workRobotList.get(i).inWorkPage = true;
                                workRobotList.get(i).heatDataMsg.command = CommondType.CMD_HEARTBEAT;
                            }

                        }
                        satrtTransfeRoute =false;

                        //连接失败返回到遥控器蓝牙
                        if(binder != null){
                            if(binder.getIsWorkingId() != BLEService.BLE_HANDLE_CONECT) {
                                binder.setBleWorkTpye(BLEService.BLE_HANDLE_CONECT,false);
                                binder.startScanBle();
                            }
                        }

                        break;
                    case COMD_BLE_WAIT_OFF://wait指令发送超时，不用管，继续发送CMD_BLE_START指令

                        for (int i = 0; i < workRobotList.size(); i++) {

                            if (workRobotList.get(i).heatDataMsg.robotId == isSelectRobotId) {
                                workRobotList.get(i).heatDataMsg.command = CommondType.CMD_BLE_START;
                                handler.sendEmptyMessageDelayed(COMD_BLE_START_OFF,2000);//装载超时
                            }
                        }
                        break;
                    case COMD_BLE_START_OFF:

                        if(binder !=null) {
                            binder.setBleWorkTpye(BLEService.BLE_ROBOT_CONECT,false);
                            binder.startScanBle();
                            robotBleConnectAgain = true;

                        }
                        break;
                    case COMD_BLE_END_OFF:

                        progressDialog.cancel();//发送end超时默认已经传输成功

                        for (int i = 0; i < workRobotList.size(); i++) {

                            if (workRobotList.get(i).heatDataMsg.robotId == isSelectRobotId) {
                                workRobotList.get(i).heatDataMsg.command = CommondType.CMD_HEARTBEAT;
                            }
                        }
                        break;
                    case SEND_ROUTE_SUCCESS:
                        progressDialog.cancel();

                        for (int i = 0; i < workRobotList.size(); i++) {

                            if (workRobotList.get(i).heatDataMsg.robotId == isSelectRobotId) {
                                workRobotList.get(i).workMatch.isMatch=true;
                            }
                        }
                        break;

                    case  BLE_CONNECT_ON:

                        if(binder.getIsWorkingId() == BLEService.BLE_ROBOT_CONECT) {

                            robotBleConnectAgain = false;

                            if (binder != null) {//连接算法板蓝牙
                                if (binder.getIsWorkingId() == BLEService.BLE_ROBOT_CONECT) {

                                    //测试
                                    matchRouteId=0;
                                    //开始发送数据
                                    if (matchRouteId >= 0 && matchRouteId < robotCruisePaths.size()) {
                                        binder.sendRouteData(robotCruisePaths.get(matchRouteId));
                                    }
                                }
                            }
                        }else if(binder.getIsWorkingId() == BLEService.BLE_HANDLE_CONECT){

                           if(sendFailConnectHandle == true) {//发送失败重新连接
                               sendFailConnectHandle = false;
                               startBtn.setEnabled(true);//重新连接成功啦才能再次传输文件

                           }
                            if(satrtTransfeRoute == true) {

                                binder.IntoWorkMapPage(true);
                                for (int i = 0; i < workRobotList.size(); i++) {
                                    if (isSelectRobotId == workRobotList.get(i).heatDataMsg.robotId) {
                                        workRobotList.get(i).inWorkPage = true;
                                        workRobotList.get(i).heatDataMsg.command = CommondType.CMD_BLE_END;
                                        handler.sendEmptyMessageDelayed(COMD_BLE_END_OFF,2000);
                                    }

                                }

                                satrtTransfeRoute =false;
                            }
                        }

                        break;

                }
            }
        };

    }

    /***
     * 机器人返回信息处理
     */
    private void translateChangeMsg(){

        if(isSelectRobotId == isWorkRobot.heatDataMsg.robotId){

            //坐标转换
            if(robotCruisePaths.size()>0) {
                robotPosition.x = screenPoint.x / 2 + ((((double) isWorkRobot.heatDataMsg.poseLongitude / MappingGroup.INM_LON_LAT_SCALE) * 180 / MappingGroup.PI) * Math.cos(isWorkRobot.heatDataMsg.poseLatitude / MappingGroup.INM_LON_LAT_SCALE) - robotCruisePaths.get(0).bPoint.x * Math.cos(robotCruisePaths.get(0).bPoint.y * Math.PI / 180)) * (screenPoint.x * GPS_DIS / MAPMAX_DIS);
                //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                robotPosition.y = screenPoint.y / 2 + (robotCruisePaths.get(0).bPoint.y - ((double) isWorkRobot.heatDataMsg.poseLatitude / MappingGroup.INM_LON_LAT_SCALE) * 180 / MappingGroup.PI) * (screenPoint.y * GPS_DIS / MAPMAX_DIS);
                //判断是否与起点重合
           //     Log.d(TAG, "当前位置：X=" + robotPosition.x + " Y=" + robotPosition.y);
            }
            //跟踪进度
            if(isWorkRobot.workMatch.isMatch == true ){

                if(isWorkRobot.workMatch.taskCompleted/isWorkRobot.workMatch.matchroute.size() > 1 && isWorkRobot.workMatch.taskCompleted>1) {
                    for (int k = isWorkRobot.workMatch.taskCompleted-1; k < isWorkRobot.workMatch.matchroute.size(); k++) {
                        if (Math.abs(robotPosition.x - isWorkRobot.workMatch.matchroute.get(k).x) < (NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                                && Math.abs(robotPosition.y - isWorkRobot.workMatch.matchroute.get(k).y) < (NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {
                            if (workRobotList.get(k).workMatch.isMatch == false) {//重合点
                                isWorkRobot.workMatch.taskCompleted = k;
                                break;
                            }
                        }
                    }
                }else{
                    for (int k = 0; k < isWorkRobot.workMatch.matchroute.size(); k++) {
                        if (Math.abs(robotPosition.x - isWorkRobot.workMatch.matchroute.get(k).x) < (NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                                && Math.abs(robotPosition.y - isWorkRobot.workMatch.matchroute.get(k).y) < (NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {
                            if (workRobotList.get(k).workMatch.isMatch == false) {//重合点
                                isWorkRobot.workMatch.taskCompleted = k;
                                break;
                            }
                        }
                    }
                }
            }
            //匹配起点
            for(int k=0;k<routeList_L.size();k++){

                if(Math.abs(robotPosition.x - routeList_L.get(k).get(0).x) < (NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                        && Math.abs(robotPosition.y - routeList_L.get(k).get(0).y) < (NEAR_DIS * screenPoint.y / MAPMAX_DIS) ){
                    if(workRobotList.get(k).workMatch.isMatch == false){
                        //-------------------//
                        //与起点重合可以开始传文件
                        //  startBtn.setEnabled(true);
                     //   startBtn.setBackgroundColor(getResources().getColor(R.color.tankgreen));
                        matchRouteId = k;

                        //手机振动
                        if(vibrationAndMusic.getVibrate() == false) {
                            vibrationAndMusic.Vibrate(new long[]{500, 1000, 500, 1000}, true);
                        }

                    }
                }else{
                    matchRouteId = -1;
                    //手机振动
                    if(vibrationAndMusic.getVibrate()) {
                        vibrationAndMusic.stopVibration();
                    }
                    //  startBtn.setEnabled(false);
                  //  startBtn.setBackgroundColor(getResources().getColor(R.color.colorGray));
                }
            }
        }
    }

    private void initProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("提示");
        progressDialog.setMessage("正在传输路径文件");
        progressDialog.setCancelable(false);
    }
    /***
     * 轮序获取的数据，更新显示
     */
    private void updateData(boolean onLine){

        if(onLine == true) {
            if (isWorkRobot != null) {
                String state = "";
                tankLevelText.setText("药量 " + isWorkRobot.heatDataMsg.tankLevel + "%");
                batteryText.setText("电量 " + isWorkRobot.heatDataMsg.batteryPercentage + "%");
                if (isWorkRobot.heatDataMsg.curState < 100) {
                    state += "作业中";
                } else {
                    state += "完成";

                }
                backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.tankgreen));
                if (isWorkRobot.heatDataMsg.tankLevel < TANKLEVEL_MIN) {
                    state += "|加药";
                    backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorRed));
                }
                if (isWorkRobot.heatDataMsg.batteryPercentage < BATTERY_MIN) {
                    state += "|换电";
                    backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorRed));
                }
                if (isWorkRobot.heatDataMsg.dAlarm == true) {
                    state += "|救援";
                    backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorRed));
                }
                robotMsgText.setText(state);

                spinner1.setEnabled(true);//
                spinner1.setSelection(isWorkRobot.workAuto);


            }
        }else{
            tankLevelText.setText("药量 --");
            batteryText.setText("电量 --");
            robotMsgText.setText("离线");
            backgroundAlarm.setBackgroundColor(getResources().getColor(R.color.colorGray));
            spinner1.setEnabled(false);//禁用
        }
    }
    /***
     * 重绘地图
     */
    private void drawMap(){

        workMapView.setRobotAndPersonPosition(robotPosition,personPosition, movePoint,mapRatio);
        /*
        if(isWorkRobot.workMatch.isMatch == true) {//绘制正在作业路径
            workMapView.drawWorkRoute(isWorkRobot.workMatch.matchroute, routeList_M, isWorkRobot.workMatch.taskCompleted);
        }else{//绘制匹配模式下所有路径
            workMapView.drawMatchRoute(routeList_L, routeList_M, matchFlagList);
        }*/
        workMapView.drawMatchRoute(routeList_L, routeList_M, matchFlagList);
      //  Log.d(TAG,"画地图\n");
    }
    private void setOnClick() {
        centerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binder !=null){
                    binder.IntoWorkMapPage(false);
                }
                startActivity(centerCtr);
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBtn.setVisibility(View.INVISIBLE);
                spinner1.setVisibility(View.VISIBLE);

                //准备传输路径文件，向所有机器发送等待指令

                satrtTransfeRoute = true;
                for (int j = 0; j < workRobotList.size(); j++) {
                    binder.IntoWorkMapPage(false);//取消分时轮询多分配的时间
                    workRobotList.get(j).heatDataMsg.command = CommondType.CMD_WAIT;

                    if(workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId){
                        if(matchRouteId>0 && matchRouteId<robotCruisePaths.size()) {//匹配的果园路径名

                            workRobotList.get(j).workMatch.routeName = robotCruisePaths.get(j).mFileName;
                        }
                    }
                }
                //发送等待指令超时
                handler.sendEmptyMessageDelayed(COMD_BLE_WAIT_OFF,2000);
                //停止震动
                if(vibrationAndMusic.getVibrate()){
                    vibrationAndMusic.stopVibration();
                }
                progressDialog.show();

            }
        });
        //控制选择
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                if(ctrSpinnerUpdate == false) {
                    switch (i) {
                        case 0:
                            //自动驾驶
                            for (int j = 0; j < workRobotList.size(); j++) {
                                if (workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId) {

                                    if(workRobotList.get(j).heatDataMsg.taskFile == true) {

                                        workRobotList.get(j).heatDataMsg.command = CommondType.CMD_AUTO;
                                        Log.d(TAG, "WorkMapActivity->进入=" + workRobotList.get(j).heatDataMsg.command);
                                        changeComd = true;
                                        handler.sendEmptyMessageDelayed(COMD_CHOICE_DELAY, 1000);//等待一段时间后才刷新界面

                                    }else{
                                        startBtn.setVisibility(View.VISIBLE);
                                        spinner1.setVisibility(View.INVISIBLE);

                                        AlertDialog.Builder msgDialog =
                                                new AlertDialog.Builder(WorkMapActivity.this);

                                        msgDialog.setTitle("提示");
                                        msgDialog.setMessage("该机器人未匹配路径");
                                        msgDialog.show();
                                    }
                                    break;
                                }
                            }

                            break;
                        case 1:
                            //手动控制
                            for (int j = 0; j < workRobotList.size(); j++) {
                                if (workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId) {
                                    workRobotList.get(j).heatDataMsg.command = CommondType.CMD_MANUAL;
                                    Log.d(TAG,"WorkMapActivity->进入="+workRobotList.get(j).heatDataMsg.command);
                                    changeComd = true;
                                    handler.sendEmptyMessageDelayed(COMD_CHOICE_DELAY, 1000);//等待一段时间后才刷新界面
                                    break;
                                }
                            }
                            break;
                        case 2:
                            //手动转场
                            for (int j = 0; j < workRobotList.size(); j++) {
                                if (workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId) {
                                    workRobotList.get(j).heatDataMsg.command = CommondType.CMD_TRANSITION;
                                    Log.d(TAG,"WorkMapActivity->进入="+workRobotList.get(j).heatDataMsg.command);
                                    workRobotList.get(j).heatDataMsg.taskFile =false;//转场后消除路径文件标志
                                    workRobotList.get(j).workMatch.routeName = "";
                                    changeComd = true;
                                    handler.sendEmptyMessageDelayed(COMD_CHOICE_DELAY, 1000);//等待一段时间后才刷新界面
                                    break;
                                }
                            }
                            break;
                        case 3:
                            for (int j = 0; j < workRobotList.size(); j++) {
                                if (workRobotList.get(j).heatDataMsg.robotId == isSelectRobotId) {
                                    workRobotList.get(j).heatDataMsg.command = CommondType.CMD_SUPPLY;
                                    changeComd = true;
                                    handler.sendEmptyMessageDelayed(COMD_CHOICE_DELAY, 1000);//等待一段时间后才刷新界面
                                    break;
                                }
                            }
                            break;

                    }

                    //延时消息处理，若无回复就执行失败
                    handler.sendEmptyMessageDelayed(COMD_NO_RETURN,2000);

                }else {
                    ctrSpinnerUpdate = false;
                }

            }


            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d(TAG,"onNothingSelected触发");
             //
            }
        });

        mapZoomUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mapRatioZoom < MAPRATIO_MAX){
                    mapRatioZoom++;
                    transformationPosition();

                    if(screenPoint.x > 0) {

                        movePoint.x = (1 - mapRatio) * robotPosition.x;
                        movePoint.y = (1 - mapRatio) * robotPosition.y;

                    }

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

                //    Log.d(TAG,"倍数"+mapRatio+" 偏移X="+movePoint.x+" Y="+movePoint.y +" 位置X="+robotPosition.x+" Y="+robotPosition.y);

                }



            }
        });

        mapZoomDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mapRatioZoom > MAPRATIO_MIN){
                    mapRatioZoom--;
                    transformationPosition();
                    if(screenPoint.x > 0) {

                        movePoint.x = (1 - mapRatio) * robotPosition.x;
                        movePoint.y = (1 - mapRatio) * robotPosition.y;
                    }

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
                //    Log.d(TAG,"倍数"+mapRatio+" 偏移X="+movePoint.x+" Y="+movePoint.y +" 位置X="+robotPosition.x+" Y="+robotPosition.y);

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
            //    Log.d(TAG,"按下X="+startPoint.x+" Y="+startPoint.y);
                break;
            case MotionEvent.ACTION_MOVE:          //滑动

                movePoint.x = movePoint.x + event.getX() - startPoint.x;
                movePoint.y = movePoint.y + event.getY() - startPoint.y;
            //    Log.d(TAG,"移动X="+movePoint.x+" Y="+movePoint.y);

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

                                   RobotCruisePath robotCruisePath = new RobotCruisePath();
                                   robotCruisePath.Open(files[k].getName(),filedir);

                                   basicPosition.x=screenPoint.x / 2+robotCruisePath.bPoint.x;
                                   basicPosition.y=screenPoint.y / 2+robotCruisePath.bPoint.y;
                                   //当前默认只有一个主干道
                                   //   Log.d(TAG, files[0].getName() +" 帧长："+len[0]+" 基站坐标："+bpoint.x+" "+bpoint.y+ "测绘点个数："+ gpslist.size()+"\n");

                                   ArrayList<GpsPoint> pointList = new ArrayList<GpsPoint>();

                                   for (int j = 0; j < robotCruisePath.mPoints.size(); j++) {
                                       GpsPoint point=new GpsPoint();
                                       //转化为画布坐标
                                       point.x = screenPoint.x / 2 + robotCruisePath.mPoints.get(j).x * (screenPoint.x/ MAPMAX_DIS);
                                       point.y = screenPoint.y / 2 - robotCruisePath.mPoints.get(j).y * (screenPoint.y/ MAPMAX_DIS);
                                       pointList.add(point);
                                 //      Log.d(TAG,"测绘数据：X="+robotCruisePath.mPoints.get(j).x +"Y="+robotCruisePath.mPoints.get(j).y+"\n");
                                   }

                                   if(files[k].getName().indexOf("L_") != -1) {

                                       routeList_L.add(pointList);//果园
                                       robotCruisePaths.add(robotCruisePath);

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
                                       Log.d(TAG,"WorkMapActivity路径文件名："+files[k].getName());
                                   }

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
            binder.setBleWorkTpye(BLEService.BLE_HANDLE_CONECT,true);

            //获取在线的机器人
            workRobotList = binder.getRobotList();

            for(int i=0;i<workRobotList.size();i++){
                if(workRobotList.get(i).heatDataMsg.robotId == isSelectRobotId){
                    Log.d(TAG,"当前机器人ID:"+isSelectRobotId+" 匹配果园："+workRobotList.get(i).workMatch.orchardName);
                    workRobotList.get(i).inWorkPage = true;
                    binder.IntoWorkMapPage(true);//进入控制界面，分配时间碎片
                    break;
                }
            }

            //读取路径文件
            readMappingFromSD();

            //设置回调
            binder.getService().setRobotWorkingCallback(new BLEService.RobotWorkingCallback() {
                @Override
                public void RobotStateChanged(TankRobot tankRobot) {
                  //  translateChangeMsg(tankRobot);

                    if(isSelectRobotId == tankRobot.heatDataMsg.robotId){
                        //接收到信息，取消COMD_NO_RETURN消息
                        handler.removeMessages(COMD_NO_RETURN);
                        //处理消息
                        if(tankRobot.robotOnline == false  || (tankRobot.heatDataMsg.rtkState & 0x03) != 0x03){//掉线

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
                        case BLEService.BLE_SCAN_OFF:
                            if(binder.getIsWorkingId() == BLEService.BLE_ROBOT_CONECT) {
                                handler.sendEmptyMessage(SEND_ROUTE_DATA_FAIL);
                                Log.d(TAG,"BLEService.BLE_ROBOT_CONECT->没有搜索到蓝牙");
                            }else{
                                Log.d(TAG,"BLEService.BLE_HANDLE_CONECT->没有搜索到蓝牙");
                                binder.startScanBle();//继续搜索
                            }
                            break;
                        case BLEService.BLE_CONNECT_OFF:
                            //掉线重连
                            if(binder.getIsWorkingId() == BLEService.BLE_ROBOT_CONECT) {
                                if(robotBleConnectAgain == true){//还没连接到串口时掉线重连
                                    binder.connectBle(null, true);
                                }else {
                                    handler.sendEmptyMessage(SEND_ROUTE_DATA_FAIL);
                                }
                            }else{
                                binder.connectBle(null, true);
                            }
                            break;
                        case BLEService.BLE_CONNECT_ON:
                            handler.sendEmptyMessage( BLE_CONNECT_ON);

                            break;
                        case BLEService.BLE_CONNECTED:
                            break;
                        case BLEService.BLE_SEND_ROUTE_END:
                            if(binder !=null) {//数据发送完 转为连接遥控器蓝牙
                              binder.setBleWorkTpye(BLEService.BLE_HANDLE_CONECT,false);
                              binder.startScanBle();
                            }
                            break;
                    }
                }

                @Override
                public void ComdReturnChange(HeatDataMsg heatDataMsg) {

                    //准备开始传路径文件
                //    if(satrtTransfeRoute == true) {

                        if(heatDataMsg.command == CommondType.CMD_WAIT) {
                            int flag = 0;
                            for (int i = 0; i < workRobotList.size(); i++) {
                                if (workRobotList.get(i).heatDataMsg.command == CommondType.CMD_WAIT) {
                                    flag++;
                                }
                            }
                            //所有等待指令都发送完，开始发送CMD_BLE_START;
                            if (flag <= 1) {
                                for (int i = 0; i < workRobotList.size(); i++) {

                                    if (workRobotList.get(i).heatDataMsg.robotId == isSelectRobotId) {
                                        workRobotList.get(i).heatDataMsg.command = CommondType.CMD_BLE_START;

                                        handler.removeMessages(COMD_BLE_WAIT_OFF);//正常，取消超时
                                        handler.sendEmptyMessageDelayed(COMD_BLE_START_OFF,2000);

                                    }
                                }
                            }
                        }else  if(heatDataMsg.command == CommondType.CMD_BLE_START){//接收到返回指令 开始传输路径文件
                            //  binder.unconnectBle();//断开控制蓝牙
                            handler.removeMessages(COMD_BLE_START_OFF);//正常，取消超时
                            binder.setBleWorkTpye(BLEService.BLE_ROBOT_CONECT,false);
                            robotBleConnectAgain = true;
                            binder.startScanBle();
                            handler.sendEmptyMessageDelayed(SEND_ROUTE_DATA_FAIL,15000);

                        }else if(heatDataMsg.command == CommondType.CMD_BLE_END){

                            handler.removeMessages(COMD_BLE_END_OFF);//正常，取消超时

                             if(heatDataMsg.taskFile == true){
                                 handler.sendEmptyMessage(SEND_ROUTE_SUCCESS);
                             }else{
                                 handler.sendEmptyMessage(SEND_ROUTE_DATA_FAIL);
                            }
                        }
                  //  }
                }

                @Override
                public void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList) {

                    if(binder.getIsWorkingId() == BLEService.BLE_ROBOT_CONECT) {
                        String robotIdStr = "";
                        if (isWorkRobot.heatDataMsg.robotId < 10) {
                            robotIdStr = "R0000" + isWorkRobot.heatDataMsg.robotId;
                        } else if (isWorkRobot.heatDataMsg.robotId < 100) {
                            robotIdStr = "R000" + isWorkRobot.heatDataMsg.robotId;
                        } else if (isWorkRobot.heatDataMsg.robotId < 1000) {
                            robotIdStr = "R00" + isWorkRobot.heatDataMsg.robotId;
                        } else if (isWorkRobot.heatDataMsg.robotId < 10000) {
                            robotIdStr = "R0" + isWorkRobot.heatDataMsg.robotId;
                        } else {
                            robotIdStr = "R" + isWorkRobot.heatDataMsg.robotId;
                        }

                        for (int i = 0; i < mBleDeviceList.size(); i++) {

                            if (robotIdStr.equals(mBleDeviceList.get(i).getName())) {
                                handler.removeMessages(SEND_ROUTE_DATA_FAIL);
                                binder.connectBle(mBleDeviceList.get(i), false);//连接
                                break;
                            }
                            Log.d(TAG,"BLEService.BLE_ROBOT_CONECT->搜索到蓝牙"+mBleDeviceList.get(i).getName());
                        }
                    }else if(binder.getIsWorkingId() == BLEService.BLE_HANDLE_CONECT){
                        if (mBleDeviceList.size()>0) {
                            //连接蓝牙
                            binder.connectBle(mBleDeviceList.get(0),false);
                            //     Log.d(TAG,"ConnectActivity->连接蓝牙"+mBleDeviceList.get(0).getName());
                        }
                    }
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
                    if(robotCruisePaths.size()>0) {
                        personPosition.x = screenPoint.x / 2 + ((aMapLocation.getLongitude() * 180 / MappingGroup.PI) * Math.cos(aMapLocation.getLatitude()) - robotCruisePaths.get(0).bPoint.x * Math.cos(robotCruisePaths.get(0).bPoint.y * Math.PI / 180)) * (screenPoint.x * GPS_DIS / MAPMAX_DIS);
                        //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                        personPosition.y = screenPoint.y / 2 + (robotCruisePaths.get(0).bPoint.y - aMapLocation.getLatitude() * 180 / MappingGroup.PI) * (screenPoint.y * GPS_DIS / MAPMAX_DIS);
                    }
                }else {
                    //   Toast.makeText(getApplicationContext(),"定位失败",
                    //         Toast.LENGTH_SHORT).show();

                }
            }
        }
    };
}
