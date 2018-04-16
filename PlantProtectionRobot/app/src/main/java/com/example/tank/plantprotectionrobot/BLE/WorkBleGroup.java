package com.example.tank.plantprotectionrobot.BLE;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.example.tank.plantprotectionrobot.Robot.HeatDataMsg;
import com.example.tank.plantprotectionrobot.DataProcessing.MappingGroup;

import java.util.ArrayList;

import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getFloat;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getInt;
import static com.example.tank.plantprotectionrobot.DataProcessing.SDCardFileTool.getShort;

/**工作组蓝牙，有四种 基站蓝牙 B开头 手柄蓝牙H开头  算法板 R开头 测绘杆M开头
 * Created by TK on 2018/2/7.
 */

public class WorkBleGroup {

    private static  String TAG = "Tank001";
    //当前连接的蓝牙
    public BluetoothDevice bleBasicCneted=null;//基站
    public BluetoothDevice bleHandleCneted=null;//遥控器
    public BluetoothDevice bleMapCneted=null;   //测绘杆
    public BluetoothDevice bleRobotCneted=null;  //机器人

    public ArrayList<BluetoothDevice> findBleList = new ArrayList<BluetoothDevice>();

    public BluetoothGattCharacteristic isConnectGattCh=null;//当前连接的串口服务
    //读写
    public   BluetoothLeClass mBLE =null;

    public int isWorkingId=0;// 当前工作的蓝牙ID

    /***
     *
     * @param id 蓝牙工作类型ID
     */
    public void transfeConnection(int id,BluetoothDevice device){


        //若有蓝牙连接先断开
        switch(isWorkingId){
            case  BLEService.BLE_BASIC_CONECT:
           //     bleBasicCneted = device;
                break;
            case  BLEService.BLE_HANDLE_CONECT:
           //     bleHandleCneted=device;
                break;
            case  BLEService.BLE_MAP_CONECT:
         //       bleMapCneted=device;
                break;
            case   BLEService.BLE_ROBOT_CONECT:
           //     bleRobotCneted=device;
                break;
        }


        isWorkingId=id;
        //需要连接的
        switch(id){
            case  BLEService.BLE_BASIC_CONECT:
                bleBasicCneted = device;
                break;
            case  BLEService.BLE_HANDLE_CONECT:
                bleHandleCneted=device;
                break;
            case  BLEService.BLE_MAP_CONECT:
                bleMapCneted=device;
                break;
            case   BLEService.BLE_ROBOT_CONECT:
                bleRobotCneted=device;
                break;
        }

    }

    public boolean addBleDevice(int id,BluetoothDevice device){

        boolean addFlag=false;
        String check="";
        //蓝牙名过滤
//        Log.i(TAG,"WorKBleGroup->添加蓝牙"+device.getName().toString());
        switch(id){
            case  BLEService.BLE_BASIC_CONECT:
                check = "[B]\\d{5}";
                break;
            case  BLEService.BLE_HANDLE_CONECT:
                check = "[H]\\d{5}";
                break;
            case  BLEService.BLE_MAP_CONECT:
                check = "[M]\\d{5}";
                break;
            case   BLEService.BLE_ROBOT_CONECT:
                check = "[R]\\d{5}";

                break;
        }

     //   Log.d(TAG,"BLEService->"+device.getName());
        if(device.getName() != null) {

            String bleName = device.getName().toString();
            //    String bleName = device.getName();
            if (bleName.matches(check)) {

                if (findBleList.size() > 0) {
                    boolean flag = false;
                    int len = findBleList.size();
                    for (int i = 0; i < len; i++) {
                        if (device.getName().toString().equals(findBleList.get(i).getName().toString())) {
                            flag = true;
                        }
                    }
                    if (flag == false) {
                        findBleList.add(device);
                        addFlag = true;
                    }
                } else {
                    findBleList.add(device);
                    addFlag = true;
                    //    Log.d(TAG,"WorKBleGroup->添加蓝牙"+device.getName().toString());
                    Log.i(TAG, "WorKBleGroup->添加蓝牙" + device.getName().toString());
                }


            } else {
                //   Log.d(TAG,"WorKBleGroup->蓝牙搜索名字匹配失败");
            }
        }

        return addFlag;
    }

    /***
     * 解析测绘蓝牙数据，35字节，两个数据包
     */
    byte[] bleBuf = new byte[35];
    byte revCount=0;
    public  MappingGroup translateMappingData(BluetoothGattCharacteristic characteristic) {

        MappingGroup rtkMap =new MappingGroup();
        //测绘数据长度，等于两个蓝牙数据包,35字节
        byte[] buf = characteristic.getValue();

        if(20 == buf.length && revCount == 0) {//第一个包为20字节才开始接收
            revCount =1;
            System.arraycopy(buf, 0, bleBuf, 0, buf.length);
        }
        if(1 == revCount){

            System.arraycopy(buf, 0, bleBuf, 20, buf.length);

            if (CRC16_ccitTable.CRC16_ccitt(bleBuf)) {//CRC校验

                //转化成数据流转GPS数据
                int sp = 1;
                rtkMap.rtkState = bleBuf[sp];   //RTK状态
                sp = sp + 1;
                rtkMap.longitude = getInt(bleBuf, sp); //经度
                sp = sp + 4;
                rtkMap.latitude = getInt(bleBuf, sp); //纬度
                sp = sp + 4;
                rtkMap.altitude = getFloat(bleBuf, sp); //海拔
                sp = sp + 4;
                rtkMap.roll = getFloat(bleBuf, sp); //IMU
                sp = sp + 4;
                rtkMap.pitch = getFloat(bleBuf, sp); //IMU
                sp = sp + 4;
                rtkMap.yaw = (float) (getFloat(bleBuf, sp)*180/MappingGroup.PI); //方向
                sp = sp + 4;
                rtkMap.GPSTime_weeks = getShort(bleBuf, sp); //周
                sp = sp + 2;
                rtkMap.GPSTime_ms = getInt(bleBuf, sp); //时间ms


       //         Log.d(TAG, "RTK状态：" + rtkMap.rtkState + " 经度：" + rtkMap.longitude + " 纬度：" + rtkMap.latitude + " 海拔：" + rtkMap.altitude + " roll：" + rtkMap.roll
       //                 + " pitch：" + rtkMap.pitch + " 方向：" + rtkMap.yaw + " 周：" + rtkMap.GPSTime_weeks + " 时间：" + rtkMap.GPSTime_ms);

            }else {
                rtkMap = null;
            }

            revCount =0;
            return rtkMap;
        }

        return null;

    }

    public HeatDataMsg translateTaskData(BluetoothGattCharacteristic characteristic){
        HeatDataMsg stateMsg = new HeatDataMsg();

        byte[] buf = characteristic.getValue();
     //   Log.d(TAG,"接收到数据->"+Utils.bytesToHexString(buf));

        int sp = 1;
        stateMsg.robotId = buf[sp] | (buf[sp+1]<<8);
        sp=sp+2;
        stateMsg.revCommand = buf[sp];
        sp=sp+1;
        stateMsg.poseLongitude = getInt(buf,sp);
        sp=sp+4;
        stateMsg.poseLatitude = getInt(buf,sp);
        sp=sp+4;
        stateMsg.posePhi = getShort(buf,sp);
        sp=sp+2;
        stateMsg.tankLevel = buf[sp];
        sp=sp+1;
        stateMsg.batteryPercentage = buf[sp];
        sp=sp+1;
        stateMsg.curState = buf[sp];
        sp=sp+1;
        byte curBitsState =  buf[sp];

        stateMsg.rtkState = (byte) ((curBitsState>>2) & 0x03);


        //有无基站GPS
        if((curBitsState & 0x10) == 0x10) {
            stateMsg.basicGps = true;
        }else{
            stateMsg.basicGps =false;
        }
        //驱动器报警
        if((curBitsState & 0x08) == 0x08) {
            stateMsg.dAlarm =true;
        }else{
            stateMsg.dAlarm =false;
        }

       //有无任务
        if((curBitsState & 0x02) == 0x02) {
            stateMsg.taskFile = true;
        }else{
            stateMsg.taskFile =false;
        }

        //发动机状态
        if((curBitsState & 0x02) == 0x02) {
            stateMsg.motorState = true;
        }else{
            stateMsg.motorState = false;
        }
        return stateMsg;
    }

    /***
     *
     * @param com 要发送的数据
     * @return
     */
    public boolean sendCommand(byte[] com){
       if(isConnectGattCh !=null && mBLE !=null){
           isConnectGattCh.setValue(com);
           mBLE.writeCharacteristic(isConnectGattCh);
       }else{
           return false;
       }
       return true;
    }

}
