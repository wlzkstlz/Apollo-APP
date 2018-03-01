package com.example.tank.plantprotectionrobot.BLE;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.DataProcessing.GpsPoint;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.R;
import com.example.tank.plantprotectionrobot.Robot.CommondType;
import com.example.tank.plantprotectionrobot.Robot.HeatDataMsg;
import com.example.tank.plantprotectionrobot.Robot.PollingManagement;
import com.example.tank.plantprotectionrobot.Robot.RobotManagement;
import com.example.tank.plantprotectionrobot.Robot.TankRobot;
import com.example.tank.plantprotectionrobot.Robot.WorkMatch;
import com.example.tank.plantprotectionrobot.WaveFiltering.RobotCruisePath;

import java.util.ArrayList;
import java.util.List;

import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getFloat;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getShort;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.putFloat;

/**
 * Created by TK on 2018/1/22.
 */

public class BLEService extends Service {
    private String data = "";
    private static  String TAG = "Tank001";
    private boolean serviceRunning = false;
   //发送消息标志
    private int msgWhat = 0;
    //接收串口数据标志

    //蓝牙扫描时间
    public static final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    public static final int BLE_CONNECT_OFF =21;//练呀连接失败
    public static final int BLE_SCAN_OFF = 11;
    public static final int BLE_SCAN_ON = 10; //扫描到蓝牙
    public static final int BLE_CONNECTED=40;//当前正在连接的蓝牙反馈
    public static final int BLE_SEND_ROUTE_END= 50;//路径文件传输完

    private final int SCAN_PERIOD =5000;//蓝牙搜索时间

    public static final int  SERV_BLE_NULL = 0;//不做任何事


    //蓝牙工作类型ID
    public static final int BLE_MAP_CONECT=10; //测绘蓝牙
    public static final int BLE_HANDLE_CONECT=20;//手柄蓝牙
    public static final int BLE_ROBOT_CONECT=30;//算法板蓝牙
    public static final int BLE_BASIC_CONECT=40;//基站蓝牙
    public static final int BLE_NO_CONECT = 0;//没有蓝牙连接

    //蓝牙
    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**读写BLE终端*/
    private  BluetoothLeClass mBLE;
    /**蓝牙管理器**/
    private BluetoothManager bluetoothManager;

    private final Handler scanHandler = new Handler();

    private final static String UUID_KEY_DATA = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_CAR = "0000fff1-0000-1000-8000-00805f9b34fb";//车上用的蓝牙


    private MappingCallback mappingCallback = null;
    private RobotWorkingCallback robotWorkingCallback =null;

    //蓝牙信息处理类
    private WorkBleGroup workBleGroup = new WorkBleGroup();
    //分时轮询
    private PollingManagement pollingManagement = new PollingManagement();
    //机器人管理类
    private RobotManagement robotManagement = new RobotManagement(pollingManagement,workBleGroup);

    // 解绑Servcie调用该方法
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"--onUnbind()--");
        return super.onUnbind(intent);
    }

    // 必须实现的方法，用于返回Binder对象
    @Override
    public IBinder onBind(Intent intent) {

        Log.d(TAG,"--onBind()--");

        msgWhat=BLE_CONNECTED;

        //返回测绘正在连接的蓝牙
        if (mappingCallback != null && workBleGroup.isWorkingId == BLE_MAP_CONECT) {
            if(workBleGroup.bleMapCneted !=null) {
                mappingCallback.BleConnectedDevice(workBleGroup.bleMapCneted);
            }
        }
        //返回遥控器蓝牙状态
        if(robotWorkingCallback !=null && workBleGroup.isWorkingId == BLE_HANDLE_CONECT){
            if(workBleGroup.bleHandleCneted !=null) {
                robotWorkingCallback.BleConnectedDevice(workBleGroup.bleHandleCneted);
            }
        }

        return new BleBinder();
    }

    /***
     *
     */
    public class BleBinder extends Binder {
        public  BLEService getService() {
            return BLEService.this;
        }

        //获取正在工作的蓝牙类型Id
        public int getIsWorkingId(){
            return workBleGroup.isWorkingId;
        }

        /***
         * 开始传输路径文件
         */
        public void sendRouteData(final RobotCruisePath robotCruisePath){

            new Thread(){
                @Override
                public void run() {
                    /*
                    for(int i=0;i<route.size();i++){
                         if(workBleGroup.isConnectGattCh == null){
                        workBleGroup.sendCommand();
                        }
                    }*/
                    byte[] data = new byte[19];
                    for(int i=0;i<19;i++){
                        data[i] = 0x55;
                    }

                    byte[]bbuf =new byte[16];
                    int index=0;
                    putDouble(bbuf,robotCruisePath.bPoint.x,index);
                    index+=8;
                    putDouble(bbuf,robotCruisePath.bPoint.y,index);

                    workBleGroup.sendCommand(bbuf);//基站

                    for(int i=0;i<robotCruisePath.mPathPoints.size();i++){//PathPoint数据为20字节，一个蓝牙数据包

                        byte[] pbuf = robotCruisePath.PathPoitToArray(robotCruisePath.mPathPoints.get(i));
                        workBleGroup.sendCommand(pbuf);

                        try {
                            sleep(1); //延时
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }


                    if(robotWorkingCallback !=null){//文件传输完
                        robotWorkingCallback.BleStateChanged(BLE_SEND_ROUTE_END);
                    }
                    super.run();
                }
            }.start();

        }
        /***
         * 设置时间碎片分配，即是进入工作地图界面的机器人分配更多时间碎片
         */
        public void IntoWorkMapPage(boolean workMapPage){

            if(workMapPage == true) {
                pollingManagement.setHaveWorkMapPageCtr(true);
            }else{

                if (robotManagement.workRobotList != null) {//取消进入工作地图界面标志
                    for (int i = 0; i < robotManagement.workRobotList.size(); i++) {
                        robotManagement.workRobotList.get(i).inWorkPage = false;
                    }
                }

                pollingManagement.setHaveWorkMapPageCtr(false);
            }

        }

        public void nextPage(String page,String nextPage){

        }
        /***
         * 获取在线的机器人列表
         * @return
         */
        public  ArrayList<TankRobot> getRobotList(){
            return robotManagement.workRobotList;
        }

        /***
         * @param workRobot 当前工作的机器人
         */
        public void changeWorkRobotList(ArrayList<TankRobot> workRobot){
            robotManagement.workRobotList=workRobot;
        }
        /*** 添加机器人
         * @param workRobot 新添加的机器人
         */
        public void addWorkRobot(TankRobot workRobot){
            robotManagement.workRobotList.add(workRobot);
            robotManagement.pollCount = robotManagement.workRobotList.size()-1;//
            //----------测试---------//
            if(robotManagement.workRobotList.size() >=2 ){
                robotManagement.workRobotList.get(robotManagement.workRobotList.size()-2).workAuto = TankRobot.CTR_AUTO;
            }
        }
        /***
         *
         * @param type 1连接测绘蓝牙，2连接手柄蓝牙，3连接算法板蓝牙 4连接基站蓝牙
         */
        public void setBleWorkTpye(int type,boolean newPage){
            if(newPage == true) {
                mappingCallback = null;
                robotWorkingCallback = null;

                robotManagement.robotWorkingCallback = robotWorkingCallback;
            }

            if(type != workBleGroup.isWorkingId){
                mBLE.disconnect();
                mBLE.close();
                workBleGroup.isConnectGattCh = null;
                Log.d(TAG," Bleservice->断开连接");
            }
            //重新连接时，先清除之前的回调函数
            workBleGroup.isWorkingId = type;

        }

        //开始搜索蓝牙
        public void startScanBle(){
            //开启蓝牙
            if (mBluetoothAdapter != null) {
                 if(!mBluetoothAdapter.isEnabled()) {
                     mBluetoothAdapter.enable();
                 }
            }
            workBleGroup.findBleList.clear();
            scanLeDevice(true);
        }
        //停止搜索蓝牙
        public void stopScanBle(){
            if(mBluetoothAdapter !=null) {
                if (mBluetoothAdapter.isEnabled()) {
                    scanLeDevice(false);
                }
            }
        }
        //断开蓝牙连接
        public void unconnectBle()
        {
            if(mBLE !=null) {
                mBLE.disconnect();
                mBLE.close();
                workBleGroup.isWorkingId = 0;
                workBleGroup.isConnectGattCh = null;
            }
        }
        //开始连接蓝牙
        /***
         *
         * @param bluetoothDevice
         * @param connetType 连接类型，true 表示掉线重连接 false 重新连接新地址
         * @return
         */
        public boolean connectBle(BluetoothDevice bluetoothDevice,boolean connetType){
            if(mBluetoothAdapter !=null) {
                if ( connetType == false) {
                    if (mBluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress())) {

                        mBLE.connect(bluetoothDevice.getAddress());
                        switch (workBleGroup.isWorkingId){
                            case BLE_HANDLE_CONECT:
                                workBleGroup.bleHandleCneted=bluetoothDevice;
                                workBleGroup.bleRobotCneted=null;
                                workBleGroup.bleMapCneted=null;
                                workBleGroup.bleBasicCneted=null;
                                break;
                            case BLE_BASIC_CONECT:
                                workBleGroup.bleHandleCneted=null;
                                workBleGroup.bleRobotCneted=null;
                                workBleGroup.bleMapCneted=null;
                                workBleGroup.bleBasicCneted=bluetoothDevice;
                                break;
                            case BLE_MAP_CONECT:
                                workBleGroup.bleHandleCneted=null;
                                workBleGroup.bleRobotCneted=null;
                                workBleGroup.bleMapCneted=bluetoothDevice;
                                workBleGroup.bleBasicCneted=null;
                                break;
                            case BLE_ROBOT_CONECT:
                                workBleGroup.bleHandleCneted=null;
                                workBleGroup.bleRobotCneted=bluetoothDevice;
                                workBleGroup.bleMapCneted=null;
                                workBleGroup.bleBasicCneted=null;
                                break;
                        }
             //           workBleGroup.isConnectedBle = bluetoothDevice;

                        Log.d(TAG, "连接蓝牙：" + bluetoothDevice.toString());
                    } else {
                        return false;
                    }
                }else {
                    /*
                    //若参数
                    if (workBleGroup.isConnectedBle != null) {
                        mBLE.connect(workBleGroup.isConnectedBle.getAddress());
                        Log.d(TAG, "接收蓝牙连接指令连接蓝牙");
                    }
                    */
                    switch (workBleGroup.isWorkingId) {
                        case BLE_HANDLE_CONECT:
                            mBLE.connect(workBleGroup.bleHandleCneted.getAddress());
                            break;
                        case BLE_BASIC_CONECT:
                            mBLE.connect(workBleGroup.bleBasicCneted.getAddress());
                            break;
                        case BLE_MAP_CONECT:
                            mBLE.connect(workBleGroup.bleMapCneted.getAddress());
                            break;
                        case BLE_ROBOT_CONECT:
                            mBLE.connect(workBleGroup.bleRobotCneted.getAddress());
                            break;
                    }

                }
            }else{
                return false;
            }
            return true;
        }

    }

    // 创建Service时调用该方法，只调用一次
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"--onCreate()--");
        serviceRunning = true;
        new Thread() {
            @Override
            public void run() {
                while (serviceRunning) {
                    if (mappingCallback != null || robotWorkingCallback != null) {

                        switch(msgWhat){
                            case BLE_SCAN_ON:
                                msgWhat=0;
                               break;
                            case BLE_SCAN_OFF://没有扫描到
                                msgWhat=0;
                                break;
                           case BLE_CONNECT_ON: //蓝牙连接成功，回调
                               msgWhat=0;
                               break;
                            case BLE_CONNECT_OFF: //蓝牙连接失败，返回
                                msgWhat=0;
                                break;
                            case BLE_CONNECTED:
                                msgWhat=0;
                                break;
                            default:
                                break;
                        }

                    }
                    try {
                        sleep(10); //延时
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
    }

    // 每次启动Servcie时都会调用该方法
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"--onStartCommand()--");
      //  data = intent.getStringExtra("data");
       openBle();

        return super.onStartCommand(intent, flags, startId);
    }

    // 退出或者销毁时调用该方法
    @Override
    public void onDestroy() {
        serviceRunning = false;
      Log.d(TAG,"--onDestroy()--");
        super.onDestroy();
    }
/**********************************作业时回调start*******************************************/

public interface RobotWorkingCallback {
    void RobotStateChanged(TankRobot tankRobot);
    void BleStateChanged(int msg);     //蓝牙状态回掉
    void BleConnectedDevice(BluetoothDevice connectedDevice);//返回正在连接的蓝牙设备
    void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList); //蓝牙搜索回调
    void ComdReturnChange(HeatDataMsg heatDataMsg);//执行成功的指令返回
}
public void setRobotWorkingCallback(RobotWorkingCallback  callback){

    this.robotWorkingCallback = callback;
    robotManagement.robotWorkingCallback = this.robotWorkingCallback;
}
public RobotWorkingCallback getRobotWorkingCallback(){
    return this.robotWorkingCallback;
}

 /*********************************作业时回调end********************************************/


/**********************************测绘时的回调函数start*******************************************/
    public MappingCallback getMappingCallback() {
        return mappingCallback;
    }

    public void setMappingCallback(MappingCallback mappingCallback) {
        this.mappingCallback = mappingCallback;
    }
    /***
     *  通过回调机制，将Service内部的变化传递到外部
     *  what 1返回蓝牙搜索数据 2返回蓝牙接收的数据
     */
    public interface MappingCallback {
        void BleDataChanged(MappingGroup rtkMap); //接收数据回调
        void BleStateChanged(int msg);     //蓝牙状态回掉
        void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList); //蓝牙搜索回调
        void BleConnectedDevice(BluetoothDevice connectedDevice);//返回正在连接的蓝牙设备

    }
/**********************************测绘时的回调函数start********************************************/

/****************************************蓝牙监听start**********************************************/


    /***
     * 开启蓝牙以及以及监听事件
     */
    private void openBle(){

        //开启蓝牙
        if(mBluetoothAdapter == null){

            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, "手机不支持蓝牙", Toast.LENGTH_SHORT).show();

            }

            //开启蓝牙
            if (mBluetoothAdapter != null || !mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }

            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "手机不支持蓝牙", Toast.LENGTH_SHORT).show();
            }else {

                //   scanLeDevice(true);
                //初始化
                mBLE = new BluetoothLeClass(this);

                workBleGroup.mBLE = mBLE; //将对象传递给蓝牙管理

                if (!mBLE.initialize()) {
                    Toast.makeText(this, "蓝牙服务初始化失败", Toast.LENGTH_SHORT).show();
                    workBleGroup.mBLE =null;
                }
                //发现BLE终端的Service时回调
                mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
                //收到BLE终端数据交互的事件
                mBLE.setOnDataAvailableListener(mOnDataAvailable);
                //BLE设备断开连接交互
                mBLE.setOnDisconnectListener(mOnDisconnectListener);
                //BLE设备连接上交互
                mBLE.setOnConnectListener(mOnConnectListener);
            }

        }

    }

    /***
     * 蓝牙搜索回调函数
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    //      addScanLeDevice(device);
                    workBleGroup.addBleDevice(workBleGroup.isWorkingId,device);
                    if(workBleGroup.findBleList.size()>0){
                        msgWhat = BLE_SCAN_ON;
                  //      Log.d(TAG,"BleService->添加蓝牙"+workBleGroup.findBleList.get(0).getName().toString());

                        if(workBleGroup.findBleList.size()>0) {

                            ArrayList<BluetoothDevice> mBleList = new ArrayList<BluetoothDevice>();
                            mBleList.addAll(workBleGroup.findBleList.subList(0, workBleGroup.findBleList.size()));
                            if (mappingCallback != null) {
                                mappingCallback.BleScanChanged(mBleList);
                                Log.d(TAG,"BleService-> mappingCallback->返回搜索到的蓝牙"+workBleGroup.findBleList.get(0).getName().toString());

                            }
                            //       Log.d(TAG, "扫描到设备" + mBleDeviceList.get(mBleDeviceList.size()-1).getName().toString()+" ID"+
                            //               mBleDeviceList.get(mBleDeviceList.size()-1).getAddress().toString() +"->"+ mBleDeviceList.size());
                            if (robotWorkingCallback != null) {
                                robotWorkingCallback.BleScanChanged(mBleList);
                                Log.d(TAG,"BleService->robotWorkingCallback->返回搜索到的蓝牙"+workBleGroup.findBleList.get(0).getName().toString());

                            }

                        }

                    }

                }
            };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if(mBluetoothAdapter !=null){
                       mBluetoothAdapter.stopLeScan(mLeScanCallback);

                        if(workBleGroup.findBleList.size() == 0) {//没有查找到测绘杆
                            msgWhat = BLE_SCAN_OFF;

                            if (mappingCallback != null) {
                                mappingCallback.BleStateChanged(msgWhat);
                            }
                            if(robotWorkingCallback != null){
                                robotWorkingCallback.BleStateChanged(msgWhat);
                            }

                        }

                    }
                }
            }, SCAN_PERIOD);

         //   Log.d(TAG,"开始搜索蓝牙");
            if(mBluetoothAdapter !=null) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
        } else {
            if(mBluetoothAdapter !=null) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }

    //BLE设备连接监听,当设备连接上后会调用此函数
    private BluetoothLeClass.OnConnectListener mOnConnectListener
            = new  BluetoothLeClass.OnConnectListener(){
        @Override
        public void onConnect(BluetoothGatt gatt) {
       //     Log.i(TAG,gatt.getDevice().getAddress()+":连接成功");
         //   msgWhat = BLE_CONNECT_ON;
        }
    };

    //BLE设备断开连接监听，当设备断开连接是会调用次函数
    private BluetoothLeClass.OnDisconnectListener mOnDisconnectListener
            = new BluetoothLeClass.OnDisconnectListener(){
        @Override
        public void onDisconnect(BluetoothGatt gatt) {
            Log.i(TAG,gatt.getDevice().getAddress()+":连接断开");
            msgWhat = BLE_CONNECT_OFF ;
        //    mBLE.close();
       //     mCharacteristic = null;

            if (mappingCallback != null) {
                mappingCallback.BleStateChanged(msgWhat);
            }

            if(robotWorkingCallback != null) {
                robotWorkingCallback.BleStateChanged(msgWhat);
            }

            workBleGroup.isConnectGattCh = null;
      //      isConnectedBle=null;


        }
    };


    //搜索到BLE终端服务的事件
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover
            = new BluetoothLeClass.OnServiceDiscoverListener(){

        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
            displayGattServices(mBLE.getSupportedGattServices());
        }

    };


    //  收到BLE终端数据交互的事件

    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable
            = new BluetoothLeClass.OnDataAvailableListener() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            //    Log.i(TAG, "onCharRead " + gatt.getDevice().getAddress() + " -> " + Utils.bytesToHexString(characteristic.getValue()));

            }
        }
        // 收到BLE终端写入数据回调
        @Override
        public void onCharacteristicRevice(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {

          //  Log.d(TAG,"蓝牙数据帧： "+characteristic.getValue().length +" -> " +Utils.bytesToHexString(characteristic.getValue()));

            switch(workBleGroup.isWorkingId) {
                case BLEService.BLE_MAP_CONECT:
                MappingGroup rtkMap = workBleGroup.translateMappingData(characteristic);

                if (mappingCallback != null && rtkMap != null) {
                    mappingCallback.BleDataChanged(rtkMap);
                }
                break;
                case BLEService.BLE_HANDLE_CONECT:
                    HeatDataMsg heatDataMsg = workBleGroup.translateTaskData(characteristic);
                    translateReturnMsg(heatDataMsg);

                    break;
            }
           // readInPutBytes(characteristic);

        }
    };

    /***
     * 处理返回信息
     */
    private void translateReturnMsg(HeatDataMsg heatDataMsg){

        for (int i=0;i<robotManagement.workRobotList.size();i++){

            if(heatDataMsg.robotId == robotManagement.workRobotList.get(i).heatDataMsg.robotId){

                robotManagement.workRobotList.get(i).checkCount = 0;//掉线检测清零

                //-------------在这里处理返回指令---------//
                //-----------------------------------------//
                //机器人可能重新装载啦新指令，所以不能覆盖新指令
                if(heatDataMsg.command  != robotManagement.workRobotList.get(i).heatDataMsg.command){
                    heatDataMsg.command = robotManagement.workRobotList.get(i).heatDataMsg.command;
                }else{
                    //---------------------------//

                    //非心跳指令执行成功返回
                    if(robotWorkingCallback !=null && heatDataMsg.command != CommondType.CMD_HEARTBEAT){
                        robotWorkingCallback.ComdReturnChange(heatDataMsg);
                    }

                    //---------------------------//
                   heatDataMsg.command = CommondType.CMD_HEARTBEAT;//指令执行成功后转为心跳
                }
                robotManagement.workRobotList.get(i).heatDataMsg = heatDataMsg;//更新信息
                robotManagement.workRobotList.get(i).robotOnline = true;
           //     Log.d(TAG,"BLEService->"+robotManagement.workRobotList.get(i).workAuto);
//
                //返回值更新控制方式

                switch (heatDataMsg.curState){
                    case TankRobot.CTR_AUTO:
                        robotManagement.workRobotList.get(i).workAuto = TankRobot.CTR_AUTO;
                        break;
                    case TankRobot.CTR_HANLDE:
                        robotManagement.workRobotList.get(i).workAuto = TankRobot.CTR_HANLDE;
                        break;
                    case TankRobot.CTR_HANDLE_TURN:
                        robotManagement.workRobotList.get(i).workAuto = TankRobot.CTR_HANDLE_TURN;
                        break;

                }

                pollingManagement.onPollingNext();//收到返回进入下一组轮询
                //返回数据
                if(robotWorkingCallback !=null){
                    robotWorkingCallback.RobotStateChanged(robotManagement.workRobotList.get(i));
                }

                break;
            }
        }
    }

    //连接串口服务器
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            //-----Characteristics的字段信息-----//
            List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();

            for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {

                //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic
                if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_DATA)){
                    //设置串口可接收通知的，设置其可以接收通知（notification）
                    mBLE.setCharacteristicNotification(gattCharacteristic, true);

                    Log.i(TAG,"连接到串口BLE");
                    workBleGroup.isConnectGattCh = gattCharacteristic;
                    msgWhat = BLE_CONNECT_ON;

                    if (mappingCallback != null) {
                        mappingCallback.BleStateChanged(msgWhat);
                    }

                    if(robotWorkingCallback != null){
                        robotWorkingCallback.BleStateChanged(msgWhat);
                    }

                }else if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_CAR)){
                    //设置串口可接收通知的，设置其可以接收通知（notification）
                    mBLE.setCharacteristicNotification(gattCharacteristic, true);

                    Log.i(TAG,"连接到串口BLE");
                    workBleGroup.isConnectGattCh = gattCharacteristic;
                    msgWhat = BLE_CONNECT_ON;

                    if (mappingCallback != null) {
                        mappingCallback.BleStateChanged(msgWhat);
                    }

                    if(robotWorkingCallback != null){
                        robotWorkingCallback.BleStateChanged(msgWhat);
                    }
                }
            }
        }
    }
/****************************************蓝牙监听end****************************************************/
}
