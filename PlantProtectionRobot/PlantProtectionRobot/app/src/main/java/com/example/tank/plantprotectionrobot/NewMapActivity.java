package com.example.tank.plantprotectionrobot;


import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.ChoicePage.ConnectRTK;
import com.example.tank.plantprotectionrobot.ChoicePage.MySpinnerAdapter;
import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.LocationUtils;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.autonavi.ae.search.log.GLog.fileDir;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.ByteToArrayMappingGroup;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.MappingGroupToArray;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingData;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingHead;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.getMappingLength;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.setMappingDataAll;
import static com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup.setMappingLength;
import static com.example.tank.plantprotectionrobot.DataProcessing.PermisionUtils.initLocationPermission;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getMappingList;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getShort;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.readFileFromSDCard;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.writeFileToSDCard;

/*
 @新建地图测绘界面程序
 */

public class NewMapActivity extends AppCompatActivity implements View.OnTouchListener{

    Button exitBtn;
    Button saveBtn;
    Button eraseBtn;
    Button moveCenterBtn;
    MapView mapView;
    private boolean isGpsEnabled;
    private String locateType;
    private final  String TAG = "Tank001";

    //数据，此处保存当前打开的农场主名，即是农场主文件中名
    private SharedPreferences setinfo;
    private SharedPreferences.Editor infoEditor;



    //高德地图
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    private boolean locatonFlag;  //权限获取标志1表示获取位置权限成功

    //手势控制相关
    //此处是测试用
    private ArrayList<GpsPoint>  mListPointL; //当前测绘显示数据
    private ArrayList<GpsPoint>  mListPointM; //主干道测绘显示数据
    //测绘数据，GPS原始数据
    private ArrayList<MappingGroup> mappingList;
    private double mappingAdd=0.0;

    private int detFlag =0; //擦除点标志

    private long  longTouch;  //长按判断计时
    private Point screenPoint; //获取屏幕的大小
    private int touchMode = 0;//手势检测标志1时表示有两个触点
    private int touchDet = 0;//手势检长按
    private PointF startPoint = new PointF(); //第一个手指按下是的坐标
    private PointF midPoint = new PointF();   //两个手指防线时的中点坐标

    //删除点
    private GpsPoint touchPoint = new GpsPoint();
    //擦除模式当前位置
    private GpsPoint psonPoint = new GpsPoint();
    private final int NEAR_DIS = 4;//判断点是否重合距离，单位米
    private final int MAPMAX_DIS = 5000;//地图最大距离单位米
    private final int GPS_DIS = 111000;//纬度1度的距离，单位米
    //基站坐标
    private GpsPoint bPoint = new GpsPoint();

    //地图显示偏移坐标
    private GpsPoint mvPoint = new GpsPoint();
    private GpsPoint mvPointO = new GpsPoint();
    //比例尺参数
    private  float ratio = 1;
    private  float ratioO = 1;
    private final int RATIO_MIN=1;
    private final int RATIO_MAX=1000;
    //计算滑动距离
    private float startDis=0;
    private float endDis=0;

    //刷新地图的定时器
    private Handler handler;
    private Timer timer;

    private float number=0;

    //测绘擦除状态 false表示字测绘运行，true表示正在擦除
    private boolean detMapping;

    private Intent centerCtr;
    //**蓝牙相关**//
    private Intent intentSev;

    private final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    private final int BLE_CONNECT_OFF =21;//练呀连接失败
    private final int BLE_SCAN_OFF = 11;
    private final int BLE_SCAN_ON = 10; //扫描到蓝牙
    private final int BLE_DATA_ON = 30; //接收到数据
    private final int DRAW_MAP =1;

    //BLE service
    private BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;

    private final int BLE_MAPPING = 1;//测绘蓝牙

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_map);
        eraseBtn = (Button)findViewById(R.id.button1);
        saveBtn = (Button)findViewById(R.id.button2);
        exitBtn = (Button)findViewById(R.id.button3);
        moveCenterBtn=(Button)findViewById(R.id.button4);
        mapView = (MapView)findViewById(R.id.mapView);
        mapView.setOnTouchListener(this);

        //开启蓝牙Service
        intentSev = new Intent(this, BLEService.class);
        //回到控制中心
        centerCtr = new Intent(this,CenterControlActivity.class);
        //存储文件名的数据文件
        setinfo = getSharedPreferences("TankSetInfo", Context.MODE_PRIVATE);
        infoEditor = setinfo.edit();
        //实例化测绘list
        mappingList=new ArrayList<MappingGroup>();


        //初始化高德地图定位
        initMap();
        //初始化自定义地图
        InitDrawMap();
        //按键监听
        setOnClick();

        //与定时器配合定时器，用于更新地图数据
        handler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == DRAW_MAP) {
                    if (mListPointL.size() > 1) {

                        ArrayList<GpsPoint> list1 = new ArrayList<GpsPoint>();
                        ArrayList<GpsPoint> list2 = new ArrayList<GpsPoint>();

                        if(mListPointL.size()>0) {
                           list1.addAll(mListPointL.subList(0, detFlag + 1));
                          list2.addAll(mListPointL.subList(detFlag, mListPointL.size()));
                        }
                        //   Log.d("Tank001","截断点："+flag);
                        if (detMapping == false) {
                            mapView.setLinePoint2(list1, mListPointM, list1.get(list1.size() - 1), mvPoint, ratio);
                        } else {
                            mapView.setLinePoint3(list1, list2, mListPointM, list2.get(list2.size() - 1), psonPoint, touchPoint, mvPoint, ratio);

                        }
                    }
                }
                super.handleMessage(msg);
            };
        };
        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
        }, 1000,100);
    }

    /***
     * 设置控件监听
     */
    private void setOnClick(){
        eraseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                detMapping =true;
                eraseBtn.setVisibility(View.INVISIBLE);
                saveBtn.setText("确认");
                if(mListPointL.size()>1) {
                     touchPoint.x = mListPointL.get(mListPointL.size()-1).x;
                     touchPoint.y = mListPointL.get(mListPointL.size()-1).y;
                     detFlag = mListPointL.size() - 1;
                }
                //停止播放音乐
              if(binder !=null) {
                    binder.playmusic(false);
                }

            }
        });
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(true == detMapping){//小于10m
/*
                    if(Math.abs(psonPoint.x-mListPointL.get(mListPointL.size()-1).x)<((double)NEAR_DIS*screenPoint.x/MAPMAX_DIS)
                            && Math.abs(psonPoint.x-mListPointL.get(mListPointL.size()-1).x)<((double)NEAR_DIS*screenPoint.y/MAPMAX_DIS)){
                        saveBtn.setText("保存");
                        detMapping =false;
                        eraseBtn.setVisibility(View.VISIBLE);
                    }else {
                        Toast.makeText(getApplicationContext(), "走到测绘绿色箭头位置才能退出",
                                Toast.LENGTH_SHORT).show();
                    }
*/
                }else {
                    if(binder !=null)
                    {
                        //断开蓝牙连接
                        binder.unconnectBle();
                        //关闭音乐
                        binder.playmusic(false);
                    }
                    startActivity(centerCtr);
                }

            }
        });
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(false == detMapping) {
                    //获取文件路径
                    String UserFileUsing = setinfo.getString("UserFileUsing", null);
                    String fileDir = "Tank" + File.separator + UserFileUsing + File.separator + "mapping";
                    //保存测绘类型
                    String mappingType = setinfo.getString("MappingType", "L_");
                    //   Log.d("debug001",fileDir +mappingType);
                    Log.d("debug001", "测绘点数：" + mappingList.size());

                    if (mappingList.size() > 0) {
                        setMappingDataAll(mappingList, mappingList.size(),bPoint,fileDir, mappingType);
                        //     setMappingLength(fileDir,mappingType,1010);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "测绘点太少，无法形成测绘路径",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    //停止播放音乐
                    if(binder !=null) {
                        binder.playmusic(false);
                    }

                }else{
                    if(mListPointL.size()>1) {
                        if (Math.abs(psonPoint.x - mListPointL.get(detFlag).x) < ((double) NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                                && Math.abs(psonPoint.x - mListPointL.get(detFlag).x) < ((double) NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {
                            ArrayList<GpsPoint> pl = new ArrayList<GpsPoint>();
                            ArrayList<MappingGroup> ml = new ArrayList<MappingGroup>();

                            pl.addAll(mListPointL.subList(0, detFlag + 1));
                            ml.addAll(mappingList.subList(0, detFlag + 1));

                            mListPointL = pl;
                            mappingList = ml;

                            Log.d("Tank001", mappingList.size() + " " + detFlag);
                            saveBtn.setText("保存");
                            detMapping = false;
                            eraseBtn.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(getApplicationContext(), "走到黑点位置才能擦除",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        saveBtn.setText("保存");
                        detMapping = false;
                        eraseBtn.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        moveCenterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

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
        });
    }
    /***
     * 初始化画布相关
     */
    private void InitDrawMap(){

        mListPointL = new ArrayList<GpsPoint>();
        mListPointM = new ArrayList<GpsPoint>();
        screenPoint = new Point();


        screenPoint.x = 0;
        screenPoint.y = 0;
        //测量控件高度监听函数
        mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
         //       Log.e("Tank001", mapView.getMeasuredWidth() + "==" + mapView.getMeasuredHeight());
                screenPoint.x = mapView.getMeasuredWidth();
                screenPoint.y = mapView.getMeasuredHeight();
                mvPoint.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);
                mvPointO.set((1-ratioO)*screenPoint.x/2,(1-ratioO)*screenPoint.y/2,0,0);
            }
        });

        bPoint.set((double) setinfo.getLong("basicRTK.X", 0)/10000000000l,(double) setinfo.getLong("basicRTK.Y", 0)/10000000000l,0,0);

        Log.d("Tank001","经度："+bPoint.x+" 纬度：\n"+bPoint.y);

        //touchPoint = mListPoint1.get(mListPoint1.size()-1);类传递是传递指针
        if(mListPointL.size()>1) {
            touchPoint.x = mListPointL.get(mListPointL.size() - 1).x;
            touchPoint.y = mListPointL.get(mListPointL.size() - 1).y;
            detFlag = mListPointL.size()-1;
        }else {
            touchPoint.x = -100;
            touchPoint.y = -100;
            detFlag = 0;
        }

        detMapping = false;
        mapView.InitView(mvPoint,ratio);

    }

    /***
     * 初始高德地图
     */
    private void initMap(){
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


    /***
     * 手势控制
     * @param v
     * @param event
     * @return
     */
  //  public boolean onTouchEvent(MotionEvent event) {
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

                        for(int i=(mListPointL.size()-1);i>0;i--){

                            //查找最近的点删除
                            if(Math.abs(event.getX()-mvPointO.x - mListPointL.get(i).x*ratioO) < 5*ratioO &&
                                    Math.abs(event.getY()-mvPointO.y - mListPointL.get(i).y*ratioO)  < 5*ratioO ){
                                detFlag = i;

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

                                if(ratioO>=1 && ratioO < 10){
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

                            mvPoint.x = (midPoint.x * (ratioO - ratio) + mvPointO.x * ratio) / ratioO;
                            mvPoint.y = (midPoint.y * (ratioO - ratio) + mvPointO.y * ratio) / ratioO;
                   //         Log.d(TAG,"放大倍数："+ratio+"x偏移："+mvPoint.x+"y偏移："+mvPoint.y);
                            ratioO = ratio;
                            startDis = endDis;
                            //    mvPointO = mvPoint;
                            mvPointO.x = mvPoint.x;
                            mvPointO.y = mvPoint.y;

                        }
                    } else if(1 == touchMode){
                        //判断手指是否滑动
                        if (Math.abs(startPoint.x - event.getX()) > 20
                                && Math.abs(startPoint.y - event.getY()) > 20) {
                            touchDet = 0;
                        }

                  //      mvPoint.x = mvPointO.x + (1 - ratioO) * (startPoint.x - event.getX());
                 //       mvPoint.y = mvPointO.y + (1 - ratioO) * (startPoint.y - event.getY());

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
                        //    mvPointO = mvPoint;
                        mvPointO.x = mvPoint.x;
                        mvPointO.y = mvPoint.y;

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


    Handler bleHandler = new Handler() {
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
                //    Log.d(TAG,"RTK状态："+rtkMap.rtkState+"时间："+rtkMap.GPSTime_tow+"经度："+rtkMap.longitude
                 //           +"纬度："+rtkMap.latitude+"海拔："+rtkMap.altitude+"方向：\n"+rtkMap.direction);
                    if(rtkMap.longitude != 0) {
                        //默认基站在画布中心，测绘点的位置是相对于基站的位置
                        moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position));
                        GpsPoint gpsPoint = new GpsPoint();
                        gpsPoint.x = screenPoint.x / 2 + (rtkMap.longitude * Math.cos(rtkMap.latitude * Math.PI / 180) - bPoint.x * Math.cos(bPoint.y * Math.PI / 180)) * ((double) screenPoint.x * GPS_DIS / MAPMAX_DIS);
                        //     gpsPoint.y = screenPoint.y/2+(rtkMap.latitude - bPoint.y) * ((double) screenPoint.y * GPS_DIS  / MAPMAX_DIS);
                        //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                        gpsPoint.y = screenPoint.y / 2 + (bPoint.y - rtkMap.latitude) * ((double) screenPoint.y * GPS_DIS / MAPMAX_DIS);
                        gpsPoint.d = rtkMap.direction;

                        psonPoint.x = gpsPoint.x * ratioO;
                        psonPoint.y = gpsPoint.y * ratioO;
                        psonPoint.d = gpsPoint.d;

                        //         Log.d(TAG,"X坐标："+psonPoint.x+" Y坐标"+psonPoint.y);

                        if (false == detMapping && screenPoint.x != 0) {

                            mappingList.add(rtkMap);
                            mListPointL.add(gpsPoint);
                            detFlag = mListPointL.size() - 1;
                        } else {
                            //与起点重合

                            //   if(Math.abs(psonPoint.x-mListPointL.get(mListPointL.size()-1).x)<((double)NEAR_DIS*screenPoint.x/MAPMAX_DIS)
                            //           && Math.abs(psonPoint.x-mListPointL.get(mListPointL.size()-1).x)<((double)NEAR_DIS*screenPoint.y/MAPMAX_DIS)) {
                            //    }
                            //与删除点重合
                            if (Math.abs(psonPoint.x - mListPointL.get(detFlag).x) < ((double) NEAR_DIS * screenPoint.x / MAPMAX_DIS)
                                    && Math.abs(psonPoint.x - mListPointL.get(detFlag).x) < ((double) NEAR_DIS * screenPoint.y / MAPMAX_DIS)) {

                                //手机振动
                                VibratorUtil.Vibrate(NewMapActivity.this, 1000);
                                if(binder !=null){
                                    binder.playmusic(true);
                                }
                            }
                        }

                    }else{
                        //定位失败
                        moveCenterBtn.setBackground(getResources().getDrawable(R.drawable.position_1));

                    }
                    break;
                case BLE_CONNECT_OFF:
                    Toast.makeText(getApplication(), "测绘杆连接断开" ,
                            Toast.LENGTH_SHORT).show();
                    break;
                case BLE_CONNECT_ON:
                    break;
            }

        }
    };

    /***
     * service连接
     */
    class BleServiceConn implements ServiceConnection {
        // 服务被绑定成功之后执行

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // IBinder service为onBind方法返回的Service实例
            binder = (BLEService.BleBinder) service;


            //开启
            if(binder !=null)
            {
                //开启音乐
                binder.playmusic(true);
                binder.setBleWorkTpye(BLE_MAPPING);
            }

            binder.getService().setDataCallback(new BLEService.DataCallback() {
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
                //    bleHandler.obtainMessage(BLE_DATA_ON,data.length,-1,data).sendToTarget();
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

    /***
     * 绑定蓝牙service函数
     */
    private void bindBleSerive(){
        //Ble
        bleServiceConn = new BleServiceConn();
        bindService(intentSev, bleServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        //绑定蓝牙服务后台
        bindBleSerive();



        //加载主干道数据
        String mappingType = setinfo.getString("MappingType", "L_");
        //如果是测绘果园路径，则要显示主干道信息
        if(mappingType.equals("L_")){
            //获取文件路径
            String UserFileUsing = setinfo.getString("UserFileUsing",null);
            String filedir = "Tank" + File.separator + UserFileUsing + File.separator + "mapping";

            File[] files = getMappingList(filedir);
            int[] len=new int[2];
            GpsPoint point=new GpsPoint();
            if(files != null ) {
                ArrayList<MappingGroup> gpslist = new ArrayList<MappingGroup>();
                getMappingHead(filedir+File.separator+files[0].getName(),len,point);

                getMappingData(filedir+File.separator+files[0].getName(),gpslist,len[0]);

                //当前默认只有一个主干道
                Log.d("debug001", files[0].getName() +" 帧长："+len[0]+" 基站坐标："+point.x+" "+point.y+ "测绘点个数："+ gpslist.size()+"\n");

                for(int j=0;j<gpslist.size();j++){

                    //转化为画布坐标
                    point.x = gpslist.get(j).longitude;
                    point.y =gpslist.get(j).latitude;
                    point.d =gpslist.get(j).direction;

                    point.x = screenPoint.x/2+(gpslist.get(j).longitude*Math.cos(gpslist.get(j).latitude * Math.PI / 180) - bPoint.x*Math.cos(bPoint.y * Math.PI / 180)) * ((double) screenPoint.x * GPS_DIS / MAPMAX_DIS);
                    //     gpsPoint.y = screenPoint.y/2+(rtkMap.latitude - bPoint.y) * ((double) screenPoint.y * GPS_DIS  / MAPMAX_DIS);
                    //将坐标系转为与地图一样（手机屏幕坐标沿x轴对称）
                    point.y = screenPoint.y/2+(bPoint.y -gpslist.get(j).latitude) * ((double) screenPoint.y * GPS_DIS  / MAPMAX_DIS);
                    point.d = gpslist.get(j).direction;

                    psonPoint.x=point.x*ratioO;
                    psonPoint.y=point.y*ratioO;
                    psonPoint.d=point.d;

                    mListPointM.add(point);

                }

            }
        }

        super.onStart();
    }

    @Override
    protected void onStop() {

        if(binder !=null){
            binder.playmusic(false);
            binder.unconnectBle();
        }
        //解除蓝牙服务后台绑定绑定
        unbindService(bleServiceConn);

        super.onStop();
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

    //高德地图回调
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {

            if (aMapLocation != null) {

                if (aMapLocation.getErrorCode() == 0) {
                    /*
                    String str="";
                    str += aMapLocation.getLatitude();//获取纬度
                    str +="\n";
                    str +=aMapLocation.getLongitude();//获取经度
                    str +="\n";
                    */
                }else {
                    //   Toast.makeText(getApplicationContext(),"定位失败",
                    //         Toast.LENGTH_SHORT).show();

                }
            }
        }
    };
}
