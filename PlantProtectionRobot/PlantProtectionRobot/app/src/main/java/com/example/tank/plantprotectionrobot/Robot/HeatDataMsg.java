package com.example.tank.plantprotectionrobot.Robot;

/**
 * Created by TK on 2018/2/7.
 */

public class HeatDataMsg {

    public  int robotId;//机器人编号
    public  byte command;//需要执行的指令
    public  int poseLongitude;//相对于基站距离x
    public  int  poseLatitude;//相对于基站距离x
    public  short  posePhi;//海拔
    public  byte  tankLevel;//水量250 0.5米
    public  byte  batteryPercentage;//百分比0-100%
    public  byte  curState; //状态
    //curBitsState 用位来表示信息 bit5 = 有无基站坐标 bit4=驱动器报警 bit3-2 RTK状态 bit1有无任务文件 bit0 发动机转台

    public boolean basicGps;//有无基站数据
    public boolean dAlarm;//驱动器报警
    public byte rtkState;//rtk状态
    public boolean taskFile;//有无任务文件
    public boolean motorState;//发动机状态

    /*
    public void heatDataMsgRest(){
        robotId = 0;
        command = CommondType.CMD_HEARTBEAT;
        poseLongitude=0;
        poseLatitude=0;
        posePhi=0;
        tankLevel=0;
        batteryPercentage=0;
        curState=0;
        basicGps=false;
        dAlarm =false;
        rtkState=0;
        taskFile=false;
        motorState=false;
    }
    */
}
