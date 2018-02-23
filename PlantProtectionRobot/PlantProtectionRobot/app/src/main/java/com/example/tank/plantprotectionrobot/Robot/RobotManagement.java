package com.example.tank.plantprotectionrobot.Robot;

import android.util.Log;

import com.example.tank.plantprotectionrobot.BLE.BLEService;
import com.example.tank.plantprotectionrobot.BLE.WorkBleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 2018/2/7.
 */

public class RobotManagement{

    private final String TAG = "Tank001";

    //在运行的机器人,最多只有一个是手动控制
    public ArrayList<TankRobot> workRobotList;

    //分时轮询
    private PollingManagement pollingManagement;

    //蓝牙工作组
    public WorkBleGroup workBleGroup=null;
    //工作匹配组，机器人与果园、路径匹配
    public ArrayList<WorkMatch> workMatchList = null;

    //指令
    private byte[] comBuf = new byte[6];
    public int pollCount;//轮询计数

    private BLEService.RobotWorkingCallback robotWorkingCallback=null;

    /***
     *
     * @param poll
     * @param workBleGroup
     * @param callback  作业模式下的回调函数
     */
    public  RobotManagement(final PollingManagement poll, final WorkBleGroup workBleGroup, BLEService.RobotWorkingCallback callback){
        workRobotList = new ArrayList<TankRobot>();
        workMatchList = new ArrayList<WorkMatch>();
        this.pollingManagement=poll;
        this.workBleGroup=workBleGroup;

        //配置分时轮询
        pollingManagement.setPolling(5);
        pollingManagement.haveHandleCtr=false;

        pollCount=0;

        robotWorkingCallback = callback;


        //分时轮询会调用
        poll.setPollCallback(new PollingManagement.PollCallback() {


            @Override
            public void askAutoRobot() {
            //    Log.d(TAG,"RobotManagement->askAutoRobot()");

                if(workBleGroup !=null) {
                    if(workBleGroup.mBLE !=null && workBleGroup.isConnectGattCh !=null) {

                        //针对已添加的机器人，发送前更新新消息,如果是没有接收到返回，这里也将会更新数据，
                        if(pollCount<workRobotList.size() && workRobotList.get(pollCount).isWorking ==false) {
                            if(robotWorkingCallback !=null) {
                                robotWorkingCallback.RobotStateChanged(workRobotList.get(pollCount));
                            }
                        }
                        //
                        if(getAutoCommond()){
                          workBleGroup.sendCommand(comBuf);
                        }
                        pollCount++;
                        if(pollCount>=workRobotList.size()){
                            pollCount=0;
                        }
                    }
                }

            }

            @Override
            public void askHanldeRobot() {

                if(getHandleCommond()){
                    workBleGroup.sendCommand(comBuf);
                }
           //     Log.d(TAG,"RobotManagement->askHanldeRobot()");
            }
        });

    }
    //自动驾驶
    private boolean getAutoCommond(){

        if(workRobotList.size()-1>=pollCount){
            //因为只有一个是手动控制，如果当前不是就调到下一个
            if(workRobotList.get(pollCount).workAuto == TankRobot.CTR_AUTO){
                pollCount++;
                if(pollCount>=workRobotList.size()){
                    pollCount=0;
                }
            }
            //装指令
            comBuf[0] = (byte)(workRobotList.get(pollCount).heatDataMsg.robotId>>8);//机器人ID
            comBuf[1] = (byte)(workRobotList.get(pollCount).heatDataMsg.robotId);
            comBuf[2] =  workRobotList.get(pollCount).LORA_CH;                      //信道
            comBuf[3] = 0x55;
            comBuf[4] = workRobotList.get(pollCount).heatDataMsg.command;//指令
            comBuf[5] =0;

            workRobotList.get(pollCount).checkCount++;//掉线检测
        }else{
            return false;
        }
        return true;
    }
    private boolean getHandleCommond(){
        for(int i=0;i<workRobotList.size();i++){
            if(workRobotList.get(i).workAuto != TankRobot.CTR_AUTO){

                //装指令
                comBuf[0] = (byte)(workRobotList.get(pollCount).heatDataMsg.robotId>>8);
                comBuf[1] = (byte)(workRobotList.get(pollCount).heatDataMsg.robotId);
                comBuf[2] =  workRobotList.get(pollCount).LORA_CH;
                comBuf[3] = 0x55;
                comBuf[4] = workRobotList.get(pollCount).heatDataMsg.command;//指令
                comBuf[5] =0;
                workRobotList.get(pollCount).checkCount++;//掉线检测
                break;
            }
        }
        return true;
    }

    public void addRobot(TankRobot robot){
        workRobotList.add(robot);
        pollCount = workRobotList.size()-1;
    }


}
