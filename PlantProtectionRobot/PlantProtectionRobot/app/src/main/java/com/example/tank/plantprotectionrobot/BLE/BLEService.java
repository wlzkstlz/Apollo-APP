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
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;
import com.example.tank.plantprotectionrobot.R;

import java.util.ArrayList;
import java.util.List;

import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getDouble;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;

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
    private final int BLE_CONNECT_ON = 20; //蓝牙连接成功
    private final int BLE_CONNECT_OFF =21;//练呀连接失败
    private final int BLE_SCAN_OFF = 11;
    private final int BLE_SCAN_ON = 10; //扫描到蓝牙
    private final int BLE_DATA_ON = 30; //接收到数据
    private final int BLE_CONNECTED=40;//当前正在连接的蓝牙反馈

    private final int SCAN_PERIOD =5000;//蓝牙搜索时间

    private final int BLE_MAPPING = 1;//测绘蓝牙
    private final int BLE_HANDLE = 2;//手柄蓝牙
    private final int BLE_MASTER = 3;//算法板蓝牙
    private final int BLE_BASIC = 4;//基站蓝牙

    //蓝牙
    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**读写BLE终端*/
    private  BluetoothLeClass mBLE;
    /**蓝牙管理器**/
    private BluetoothManager bluetoothManager;
    private BluetoothDevice isConnectedBle;

    private ArrayList<BluetoothDevice> mBleDeviceList = new ArrayList<BluetoothDevice>();
    private final Handler scanHandler = new Handler();
    private final Handler handler = new Handler();

    public static BluetoothGattCharacteristic mCharacteristic;    //当前连接的

    private final static String UUID_KEY_DATA = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static String UUID_KEY_CAR = "0000fff1-0000-1000-8000-00805f9b34fb";//车上用的蓝牙

    private int revCount =0;
    private boolean revStart =false;
    private byte[] revBuf = new byte[200];
    private int revWaitTime = 0; //接收等待时间，超时侧重新开始接收数据，防止从中间截断数据

    //**蓝牙工作工况**//
    private int bleWorkTpye =0;


    // 必须实现的方法，用于返回Binder对象
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"--onBind()--");

        msgWhat=BLE_CONNECTED;
        return new BleBinder();
    }

    public class BleBinder extends Binder {
        public  BLEService getService() {
            return BLEService.this;
        }
        //开始搜索蓝牙
        public void startScanBle(){
            //开启蓝牙
            if (mBluetoothAdapter != null || !mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }

            mBleDeviceList.clear();
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
                isConnectedBle=null;
            }
        }
        //开始连接蓝牙

        /***
         *
         * @param bluetoothDevice
         * @param connetType 连接类型，true 表示掉线重连接 fale 重新连接新地址
         * @return
         */
        public boolean connectBle(BluetoothDevice bluetoothDevice,boolean connetType){
            if(mBluetoothAdapter !=null) {
                if ( connetType == false) {
                    if (mBluetoothAdapter.checkBluetoothAddress(bluetoothDevice.getAddress())) {

                        mBLE.connect(bluetoothDevice.getAddress());
                        isConnectedBle = bluetoothDevice;
                        Log.d(TAG, "连接蓝牙：" + bluetoothDevice.toString());
                    } else {
                        return false;
                    }
                }else {
                    //若参数
                    if(isConnectedBle !=null){
                        mBLE.connect(isConnectedBle.getAddress());
                    }
                }
            }else{
                return false;
            }
            return true;
        }


        /***
         *
         * @param type 1连接测绘蓝牙，2连接手柄蓝牙，3连接算法板蓝牙 4连接基站蓝牙
         */
        public void setBleWorkTpye(int type){
            bleWorkTpye =type;
        }

    }

    // 创建Service时调用该方法，只调用一次
    double myPiont=0;
    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"--onCreate()--");

        serviceRunning = true;

        new Thread() {
            @Override
            public void run() {

                while (serviceRunning) {

                    if (dataCallback != null) {
                        //      dataCallback.dataChanged(str);
                        switch(msgWhat){
                            case BLE_SCAN_ON:
                                ArrayList<BluetoothDevice> mBleList =new ArrayList<BluetoothDevice>();
                                mBleList.addAll(mBleDeviceList.subList(0,mBleDeviceList.size()));
                                if (dataCallback != null) {
                                    dataCallback.BleScanChanged(mBleList);
                                }
                               //       Log.d(TAG, "扫描到设备" + mBleDeviceList.get(mBleDeviceList.size()-1).getName().toString()+" ID"+
                               //               mBleDeviceList.get(mBleDeviceList.size()-1).getAddress().toString() +"->"+ mBleDeviceList.size());
                               msgWhat=0;
                               break;
                            case BLE_SCAN_OFF:
                                if (dataCallback != null) {
                                    dataCallback.BleStateChanged(msgWhat);
                                }
                                msgWhat=0;
                                Log.d(TAG,"BEL蓝牙断开连接");
                                break;
                           case BLE_CONNECT_ON: //蓝牙连接成功，回调

                               if (dataCallback != null) {
                                   dataCallback.BleStateChanged(msgWhat);
                               }
                               msgWhat=0;
                               break;
                            case BLE_CONNECT_OFF: //蓝牙连接失败，返回
                                if (dataCallback != null) {
                                    dataCallback.BleStateChanged(msgWhat);
                                }
                                msgWhat=0;
                                break;

                            case BLE_CONNECTED:
                                if (dataCallback != null && isConnectedBle !=null) {
                                    dataCallback.BleConnectedDevice(isConnectedBle);
                                }
                                msgWhat=0;
                                break;
                            default:
                                break;
                        }


                        if(revWaitTime>0){
                            revWaitTime--; //RTK传输数据周期是100ms，超过50ms还没接收完侧认为数据被截断
                        }

                    }

                    /*
                        myPiont=myPiont+0.0000005;
                        MappingGroup rtkMap=new MappingGroup();
                        rtkMap.rtkState=1;   //RTK转台
                        rtkMap.GPSTime_tow = 100000; //时间
                        rtkMap.longitude = 113.894753+myPiont; //经度
                        rtkMap.latitude = 22.958744+myPiont; //纬度
                        rtkMap.altitude = 49.3000000; //海拔
                        rtkMap.direction = myPiont*1000000; //方向

                        //若有应用绑定服务，发送数据到该应用
                        if(dataCallback != null) {
                            dataCallback.BleDataChanged(rtkMap);
                        }
                    */
                    try {
                        sleep(10); //延时
                    //   sleep(100); //测试用延时
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

                scanLeDevice(true);

                //初始化
                mBLE = new BluetoothLeClass(this);
                if (!mBLE.initialize()) {
                    Toast.makeText(this, "蓝牙服务初始化失败", Toast.LENGTH_SHORT).show();
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

        return super.onStartCommand(intent, flags, startId);
    }

    // 解绑Servcie调用该方法
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"--onUnbind()--");
        dataCallback = null;//解除绑定后清除回调类
        return super.onUnbind(intent);
    }

    // 退出或者销毁时调用该方法
    @Override
    public void onDestroy() {
        serviceRunning = false;
      Log.d(TAG,"--onDestroy()--");
        super.onDestroy();
    }

    DataCallback dataCallback = null;

    public DataCallback getDataCallback() {
        return dataCallback;
    }

    public void setDataCallback(DataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }

    /***
     *  通过回调机制，将Service内部的变化传递到外部
     *  what 1返回蓝牙搜索数据 2返回蓝牙接收的数据
     */
    public interface DataCallback {
        void BleDataChanged(MappingGroup rtkMap); //接收数据回调
        void BleStateChanged(int msg);     //蓝牙状态回掉
        void BleScanChanged(ArrayList<BluetoothDevice> mBleDeviceList); //蓝牙搜索回调
        void BleConnectedDevice(BluetoothDevice connectedDevice);//返回正在连接的蓝牙设备

    }

    /***
     * 蓝牙搜索回调函数
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                            mBleDeviceList.add(device);
                            msgWhat = BLE_SCAN_ON;
                    //       Log.d(TAG, "扫描到设备" + mBleDeviceList.get(mBleDeviceList.size()-1).getName().toString()+" ID"+
                     //               mBleDeviceList.get(mBleDeviceList.size()-1).getAddress().toString() +"->"+ mBleDeviceList.size());
                }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
              //      Log.d(TAG,"没有搜索到蓝牙");
                    if(mBluetoothAdapter !=null){
                       mBluetoothAdapter.stopLeScan(mLeScanCallback);
                       if(mBleDeviceList.size() == 0) {//没有查找到测绘杆
                             msgWhat = BLE_SCAN_OFF;
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
            Log.i(TAG,gatt.getDevice().getAddress()+":连接成功");
            msgWhat = BLE_CONNECT_ON;
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
            mCharacteristic = null;
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

             Log.d(TAG,"蓝牙数据帧： "+characteristic.getValue().length +" -> " +Utils.bytesToHexString(characteristic.getValue()));

           switch (bleWorkTpye) {
               case BLE_MAPPING:

               byte[] buf = characteristic.getValue();

               if(revCount == 0 && buf[0] == 0x55  && revWaitTime ==0){
                   revStart = true;
                   revBuf[0]=0;
               }

               if(revStart == true) {
                   revCount++;

                   System.arraycopy(buf, 0, revBuf, revBuf[0] + 1, buf.length);
                   revBuf[0] += (byte) buf.length;//数组起始地址偏移
               }
               if (revCount == 3) {

                   revStart = false;
                   revCount=0;
                   byte[] bleBuf = new byte[revBuf[0]];
                   //复制缓存数据，除去第一位后为实际接收数据
                   System.arraycopy(revBuf, 1, bleBuf, 0, revBuf[0]);
                   //      Log.d(TAG,Utils.bytesToHexString(bleBuf)+"\n");
                   if (CRC16_ccitt(bleBuf)) {//CRC校验

                       //转化成数据流转GPS数据
                       MappingGroup rtkMap = new MappingGroup();
                       int sp = 2;
                       rtkMap.rtkState = bleBuf[sp];   //RTK转台
                       sp = sp + 1;
                       rtkMap.GPSTime_tow = getInt(bleBuf, sp); //时间
                       sp = sp + 4;
                       rtkMap.latitude = getDouble(bleBuf, sp); //纬度

                       sp = sp + 8;
                       rtkMap.longitude = getDouble(bleBuf, sp); //经度
                       sp = sp + 8;
                       rtkMap.altitude = getDouble(bleBuf, sp); //海拔
                       sp = sp + 8;
                       rtkMap.direction = getDouble(bleBuf, sp); //方向

                       Log.d(TAG, "RTK状态：" + rtkMap.rtkState + "时间：" + rtkMap.GPSTime_tow + "经度：" + rtkMap.longitude
                               + "纬度：" + rtkMap.latitude + "海拔：" + rtkMap.altitude + "方向：\n" + rtkMap.direction);

                       //若有应用绑定服务，发送数据到该应用
                       if (dataCallback != null) {
                           dataCallback.BleDataChanged(rtkMap);
                       }

                   } else {
                       Log.d(TAG, "接收数据校验失败");

                       revWaitTime=5;//延时50ms,防止数据从中间截断
                       //定位失败数据全为0
                       MappingGroup rtkMap = new MappingGroup();
                       rtkMap.rtkState = 0;   //RTK转台
                       rtkMap.GPSTime_tow = 0; //时间
                       rtkMap.latitude = 0; //纬度
                       rtkMap.longitude = 0; //经度
                       rtkMap.altitude = 0; //海拔
                       rtkMap.direction = 0; //方向
                   }
                   revCount = 0;
                   //     Log.d(TAG,"蓝牙接收数据： "+bleBuf.length +" -> " +Utils.bytesToHexString(bleBuf));

               }
                   break;
               case BLE_HANDLE:
                   break;
               case BLE_MASTER:
                   break;
               case BLE_BASIC:
                   break;
           }

        }
    };

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

                    mCharacteristic = gattCharacteristic;
                }else if(gattCharacteristic.getUuid().toString().equals(UUID_KEY_CAR)){
                    //设置串口可接收通知的，设置其可以接收通知（notification）
                    mBLE.setCharacteristicNotification(gattCharacteristic, true);

                    Log.i(TAG,"连接到串口BLE");

                    mCharacteristic = gattCharacteristic;
                }
            }
        }
    }

    public static int CRC16_ccitt_table[] = { 0x0000, 0x1189, 0x2312, 0x329b, 0x4624, 0x57ad, 0x6536, 0x74bf, 0x8c48, 0x9dc1, 0xaf5a,
            0xbed3, 0xca6c, 0xdbe5, 0xe97e, 0xf8f7, 0x1081, 0x0108, 0x3393, 0x221a, 0x56a5, 0x472c, 0x75b7, 0x643e,
            0x9cc9, 0x8d40, 0xbfdb, 0xae52, 0xdaed, 0xcb64, 0xf9ff, 0xe876, 0x2102, 0x308b, 0x0210, 0x1399, 0x6726,
            0x76af, 0x4434, 0x55bd, 0xad4a, 0xbcc3, 0x8e58, 0x9fd1, 0xeb6e, 0xfae7, 0xc87c, 0xd9f5, 0x3183, 0x200a,
            0x1291, 0x0318, 0x77a7, 0x662e, 0x54b5, 0x453c, 0xbdcb, 0xac42, 0x9ed9, 0x8f50, 0xfbef, 0xea66, 0xd8fd,
            0xc974, 0x4204, 0x538d, 0x6116, 0x709f, 0x0420, 0x15a9, 0x2732, 0x36bb, 0xce4c, 0xdfc5, 0xed5e, 0xfcd7,
            0x8868, 0x99e1, 0xab7a, 0xbaf3, 0x5285, 0x430c, 0x7197, 0x601e, 0x14a1, 0x0528, 0x37b3, 0x263a, 0xdecd,
            0xcf44, 0xfddf, 0xec56, 0x98e9, 0x8960, 0xbbfb, 0xaa72, 0x6306, 0x728f, 0x4014, 0x519d, 0x2522, 0x34ab,
            0x0630, 0x17b9, 0xef4e, 0xfec7, 0xcc5c, 0xddd5, 0xa96a, 0xb8e3, 0x8a78, 0x9bf1, 0x7387, 0x620e, 0x5095,
            0x411c, 0x35a3, 0x242a, 0x16b1, 0x0738, 0xffcf, 0xee46, 0xdcdd, 0xcd54, 0xb9eb, 0xa862, 0x9af9, 0x8b70,
            0x8408, 0x9581, 0xa71a, 0xb693, 0xc22c, 0xd3a5, 0xe13e, 0xf0b7, 0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64,
            0x5fed, 0x6d76, 0x7cff, 0x9489, 0x8500, 0xb79b, 0xa612, 0xd2ad, 0xc324, 0xf1bf, 0xe036, 0x18c1, 0x0948,
            0x3bd3, 0x2a5a, 0x5ee5, 0x4f6c, 0x7df7, 0x6c7e, 0xa50a, 0xb483, 0x8618, 0x9791, 0xe32e, 0xf2a7, 0xc03c,
            0xd1b5, 0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66, 0x7eef, 0x4c74, 0x5dfd, 0xb58b, 0xa402, 0x9699, 0x8710,
            0xf3af, 0xe226, 0xd0bd, 0xc134, 0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7, 0x6e6e, 0x5cf5, 0x4d7c, 0xc60c,
            0xd785, 0xe51e, 0xf497, 0x8028, 0x91a1, 0xa33a, 0xb2b3, 0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60, 0x1de9,
            0x2f72, 0x3efb, 0xd68d, 0xc704, 0xf59f, 0xe416, 0x90a9, 0x8120, 0xb3bb, 0xa232, 0x5ac5, 0x4b4c, 0x79d7,
            0x685e, 0x1ce1, 0x0d68, 0x3ff3, 0x2e7a, 0xe70e, 0xf687, 0xc41c, 0xd595, 0xa12a, 0xb0a3, 0x8238, 0x93b1,
            0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62, 0x3ceb, 0x0e70, 0x1ff9, 0xf78f, 0xe606, 0xd49d, 0xc514, 0xb1ab,
            0xa022, 0x92b9, 0x8330, 0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3, 0x2c6a, 0x1ef1, 0x0f78 };

    public static boolean CRC16_ccitt(byte [] pSrcData) {
        int crc_reg = 0x0000;
        for (int i = 1; i < pSrcData.length-2; i++) {
            crc_reg =  CRC16_ccitt_table[(crc_reg ^ pSrcData[i]) & 0xFF] ^ (crc_reg >> 8);
        }
        crc_reg &= 0xffff;
        String strCrc = Integer.toHexString(crc_reg).toUpperCase();
        Log.d(TAG,"校验值1："+strCrc);

        if(crc_reg == ((pSrcData[pSrcData.length-2] & 0xff) | (pSrcData[pSrcData.length-1]<<8 & 0xff00))){
            return true;
        }else {
            return false;
        }


    }

}
