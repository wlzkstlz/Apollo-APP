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

    public BLEService.RobotWorkingCallback robotWorkingCallback=null;

    /***
     *
     * @param poll
     * @param workBleGroup
     */
    public  RobotManagement(final PollingManagement poll, final WorkBleGroup workBleGroup){
        workRobotList = new ArrayList<TankRobot>();
        workMatchList = new ArrayList<WorkMatch>();
        this.pollingManagement=poll;
        this.workBleGroup=workBleGroup;

        //配置分时轮询
        pollingManagement.setPolling(5);
        pollingManagement.haveWorkMapPageCtr=false;

        pollCount=0;

    //    robotWorkingCallback = callback;

        //分时轮询会调用
        poll.setPollCallback(new PollingManagement.PollCallback() {

            @Override
            public void askInCenterRobot() {
            //    Log.d(TAG,"RobotManagement->askAutoRobot()");

                if(workBleGroup !=null && workRobotList.size()>0) {

                    if(workBleGroup.mBLE !=null && workBleGroup.isConnectGattCh !=null) {
                        //
                        if(getInCenterCommond()){
                          workBleGroup.sendCommand(comBuf);
                            pollCount++;
                            if(pollCount>=workRobotList.size()){
                                pollCount=0;
                            }
                        }
                    }
                }

            }

            @Override
            public void askInWorkMapRobot(){

                if(workBleGroup !=null && workRobotList.size()>0) {

                    if (workBleGroup.mBLE != null && workBleGroup.isConnectGattCh != null) {


                        if (getInWorkMapCommond()) {
                            workBleGroup.sendCommand(comBuf);
                        }
                    }
                }
            }
        });

    }

    /***
     * 获取自动驾驶发送数据
     * @return
     */
    private boolean getInCenterCommond(){

        if(pollCount <= workRobotList.size()-1){
            //因为只有一个是手动控制，如果当前不是就调到下一个
            if(workRobotList.get(pollCount).workAuto != TankRobot.CTR_AUTO){
                pollCount++;
                if(pollCount>=workRobotList.size()){
                    pollCount=0;
                }
            }

            //装指令
            if(workRobotList.get(pollCount).workAuto == TankRobot.CTR_AUTO) {
                comBuf[0] = (byte) (workRobotList.get(pollCount).heatDataMsg.robotId);//机器人ID
                comBuf[1] = (byte) (workRobotList.get(pollCount).heatDataMsg.robotId >> 8);
                comBuf[2] = workRobotList.get(pollCount).LORA_CH;                      //信道
                comBuf[3] = 0x55;
                comBuf[4] = workRobotList.get(pollCount).heatDataMsg.command;//指令
                comBuf[5] = 0;
                workRobotList.get(pollCount).checkCount++;//掉线检测

                Log.d(TAG,"RobotManagement->发送getInCenterCommond()");

              //  for(int i=0;i<workRobotList.size();i++){
              //      Log.d(TAG,"RobotManagement->workAuto="+workRobotList.get(i).workAuto);
             //   }
            }

        }else{
            return false;
        }
        return true;
    }

    /***
     * 获取手动控制发送数据
     * @return
     */
    private boolean getInWorkMapCommond(){
        for(int i=0;i<workRobotList.size();i++){
            if(workRobotList.get(i).workAuto != TankRobot.CTR_AUTO){

                //装指令
                comBuf[0] = (byte)(workRobotList.get(i).heatDataMsg.robotId);
                comBuf[1] = (byte)(workRobotList.get(i).heatDataMsg.robotId>>8);
                comBuf[2] =  workRobotList.get(i).LORA_CH;
                comBuf[3] = 0x55;
                comBuf[4] = workRobotList.get(i).heatDataMsg.command;//指令
                comBuf[5] =0;
                workRobotList.get(i).checkCount++;//掉线检测

                Log.d(TAG,"RobotManagement->getInWorkMapCommond");

                break;
            }
        }
        return true;
    }

}
