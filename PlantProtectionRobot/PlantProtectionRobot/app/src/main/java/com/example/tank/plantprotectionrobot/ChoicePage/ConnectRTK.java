package com.example.tank.plantprotectionrobot.ChoicePage;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;

import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.CenterControlActivity;
import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.NewMapActivity;
import com.example.tank.plantprotectionrobot.R;

import java.util.ArrayList;
import java.util.List;


/**
 * @新建地图时，连接测绘杆界面
 */

public class ConnectRTK extends Fragment {

    private MySpinner spinner1;
    private Spinner spinner2;
    private Button startBtn;
    private Button endBtn;

    private String MappingRodNumber;  //测绘杆标号
    private String MappingType;        //测绘类型 M_表示主干道，L_表示果园
    private String UserFileUsing;     //当前打开的果园文件名

    private EditText editText1;//经度输入值
    private EditText editText2;//纬度输入值
    private GpsPoint bPoint = new GpsPoint();   //基站坐标，弧度制

    //设置数据，此处保存当前打开的农场主名，即是农场主文件中名
    private SharedPreferences setinfo;
    private SharedPreferences.Editor infoEditor;

    //BLE service
    private BleServiceConn bleServiceConn;
    private BLEService.BleBinder binder = null;

    //绘制新地图
    private Intent intentMap ;
    //回到控制中心
    private Intent intentCenter;
    //开启蓝牙Service
    private Intent intentSev;
    private ArrayList<BluetoothDevice> mBleDeviceList=new ArrayList<BluetoothDevice>();
    private int bleConnected = 0;
    private boolean selectedFirst=true;//第一次进入程序
    private final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    private final int BLE_CONNECT_OFF =21;//练呀连接失败
    private final int BLE_SCAN_OFF = 11;
    private final int BLE_SCAN_ON = 10; //扫描到蓝牙
    private final int BLE_DATA_ON = 30; //接收到数据

    private final int BLE_CONNECTED=40;//当前正在连接的蓝牙反馈

    private boolean ble_connectFlag=false;//蓝牙连接标志

    private final  String TAG = "Tank001";


    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choive_page2, container, false);
        startBtn = (Button)view.findViewById(R.id.button1);
        endBtn = (Button)view.findViewById(R.id.button2);

        spinner1= (MySpinner)view.findViewById(R.id.spinner1);
        spinner2= (Spinner)view.findViewById(R.id.spinner2);
        editText1=(EditText)view.findViewById(R.id.editText1);
        editText2 = (EditText)view.findViewById(R.id.editText2);

        //绘制新地图
        intentMap = new Intent(getActivity(),NewMapActivity.class);
        //回到控制中心
        intentCenter = new Intent(getActivity(),CenterControlActivity.class);
        //开启蓝牙Service
        intentSev = new Intent(getActivity(), BLEService.class);
        bleServiceConn = new BleServiceConn();

        //配置数据文件
        setinfo = getActivity().getSharedPreferences("TankSetInfo", Context.MODE_PRIVATE);
        infoEditor = setinfo.edit();
        //初始化数据
        initData();
        setOnClick();

        return view;
    }

    //在Frament启动和关闭时所要处理的事件
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        selectedFirst =true;
        if (isVisibleToUser) {//相当于Fragment的onResume

            //绑定蓝牙服务
            getActivity().bindService(intentSev, bleServiceConn, Context.BIND_AUTO_CREATE);
            Log.d(TAG,"ConnectRTK,绑定BLE");

        } else {  //相当于Fragment的onPause

            Log.d(TAG,"ConnectRTK onStop() ");

        }
    }


    /***
     * 设置控件监听
     */
    private void setOnClick() {
         /*
         @选择测绘杆
         */
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                MappingRodNumber = spinner1.getSelectedItem().toString();

                //避开第一次选择触发
                if(true == selectedFirst){
                    selectedFirst =false;
                }else{
                    if (binder != null) {
                        if(mBleDeviceList.size() > 0) {
                            //停止扫描蓝牙
                            binder.stopScanBle();
                            binder.connectBle(mBleDeviceList.get(i),false);
                            Log.d(TAG,"选择测绘杆"+i);

                        }else {
                            if (binder != null) {
                                binder.startScanBle();
                            }

                        }
                    }

                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //测绘类型选择
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                MappingType  = spinner2.getSelectedItem().toString();
                if(0 == i){
                    MappingType = "M_";  //主干道
                }else
                {
                    MappingType = "L_";  //果园
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //开始测绘按钮
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //   Log.d("debug001",MappingRodNumber+MappingType);
               if(ble_connectFlag == true) {
                    //保存测绘类型
                    if (editText1.getText().toString().equals("")) {
                   //     bPoint.x = 113.894753;
                          bPoint.x = 0;
                    } else {
                        bPoint.x = Double.parseDouble(editText1.getText().toString());
                    }
                    if (editText1.getText().toString().equals("")) {
                    //    bPoint.y = 22.958744;
                        bPoint.y = 0;

                    } else {
                        bPoint.y = Double.parseDouble(editText2.getText().toString());
                    }
                    //      Log.d("Tank001","经度"+bPoint.x+"纬度"+bPoint.y);

                    infoEditor.putString("MappingType", MappingType);
                    infoEditor.putLong("basicRTK.X", (long) ((bPoint.x*MappingGroup.PI/180) * 10000000000l));
                    infoEditor.putLong("basicRTK.Y", (long) ((bPoint.y*MappingGroup.PI/180) * 10000000000l));
                    infoEditor.commit();

                    startActivity(intentMap);

                }else {
                    Toast.makeText(getActivity(), "未连接测绘，无法测绘" ,
                            Toast.LENGTH_SHORT).show();

                }



            }
        });

        //结束测绘按钮
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //断开蓝牙
                if (binder !=null) {
                    binder.unconnectBle();//断开蓝牙连接
                }
                startActivity( intentCenter);

            }
        });
    }

    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            //在handler中更新UI
            switch(msg.what){
                case  BLE_SCAN_ON:

                    Log.d(TAG,"ConnectRTK搜索到蓝牙："+mBleDeviceList.size());
                   if (mBleDeviceList.size()>0) {
                       String[] listRTK = new String[mBleDeviceList.size()];
                       for (int i = 0; i < mBleDeviceList.size(); i++) {
                           listRTK[i] = mBleDeviceList.get(i).getName().toString();
                        //   Log.d(TAG,listRTK[i]);
                       }
                       selectedFirst = true;
                       //填充到spinner
                       MySpinnerAdapter adapter;
                       adapter = new MySpinnerAdapter(getActivity(),
                               android.R.layout.simple_spinner_item, listRTK);
                       spinner1.setAdapter(adapter);

                   }
                    break;
                case BLE_SCAN_OFF:

                       String[]  listRTK =  new String[2];

                       listRTK[0] = "没搜索到，点击刷新";
                       mBleDeviceList.clear();
                        //填充到spinner
                        MySpinnerAdapter adapter;
                        adapter = new MySpinnerAdapter(getActivity(),
                                android.R.layout.simple_spinner_item, listRTK);
                        spinner1.setAdapter(adapter);
              //      Toast.makeText(getActivity(), "没有搜索到测绘杆" ,
               //             Toast.LENGTH_SHORT).show();
                    selectedFirst = true;
                    break;
                case BLE_DATA_ON:

                    //接收到数据
            //        byte[] buf_read = (byte[]) msg.obj;

                    break;
                case BLE_CONNECT_OFF:
                    String[]  list =  new String[2];
                    list[0] = "连接断开，点击新搜索";
                    mBleDeviceList.clear();
                    //填充到spinner
                    adapter = new MySpinnerAdapter(getActivity(),
                            android.R.layout.simple_spinner_item, list);
                    spinner1.setAdapter(adapter);
                    ble_connectFlag =false;
                    selectedFirst = true;
                    break;
                case BLE_CONNECT_ON:
                    ble_connectFlag =true;

                    Toast.makeText(getActivity(), "测绘杆连接成功" ,
                            Toast.LENGTH_SHORT).show();
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

            //绑定后执行动作

            binder.setBleWorkTpye(BLEService.BLE_MAP_CONECT,true);
            binder.startScanBle();//搜索蓝牙

            binder.getService().setMappingCallback(new BLEService.MappingCallback() {
                //执行回调函数
                @Override
                public void BleScanChanged(ArrayList<BluetoothDevice> bleDeviceList) {

                    if (bleDeviceList.size()>0) {
                        mBleDeviceList.clear();
                        mBleDeviceList.addAll(bleDeviceList.subList(0, bleDeviceList.size()));

                    //    Log.d(TAG, "查找到蓝牙个数：" + mBleDeviceList.size());
                        //发送通知
                        handler.sendEmptyMessage( BLE_SCAN_ON);
                    }

                }
                //BLE接收到数据
                @Override
                public void BleDataChanged(MappingGroup rtkMap) {
                    //发送通知
             //       Log.d(TAG,"接收到数据：");
                }
                //BLE状态变化
                @Override
                public void BleStateChanged(int msg) {

                    if(msg == BLE_CONNECT_OFF){
                        Log.d(TAG,"RTK蓝牙断开连接");
                    }
                    handler.sendEmptyMessage(msg);

                }

                @Override
                public void BleConnectedDevice(BluetoothDevice connectedDevice) {
                    mBleDeviceList.clear();
                    mBleDeviceList.add(connectedDevice);
                    handler.sendEmptyMessage( BLE_SCAN_ON);
                    Log.d(TAG,"接收到正在连接的蓝牙");
                }
            });
        }

        // 服务奔溃或者被杀掉执行
        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    }

    /*
*初始化数据
 */
    private void initData() {

        //
        String [] listRTK= new String[1];
        listRTK[0] = "点击搜索测绘杆...";
        String [] listMapingType= (String[])getResources().getStringArray(R.array.MapingType_spinner);

        //填充到spinner
        MySpinnerAdapter adapter;
        adapter = new MySpinnerAdapter(this.getActivity(),
                android.R.layout.simple_spinner_item, listRTK);
        spinner1.setAdapter(adapter);
        adapter = new MySpinnerAdapter(this.getActivity(),
                android.R.layout.simple_spinner_item, listMapingType);
        spinner2.setAdapter(adapter);

        bPoint.set((double) setinfo.getLong("basicRTK.X", 0)/10000000000l,(double) setinfo.getLong("basicRTK.Y", 0)/10000000000l,0,0);

        editText1.setText(""+bPoint.x);
        editText2.setText(""+bPoint.y);
    }

}
