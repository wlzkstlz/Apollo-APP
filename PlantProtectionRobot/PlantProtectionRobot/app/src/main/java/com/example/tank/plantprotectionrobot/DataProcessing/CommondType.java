package com.example.tank.plantprotectionrobot.DataProcessing;

/**LoRa发送给机器人的指令
 * Created by TK on 2018/1/31.
 */

public class CommondType {
    public  static final  int CMD_HEARTBEAT=0;//心跳
    public  static final  int CMD_AUTO=1;//自动驾驶
    public  static final  int CMD_MANUAL=2;//手动驾驶
    public  static final  int CMD_TRANSITION=3;//转场
    public  static final  int CMD_SUPPLY=4;//补给
    public  static final  int CMD_STOP=5;//急停
    public  static final  int CMD_BLE_START=6;//开始蓝牙传输
    public  static final  int CMD_BLE_END=7;//结束蓝牙传输
    public  static final  int CMD_BLE_ABORT=8;//中断蓝牙传输
    public  static final  int CMD_NONE=9;//
    public  static final  int CMD_WAIT=10;//等待机制实现
}
