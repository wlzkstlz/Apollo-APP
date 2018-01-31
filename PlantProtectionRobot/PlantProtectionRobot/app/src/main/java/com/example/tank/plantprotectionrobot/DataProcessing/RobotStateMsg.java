package com.example.tank.plantprotectionrobot.DataProcessing;

/**机器人运行状态信息
 * Created by TK on 2018/1/31.
 */

public class RobotStateMsg {
    public  int poseX; //相对于基站距离x
    public  int  poseY;//相对于基站距离x
    public  int  posePhi;//海拔
    public  int  tankLevel;//水量250 0.5米
    public  int  batteryPercentage;//百分比0-100%
    public  int  curState; //状态
    public  int  curPathId; //
    //curBitsState 用位来表示信息 bit5 = 有无基站坐标 bit4=驱动器报警 bit3-2 RTK状态 bit1有无任务文件 bit0 发动机转台
    public boolean basicGps;
    public boolean dAlarm;//驱动器报警
    public byte rtkState;//rtk状态，两个
    public boolean taskFile;//有无任务文件
    public boolean motorState;//发动机状态




}
