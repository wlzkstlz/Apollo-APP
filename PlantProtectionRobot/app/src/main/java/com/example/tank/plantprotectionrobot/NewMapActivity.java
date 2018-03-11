package com.example.tank.plantprotectionrobot;


import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.WaveFiltering.RobotCruisePath;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingData;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingHead;

import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.saveMappingDataAll;

import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.initLocationPermission;

import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getMappingList;


/*
 @新建地图测绘界面程序
 *由于程序需要按home件依然运行 又要处理和蓝牙Service的绑定和解除绑定，所以在3个地方处理与BLE的绑定关系
 * 1、按下退出件回到控制中心，2、back建触发，(都会调用onDestroy())3、保存路径文件回到连接界面 unbindService不起作用？

 */

public class NewMapActivity extends AppCompatActivity implements View.OnTouchListener{

    Button exitBtn;
    Button saveBtn;
    Button eraseBtn;
    Button moveCenterBtn;

    TextView textView1;//提示文本
    TextView textView2;//全屏比例尺
    LinearLayout linearLayout;//标题栏
    MapView mapView;

    private final  String TAG = "Tank001";

    //数据，此处保存当前打开的农场主名，即是农场主文件中名
    private SharedPreferences setinfo;
    private SharedPreferences.Editor infoEditor;
    private boolean locatonFlag;  //权限获取标志1表示获取位置权限成功


    private ArrayList<GpsPoint>  mListPointL; //当前测绘显示数据
    private ArrayList<GpsPoint>  mListPointM; //主干道测绘显示数据
    private  boolean detEnable =false; //擦除标志,true时可以擦除
    private int detIndex =0; //擦除点在array的位置

    private long  longTouch;  //长按判断计时
    private GpsPoint screenPoint; //获取屏幕的大小
    private int touchMode = 0;//手势检测标志1时表示有两个触点
    private int touchDet = 0;//手势检长按
    private PointF startPoint = new PointF(); //第一个手指按下是的坐标
    private PointF midPoint = new PointF();   //两个手指防线时的中点坐标

    //删除点
    private GpsPoint touchPoint = new GpsPoint();
    //擦除模式当前位置
    private GpsPoint psonPoint = new GpsPoint();//人当前位置，即是测绘杆当前位置
    private final double NEAR_DIS = 0.3;//判断点是否重合距离，单位米
    private final double VIRTUAL_DIS = 0.4;//虚拟点距离
    private final int MAPMAX_DIS = 5000;//地图最大距离单位米
    private final int GPS_DIS = 111000;//纬度1度的距离，单位米

    //路径滤波
    private RobotCruisePath robotCruisePath = new RobotCruisePath();
    private static  final float  DELTA_DIST = 0.5f; //过滤参数
    //基站坐标
    private GpsPoint bPoint = new GpsPoint();//弧度制
    private boolean firstInit = true;//第一次进程序


    //地图显示偏移坐标
    private GpsPoint mvPoint = new GpsPoint();
    private GpsPoint mvPointO = new GpsPoint();
    //比例尺参数
    private  float ratio = 100;
    private  float ratioO = 100;
    private final int RATIO_MIN=1;
    private final int RATIO_MAX=500;
    //计算滑动距离
    private float startDis=0;
    private float endDis=0;

    //刷新地图的定时器
    private Handler handler;
    //接收RTK数据
    private  Handler bleHandler;
    private TimerTask timerTask;

    //测绘擦除状态 false表示字测绘运行，true表示正在擦除
    private boolean detMapping;
    private boolean getLocationFlag;//定位状态false失败

    private Intent centerCtr;
    //**蓝牙相关**//
    private Intent intentSev;

    private final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    private final int BLE_CONNECT_OFF =21;//蓝牙连接失败
    private final int BLE_SCAN_OFF = 11;
    private final int BLE_SCAN_ON = 10; //扫描到蓝牙
    private final int BLE_DATA_ON = 30; //接收到数据
    private final int RTK_UCONNECT =40;//RTK掉线
    private final int DRAW_MAP =1;

    //BLE service
    private BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;

    //手机振动，音乐播放控制
    private VibrationAndMusic vibrationAndMusic;
    private int remindMsgRTK = 0;//提醒消息弹框，2有弹框，1无弹框（但是不可以再次弹出），0可以再次弹框，，目的是保证弹框不重复，RTK连接失败连接标志（包含本身定位和蓝牙）。
    private final int DIALOG_BLE_RTK_FAIL = 1;//RTK定位失败或蓝牙连接中断
    private final int DIALOG_START_MAPPING = 2;
    private int uconnectTime=0;//掉线检测，3秒没接收到数据认为掉线，定时器中计数，接收回调函数中清零


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_map);
        eraseBtn = (Button)findViewById(R.id.button1);
        saveBtn = (Button)findViewById(R.id.button2);
        exitBtn = (Button)findViewById(R.id.button3);
        moveCenterBtn=(Button)findViewById(R.id.button4);
        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);
        mapView = (MapView)findViewById(R.id.mapView);
        linearLayout =(LinearLayout)findViewById(R.id.backgroundAlarm);
        mapView.setOnTouchListener(this);


        //开启蓝牙Service
        intentSev = new Intent(this, BLEService.class);
        //回到控制中心
        centerCtr = new Intent(this,CenterControlActivity.class);
        //存储文件名的数据文件
        setinfo = getSharedPreferences("TankSetInfo", Context.MODE_PRIVATE);
        infoEditor = setinfo.edit();

        //初始化自定义地图
        InitDrawMap();
        //按键监听
        setOnClick();

       InitRtkConnect();

        //与定时器配合定时器，用于更新地图数据
        handler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == DRAW_MAP) {

                    if (mListPointL.size() > 0) {

                        ArrayList<GpsPoint> list1 = new ArrayList<GpsPoint>();
                        ArrayList<GpsPoint> list2 = new ArrayList<GpsPoint>();

                        if(mListPointL.size() > 1) {
                           list1.addAll(mListPointL.subList(0, detIndex + 1));

                           if(detIndex >0) {
                               list2.addAll(mListPointL.subList(detIndex-1, mListPointL.size()));
                           }else if(detIndex == 0){
                               list2.addAll(mListPointL.subList(detIndex, mListPointL.size()));
                           }
                        }else if(mListPointL.size() == 1){
                            list1.addAll(mListPointL.subList(0, 1));
                            list2.addAll(mListPointL.subList(0, 1));
                        }
                        //   Log.d("Tank001","截断点："+flag);
                        if (detMapping == false) {
                        //    mapView.setLinePoint2(list1, mListPointM, list1.get(list1.size() - 1),psonPoint, mvPoint, ratio);
                            mapView.setLinePoint2(mListPointL, mListPointM, mListPointL.get(mListPointL.size()-1),psonPoint, mvPoint, ratio);
                        } else {
                            mapView.setLinePoint3(list1, list2, mListPointM, list2.get(list2.size() - 1), psonPoint, touchPoint, mvPoint, ratio);

                        }
                    }
                }
                super.handleMessage(msg);
            };
        };

    }

    /***
     *
     * @param whatDialog 是什么对话框
     */
    private void displayMsgDialog(String msg,int whatDialog){


        AlertDialog.Builder msgDialog =
                new AlertDialog.Builder(NewMapActivity.this);

    //    msgDialog.setIcon(R.drawable.icon_dialog);

        switch(whatDialog){
            case DIALOG_BLE_RTK_FAIL:

                remindMsgRTK = 2;//有弹框标志
                msgDialog.setTitle("信号丢失");
                msgDialog.setMessage( msg);
                msgDialog.setPositiveButton("好的",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //...To-do
                                if (vibrationAndMusic.getVibrate()){
                                    vibrationAndMusic.stopVibration();
                                }
                                remindMsgRTK=1;//无弹框

                            }
                        });

                break;
                case DIALOG_START_MAPPING:

                    msgDialog.setTitle("擦除方法");
                    msgDialog.setMessage( msg);
                    msgDialog.setPositiveButton("好的",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //关闭音乐
                                    if(vibrationAndMusic.getMusicState()) {
                                        vibrationAndMusic.playmusic(false);
                                    }
                                    if(vibrationAndMusic.getVibrate()){
                                        vibrationAndMusic.stopVibration();
                                    }
                                    Log.d(TAG,"NewMapActivity->Dialog按键触发");
                                }
                            });

                    break;
        }

        // 显示
        msgDialog.setCancelable(false);
        msgDialog.show();

    }
    /***
     * 设置控件监听
     */
    private void setOnClick(){


        //测量控件高度监听函数
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            //此函数只要mapViewan按键事件或其它变化就会被触发
            @Override
            public void onGlobalLayout() {
                //       Log.e("Tank001", mapView.getMeasuredWidth() + "==" + mapView.getMeasuredHeight());
                screenPoint.x = mapView.getMeasuredWidth();
                screenPoint.y=  mapView.getMeasuredHeight();

                //加载主干道数据
                if(firstInit == true) {

                    if (mListPointM.size() == 0) {
                        readMappingFromSD();
                    }
                    firstInit = false;
                    mvPoint.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);
                    mvPointO.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);
                    mapView.InitView(mvPoint,screenPoint,ratioO);
                }

             //   Log.d(TAG,"函数被触发");
          //      mvPoint.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);
         //       mvPointO.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);
         //       mapView.InitView(mvPoint,ratio);

            }
        });

        eraseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                detMapping =true;
                eraseBtn.setVisibility(View.INVISIBLE);
                exitBtn.setVisibility(View.INVISIBLE);

                saveBtn.setText("确认");
                textView1.setText("擦除路径");
                if(mListPointL.size()>1) {
                     touchPoint.x = mListPointL.get(0).x;
                     touchPoint.y = mListPointL.get(0).y;
                    detIndex = 0;
                }

                String str = "您所在位置为红色点，请用手指点击需要擦除的段起点，黑色点到黄色点的灰色路径是擦除段，" +
                            "您走到黑色点后才能擦除路径";
                displayMsgDialog(str,DIALOG_START_MAPPING);
            }
        });
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binder != null) {
                    //断开蓝牙连接
                    binder.unconnectBle();
                    Log.d(TAG, "退出测绘，关闭蓝牙连接");
                }

                //解除绑定
                timerTask.cancel();
                if (binder != null) {
                    binder.setBleWorkTpye(BLEService.SERV_BLE_NULL,true);
                    binder = null;
                    bleServiceConn = null;
                }

                //关闭音乐
                if (vibrationAndMusic.getMusicState()) {
                    vibrationAndMusic.playmusic(false);
                }
                if (vibrationAndMusic.getVibrate()) {
                    vibrationAndMusic.stopVibration();
                }

                startActivity(centerCtr);

            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(false == detMapping) {

                    new Thread(){
                        @Override
                        public void run() {

                            if(robotCruisePath.mPoints.size()>0){
                                String UserFileUsing = setinfo.getString("UserFileUsing", null);
                                String fileDir = "Tank" + File.separator + UserFileUsing + File.separator + "mapping";
                                //保存测绘类型
                                String mappingType = setinfo.getString("MappingType", "L_");
                                robotCruisePath.bPoint = bPoint;
                                robotCruisePath.Save(mappingType,fileDir);
                            }
                            /*
                            if (mappingList.size() > 0) {
                                //获取文件路径
                                String UserFileUsing = setinfo.getString("UserFileUsing", null);
                                String fileDir = "Tank" + File.separator + UserFileUsing + File.separator + "mapping";
                                //保存测绘类型
                                String mappingType = setinfo.getString("MappingType", "L_");
                                //   Log.d("debug001",fileDir +mappingType);
                                Log.d(TAG, "测绘点数：" + mappingList.size());
                                saveMappingDataAll(mappingList,bPoint,fileDir, mappingType);
                            }
                            */
                            super.run();
                        }
                    }.start();


                    //停止播放音乐
                    if(vibrationAndMusic.getMusicState()) {
                        vibrationAndMusic.playmusic(false);
                    }

                    finish();

                }else{


                    if(mListPointL.size()>1) {

                     //   if (Math.abs(psonPoint.x - mListPointL.get(detIndex).x) < (NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                      //          && Math.abs(psonPoint.y - mListPointL.get(detIndex).y) < ( NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {

                        if(true == detEnable){

                            ArrayList<GpsPoint> pl = new ArrayList<GpsPoint>();

                            pl.addAll(mListPointL.subList(0, detIndex + 1));
                            mListPointL = pl;

                            robotCruisePath.DeletePoints(detIndex+1);

                            Log.d("Tank001", robotCruisePath.mPoints.size() + " " + detIndex);
                            saveBtn.setText("保存");

                            detMapping = false;

                            eraseBtn.setVisibility(View.VISIBLE);
                            exitBtn.setVisibility(View.VISIBLE);

                            //关闭手机振动
                            if(vibrationAndMusic != null) {
                                vibrationAndMusic.stopVibration();
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "走到黑点位置才能擦除",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }else{
                      //  saveBtn.setText("保存");
                      //  detMapping = false;
                      //  eraseBtn.setVisibility(View.VISIBLE);
                      //  exitBtn.setVisibility(View.INVISIBLE);

                        Toast.makeText(getApplicationContext(), "还没有测绘点无法保存",
                                Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });

        moveCenterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveToCenter();//当前位置移动到中心

            }
        });
    }

    /***
     * 将当前位置移动到画布中心
     */
    private void moveToCenter(){
        if(screenPoint.x > 0 && mListPointL.size()>0) {

            GpsPoint point = new GpsPoint();
            point.x = mListPointL.get(mListPointL.size() - 1).x;
            point.y = mListPointL.get(mListPointL.size() - 1).y;
            mvPoint.x = (1 - ratioO) * (point.x);
            mvPoint.y = (1 - ratioO) * (point.y);

            if (mvPoint.x < screenPoint.x * (1 - ratio)) {
                mvPoint.x = screenPoint.x * (1 - ratio);
            } else if (mvPoint.x > 0) {
                mvPoint.x = 0;
            }
            if (mvPoint.y < screenPoint.y * (1 - ratio)) {
                mvPoint.y = screenPoint.y * (1 - ratio);
            } else if (mvPoint.y > 0) {
                mvPoint.y = 0;
            }
            //    mvPointO = mvPoint;
            mvPointO.x = mvPoint.x;
            mvPointO.y = mvPoint.y;
        }
    }
    /***
     * 初始化画布相关
     */
    private void InitDrawMap(){


        mListPointL = new ArrayList<GpsPoint>();
        mListPointM = new ArrayList<GpsPoint>();
        screenPoint = new GpsPoint();
        vibrationAndMusic = new VibrationAndMusic(NewMapActivity.this);

        textView2.setText(""+5000/ratioO+"米");
        textView1.setText("信号丢失");

        getLocationFlag = true;//初始化的时候还没有定位,默认RTK还没有定位成功
        remindMsgRTK = 1;
        linearLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));
        moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position_1));

        screenPoint.x = 0;
        screenPoint.y = 0;

        //bPoint.set((double) setinfo.getLong("basicRTK.X", 0)/10000000000l,(double) setinfo.getLong("basicRTK.Y", 0)/10000000000l,0,0);
        bPoint.set(((double) setinfo.getLong("basicRTK.X", 0)/10000000000l)*MappingGroup.PI/180,((double) setinfo.getLong("basicRTK.Y", 0)/10000000000l)*MappingGroup.PI/180,0,0);

        //     Log.d(TAG,"经度："+bPoint.x+" 纬度：\n"+bPoint.y);

        //touchPoint = mListPoint1.get(mListPoint1.size()-1);类传递是传递指针
        if(mListPointL.size()>1) {
            touchPoint.x = mListPointL.get(mListPointL.size() - 1).x;
            touchPoint.y = mListPointL.get(mListPointL.size() - 1).y;
            detIndex = mListPointL.size()-1;
        }else {
            touchPoint.x = -1000;
            touchPoint.y = -1000;
            detIndex = 0;
        }

        detMapping = false;

    }

    /***
     * 读取主干道测绘数据
     */
    private void readMappingFromSD(){

       new Thread(){
           @Override
           public void run() {

               String mappingType = setinfo.getString("MappingType", "L_");
               //     Log.d(TAG,"测试果园数据" + mappingType);
               //如果是测绘果园路径，则要显示主干道信息
               if(mappingType.equals("L_")){

                   //获取文件路径
                   String UserFileUsing = setinfo.getString("UserFileUsing",null);
                   String filedir = "Tank" + File.separator + UserFileUsing + File.separator + "mapping";

                   File[] files = getMappingList(filedir);
                   int[] len=new int[2];

                   if(files != null ) {
                       //打开所有路径文件
                       for(int k=0;k<files.length;k++) {

                           if(files[k].getName().indexOf("M_1") != -1) { //当前默认只有一个主干道

                               RobotCruisePath robotCruisePath = new RobotCruisePath();
                               robotCruisePath.Open(files[k].getName(), filedir);

                               //当前默认只有一个主干道
                               //   Log.d(TAG, files[0].getName() +" 帧长："+len[0]+" 基站坐标："+bpoint.x+" "+bpoint.y+ "测绘点个数："+ gpslist.size()+"\n");

                               for (int j = 0; j < robotCruisePath.mPoints.size(); j++) {
                                   //转化为画布坐标
                                   GpsPoint point = getPositionFromScreen(null,robotCruisePath.mPoints.get(j));
                                   mListPointM.add(point);
                               }
                           }

                           Log.d(TAG,"WorkMapActivity路径文件名："+files[k].getName());
                       }

                   }

               }
               super.run();
           }
       }.start();
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
            case MotionEvent.ACTION_DOWN:           // 第一个手指按下事件
                touchMode = 1;//
                touchDet = 1;
                startPoint.set(event.getX(),event.getY());
                longTouch = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:

           //     Log.d(TAG,"ACTION_UP触发");
                if(1 == touchDet && true == detMapping) {
                    //擦除手势判断
                    if (System.currentTimeMillis() - longTouch > 500) {

                        //屏幕点实际坐标画布显示位置
                        touchPoint.x = (event.getX()-mvPointO.x)/ratioO;
                        touchPoint.y = (event.getY()-mvPointO.y)/ratioO;

                   //     Log.d(TAG,"长按触发");

                        for(int i=(mListPointL.size()-1);i>0;i--){

                                //查找最近的点删除
                            if (Math.abs(touchPoint.x  - mListPointL.get(i).x) < (NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                                    && Math.abs(touchPoint.y  - mListPointL.get(i).y) < ( NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {

                                detIndex = i;
                                break;
                            }
                        }
                    }
                }
                touchDet = 0;
             //   Log.d("Tank001","时间："+(longTouch-System.currentTimeMillis()));
                touchMode = 0;//
                break;
            case MotionEvent.ACTION_POINTER_UP:        // 手指放开事件

                touchMode = 0;//
        //        Log.d(TAG,"ACTION_POINTER_UP被触发");
           //     touchMode = 0;//
                break;

            case MotionEvent.ACTION_MOVE:

                if(screenPoint.x !=0) {

                    if (2 == touchMode) {
                        endDis = distance(event);
                        if (endDis != 0 && startDis != 0) {

                            if (endDis > startDis) {

                                if(ratioO>=1 && ratioO<10){
                                    ratio = ratio + (endDis - startDis)/100;
                                }else if(ratioO >=10 && ratioO  < 100) {

                                    ratio = ratio + (endDis - startDis) / 10;

                                }else if (ratioO >=100){
                                    ratio = ratio + (endDis - startDis);
                                }

                                if (ratio > RATIO_MAX) {
                                    ratio = RATIO_MAX;
                                }

                            } else {

                                if(ratioO>1 && ratioO < 10){
                                    ratio = ratio - (startDis - endDis)/100;
                                }else if(ratioO >=10 && ratioO  < 100) {

                                    ratio = ratio - (startDis - endDis) / 10;

                                }else if (ratioO >=100){
                                    ratio = ratio - (startDis - endDis);
                                }

                                if (ratio < RATIO_MIN) {

                                    ratio = RATIO_MIN;
                                    ratioO = RATIO_MIN;
                                  mvPointO.x = 0;
                                   mvPointO.y = 0;
                                }
                            }

                            mvPoint.x = midPoint.x - ((midPoint.x -mvPointO.x)/ratioO)*ratio;
                            mvPoint.y = midPoint.y - ((midPoint.y -mvPointO.y)/ratioO)*ratio;

                    //        Log.d(TAG," "+mvPoint.x + " "+mvPoint.y+"\n");

                   //         Log.d(TAG,"放大倍数："+ratio+"x偏移："+mvPoint.x+"y偏移："+mvPoint.y);
                            ratioO = ratio;
                            startDis = endDis;

                            mvPointO.x = mvPoint.x;
                            mvPointO.y = mvPoint.y;

                            textView2.setText(""+(int)((5000/ratioO)+0.5)+"米");

                        }
                    } else if(1 == touchMode){

                        //判断手指是否滑动
                        if (Math.abs(startPoint.x - event.getX()) > 20
                                && Math.abs(startPoint.y - event.getY()) > 20) {
                            touchDet = 0;
                        }

                        mvPoint.x = mvPointO.x + event.getX() - startPoint.x;
                        mvPoint.y = mvPointO.y + event.getY() - startPoint.y;

                        startPoint.x = event.getX();
                        startPoint.y = event.getY();

                        if (mvPoint.x < screenPoint.x*(1-ratio)) {
                            mvPoint.x = screenPoint.x*(1 -  ratio);
                        } else if (mvPoint.x > 0) {
                            mvPoint.x = 0;
                        }
                        if (mvPoint.y < screenPoint.y * (1 - ratio)) {
                            mvPoint.y = screenPoint.y * (1 - ratio);
                        } else if (mvPoint.y > 0) {
                            mvPoint.y = 0;
                        }

                        mvPointO.x = mvPoint.x;
                        mvPointO.y = mvPoint.y;
                    //    Log.d(TAG,"滑动触发");

                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:          // 第二个手指按下事件

                touchDet =0;
                touchMode = 2;
                startDis = distance(event);
                if(startDis>50f)
                {
                    midPoint = midPoint(event);
                }
                break;

        }
        return true;
    }

    // 计算两个触摸点之间的距离
    private float distance(MotionEvent event)
    {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);

        return (float)Math.sqrt(dx*dx +dy*dy);
    }
    // 计算两个触摸点的中点
    private PointF midPoint(MotionEvent event)
    {
        int midX = (int)(event.getX(1)+event.getX(0) )/2;
        int midY = (int)(event.getY(1)+event.getY(0) )/2;

        return new PointF(midX,midY);
    }

    /***
     * 接收处理RTK连接
     */
    private void InitRtkConnect(){

    bleHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            //在handler中更新UI
            switch(msg.what){
                case  BLE_SCAN_ON:
                    break;
                case BLE_SCAN_OFF:
                    break;
                case BLE_DATA_ON:
                    //接收到数据
                    MappingGroup rtkMap=(MappingGroup) msg.obj;
                    uconnectTime=0;
                //       Log.d(TAG,"RTK状态："+rtkMap.rtkState+"时间："+rtkMap.GPSTime_ms+"经度："+rtkMap.longitude*180/MappingGroup.PI
                //               +"纬度："+rtkMap.latitude*180/MappingGroup.PI+"海拔："+rtkMap.altitude+"方向：\n"+rtkMap.yaw);

                    if(rtkMap.rtkState == 0) {//等于0才是FIX数据

                        moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position));

                        //-----------------------相对于基站的距离单位m-----------------------------------//
                        PointF gpsPointF = getPositionFromBasicStation(rtkMap);//相对于基站的距离
                        //--------------------------显示到屏幕上的坐标----------------------------//
                        GpsPoint gpsPoint = getPositionFromScreen(rtkMap,gpsPointF);
                    //    psonPoint = gpsPoint;
                        psonPoint.x = gpsPoint.x;
                        psonPoint.y = gpsPoint.y;
                        psonPoint.d = gpsPoint.d;

                  //      Log.d(TAG,"X坐标="+gpsPointF.x+" Y坐标="+gpsPointF.y);

                        if (false == detMapping && screenPoint.x != 0) {//正常测绘模式下

                            if(getLocationFlag == false && robotCruisePath.mPoints.size()>=2){//定位失败，第一次进入定位成功
                                //获取虚拟点
                                PointF virPoint = getVirtualLocation(robotCruisePath.mPoints.get(robotCruisePath.mPoints.size()-2),
                                        robotCruisePath.mPoints.get(robotCruisePath.mPoints.size()-1));
                                //与掉线前虚拟的下一个点重合
                                if (Math.abs(gpsPointF.x - virPoint.x) < NEAR_DIS
                                            && Math.abs(gpsPointF.y - virPoint.y) < NEAR_DIS){

                                    //手机振动
                                    if(vibrationAndMusic.getVibrate() == false) {
                                        vibrationAndMusic.Vibrate(new long[]{500, 1000, 500, 1000}, true);
                                    }
                                    getLocationFlag =true;
                                    //强制添加点
                                    robotCruisePath.AddPointForce(gpsPointF);
                                }
                            }else { //正常测绘

                                //一直定位成功不显示红色箭头
                                if( getLocationFlag == true) {
                                    psonPoint.x = -10000;
                                    psonPoint.y = -10000;
                                }

                                //添加点
                                if (robotCruisePath.AddPoint(gpsPointF, DELTA_DIST)) {//添加成功
                                    //定位数据处理
                                    mListPointL.add(gpsPoint);
                                    detIndex = mListPointL.size()-1;
                                }

                                //提示信息处理
                                if(vibrationAndMusic.getVibrate()) {
                                    vibrationAndMusic.stopVibration();
                                }
                                //播放音乐
                                if(vibrationAndMusic.getMusicState() == false) {
                                    vibrationAndMusic.playmusic(true);
                                    textView1.setText("正在测绘");
                                }
                            }

                        } else if(detIndex>1){//擦除模式下

                            //与删除点重合
                       //     if (Math.abs(psonPoint.x - mListPointL.get(detIndex).x) < ( NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                       //             && Math.abs(psonPoint.y - mListPointL.get(detIndex).y) < (NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {
                            //获取虚拟的下一个点
                            PointF virPoint = getVirtualLocation(robotCruisePath.mPoints.get(detIndex-1),
                                    robotCruisePath.mPoints.get(detIndex));
                            //与虚拟点重合
                            if (Math.abs(gpsPointF.x - virPoint.x) < NEAR_DIS
                                         && Math.abs(gpsPointF.y - virPoint.y) < NEAR_DIS) {
                                //第一次重合，强制添加点
                                if(detEnable == false){
                                    robotCruisePath.AddPointForce(gpsPointF);
                                }
                                detEnable = true;

                                //手机振动
                                if(vibrationAndMusic.getVibrate() == false) {
                                    vibrationAndMusic.Vibrate(new long[]{500, 1000,500,1000}, true);
                                }
                                if (vibrationAndMusic.getMusicState() == false){
                                    vibrationAndMusic.playmusic(true);
                                }

                            }else {
                                //第一次由重合到退出，删除强制添加的点
                                if(detEnable == true) {
                                    robotCruisePath.DeletePoints(robotCruisePath.mPoints.size()-1);
                                }
                                detEnable = false;
                                //停止振动，和播放音乐
                                if(vibrationAndMusic.getVibrate()) {
                                    vibrationAndMusic.stopVibration();
                                }

                                if (vibrationAndMusic.getMusicState()){
                                    vibrationAndMusic.playmusic(false);
                                }
                            }
                        }

                        linearLayout.setBackgroundColor(getResources().getColor(R.color.tankgreen));
                        if(1 == remindMsgRTK) {
                            remindMsgRTK = 0;//连接上拉，可以再次弹框
                        }

                    }else{

                        //没有fix，不显示人的位置
                        psonPoint.x = -10000;
                        psonPoint.y = -10000;
                        psonPoint.d = 0;

                        //定位失败
                        moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position_1));
                        linearLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));

                        getLocationFlag =false;

                        if (vibrationAndMusic.getMusicState()){
                            vibrationAndMusic.playmusic(false);
                        }

                        if(remindMsgRTK == 0) {

                            if(vibrationAndMusic.getVibrate() == false) {
                                vibrationAndMusic.Vibrate(new long[]{500, 1000,500,1000}, true);

                            }
                            textView1.setText("信号丢失");

                            String str = "等待RTK信号恢复正常，您的位置为红色点，请找到黄色位置点继续开始测绘";
                            displayMsgDialog(str,DIALOG_BLE_RTK_FAIL);
                        }


                    }
                    break;
                case RTK_UCONNECT: //计时器判断RTK掉线
                    getLocationFlag = false;

                    textView1.setText("信号丢失");

                    linearLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position_1));

                    if(remindMsgRTK == 0) {

                        if (vibrationAndMusic.getMusicState()){
                            vibrationAndMusic.playmusic(false);
                        }

                        if(vibrationAndMusic.getVibrate() == false) {
                            vibrationAndMusic.Vibrate(new long[]{500, 1000,500,1000}, true);
                        }

                        String str = "等待信号恢复正常，您的位置为红色点，请找到黄色位置点继续开始测绘";
                        displayMsgDialog(str,DIALOG_BLE_RTK_FAIL);
                        //  Log.d(TAG,"信号丢失");
                    }
                    break;
                case BLE_CONNECT_OFF:
                    /*
                   textView1.setText("信号丢失");
                   linearLayout.setBackgroundColor(getResources().getColor(R.color.colorRed));

                    getLocationFlag = false;

                    moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position_1));

                    if (vibrationAndMusic.getMusicState()){
                        vibrationAndMusic.playmusic(false);
                    }

                    if(remindMsgRTK == 0) {

                        if(vibrationAndMusic.getVibrate() == false) {
                            vibrationAndMusic.Vibrate(new long[]{500, 1000,500,1000}, true);
                        }

                        String str = "蓝牙信号丢失等待复正常，您的位置为红色点，请找到黄色位置点继续开始测绘";
                       displayMsgDialog(str,DIALOG_BLE_RTK_FAIL);
                     //  Log.d(TAG,"信号丢失");
                    }
                   */
                    if(binder != null) {
                        binder.connectBle(null, true);//掉线重连
                    }
                    Log.d(TAG,"信号丢失");

                    break;
                case BLE_CONNECT_ON:
                    /*
                    remindMsgRTK = 0;//连接上，可以继续弹出消息
                    if(true == detMapping ) {
                        textView1.setText("擦除路径");
                    }else {
                        textView1.setText("正在测绘");
                    }
                    linearLayout.setBackgroundColor(getResources().getColor(R.color.tankgreen));
                    moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position));
                    */
                    break;
            }

        }
    };


}

    /***
     * 计算虚拟的下一个点
     * @param a 测绘坐标n-1
     * @param b 测绘坐标 n
     * @return 测绘坐标n+1
     */
     private PointF getVirtualLocation(PointF a,PointF b){
        PointF p = new PointF();
        double ab = Math.sqrt(Math.pow(b.x - a.x,2) + Math.pow(b.y - a.y,2));
        p.x = (float) (b.x + VIRTUAL_DIS*(b.x - a.x)/ab);
        p.y = (float) (b.y + VIRTUAL_DIS*(b.y - a.y)/ab);
        return p;

     };

    /***
     *
     * @param rtkMap 接收的RTK数据
     * @return 相对基站的坐标
     */
    private  PointF getPositionFromBasicStation(MappingGroup rtkMap){
        PointF p = new PointF();
     //   p .x = (float) (((((double)rtkMap.longitude / MappingGroup.INM_LON_LAT_SCALE ) * 180/MappingGroup.PI )* Math.cos(rtkMap.latitude/MappingGroup.INM_LON_LAT_SCALE) - (bPoint.x*180/MappingGroup.PI) * Math.cos(bPoint.y))*GPS_DIS);

        p .x = (float) ((((double)rtkMap.longitude /MappingGroup.INM_LON_LAT_SCALE - bPoint.x)*180/MappingGroup.PI)* Math.cos(rtkMap.latitude/MappingGroup.INM_LON_LAT_SCALE)*GPS_DIS);
        p .y = (float) ((((double)rtkMap.latitude / MappingGroup.INM_LON_LAT_SCALE - bPoint.y)*180/MappingGroup.PI)*GPS_DIS);

        return p;
    }

    /***
     *
     * @param rtkMap 接收的RTK数据
     * @param pointF 相对基站的坐标
     * @return 屏幕坐标
     */
    private  GpsPoint getPositionFromScreen(MappingGroup rtkMap,PointF pointF) {
        GpsPoint p = new GpsPoint();
        p.x = screenPoint.x / 2 +  pointF .x*(screenPoint.x/ MAPMAX_DIS);
        p.y = screenPoint.y / 2 - pointF .y* (screenPoint.y/ MAPMAX_DIS);  //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
        if(rtkMap !=null) {
            p.d = rtkMap.yaw;
        }
        return p;
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

            //开启，设置Ble工作类型
            if(binder != null){
                binder.setBleWorkTpye(BLEService.BLE_MAP_CONECT,true);
            }

            binder.getService().setMappingCallback(new BLEService.MappingCallback() {
                //执行回调函数
                @Override
                public void BleScanChanged(ArrayList<BluetoothDevice> bleDeviceList) {

                }
                //BLE接收到数据
                @Override
                public void BleDataChanged( MappingGroup rtkMap) {
                    Message msg = new Message();
                    msg.what = BLE_DATA_ON;
                    msg.obj = rtkMap;
                    bleHandler.sendMessage(msg);
                    //发送通知
                }
                //BLE状态变化
                @Override
                public void BleStateChanged(int msg) {
                    bleHandler.sendEmptyMessage(msg);
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

    @Override
    protected void onStart() {

        //绑定蓝牙服务
        bleServiceConn = new BleServiceConn();
        bindService(intentSev, bleServiceConn, Context.BIND_AUTO_CREATE);

        //开启定时器
        Timer timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {

                handler.sendEmptyMessage(1);

                uconnectTime++;//RTK掉线检测计时
                if( uconnectTime>=30){//计时3秒，RTK掉线
                    Message message = new Message();
                    message.what = RTK_UCONNECT;
                    bleHandler.sendEmptyMessage(RTK_UCONNECT);

                    uconnectTime=0;
                }

            //    Log.d(TAG,"定时器运行中");
            }
        };
        timer.schedule(timerTask,1000,100);

        Log.d(TAG, "NewMaoActivity->onStart()");
        super.onStart();
    }


    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed() ");
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        //删除播放器
        vibrationAndMusic.deletMediaPlayer();
        vibrationAndMusic.stopVibration();

        //解除绑定
        timerTask.cancel();
        if(binder !=null){
            binder.setBleWorkTpye(BLEService.SERV_BLE_NULL,true);
            binder=null;
            bleServiceConn = null;
        }


        Log.d(TAG, "NewMaoActivity->onDestroy() ");
        super.onDestroy();
    }

    @Override
    protected void onResume() {

        super.onResume();
        locatonFlag = initLocationPermission(NewMapActivity.this);//针对6.0以上版本做权限适配
    }


    /**
     * 权限的结果回调函数
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            locatonFlag = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
    }

}
