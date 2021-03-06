package com.example.tank.plantprotectionrobot.Robot;

import com.example.tank.plantprotectionrobot.Robot.HeatDataMsg;

/**
 * Created by TK on 2018/2/7.
 */

public class TankRobot{


    public  final byte LORA_CH=0;//机器人LoRa通信通道
    public  boolean isWorking; //是否在工作，flase表示新添加还没工作，true表示已工作
    public  boolean robotOnline; //在线true 掉线 line
    public  int checkCount; //不回复信息计数>3次认为离线
    public  int workAuto;//0自动驾驶状态 1表示手动驾驶2手动转场
    public WorkMatch workMatch; //机器人匹配果园、路劲
    public HeatDataMsg heatDataMsg;
    public boolean inWorkPage; //true在控制界面 false不在控制界面

    public static final int PILOT_STATE_INIT = 0;
    public static final int PILOT_STATE_IDLE=1;
    public static final int PILOT_STATE_TRANSITION=2;
    public static final int PILOT_STATE_AUTO=3;
    public static final int PILOT_STATE_MANUAL_WORK=4;
    public static final int PILOT_STATE_SUPPLY=5;
    public static final int PILOT_STATE_BLE_TRANSFER=6;
    public static final int PILOT_STATE_EMERGENCY=7;

    public TankRobot(int id){

        heatDataMsg =new HeatDataMsg();
        workMatch = new WorkMatch();
        checkCount=0;
        workAuto = PILOT_STATE_TRANSITION;
        robotOnline=false;
        isWorking = false;
        heatDataMsg.robotId=id;
        inWorkPage = false;
        heatDataMsg.command = CommondType.CMD_HEARTBEAT;
        heatDataMsg.revCommand = CommondType.CMD_TRANSITION;


    }




}
